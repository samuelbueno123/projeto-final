package com.cefet_rj.projeto_final.shared;

import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Stage 6 (M20) Schema Parity Guard:
 * Applies every Flyway migration (V1..V26) against an isolated H2 PostgreSQL-mode database
 * (normalizing only PostgreSQL-only index clauses like partial WHERE / functional LOWER / DESC that H2 cannot parse)
 * and enforces Hibernate `hbm2ddl.auto=validate` across every `@Entity` in the application.
 */
class FlywaySchemaParityTests {

    private static final Pattern PG_PARTIAL_INDEX = Pattern.compile(
            "(?is)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+[^;]*?)\\s+WHERE\\s+[^;]+"
    );
    private static final Pattern PG_LOWER_INDEX = Pattern.compile(
            "(?is)CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+IF\\s+NOT\\s+EXISTS\\s+idx_user_email_lower\\s+ON\\s+tb_user\\s*\\(LOWER\\(email\\)\\)"
    );
    private static final Pattern PG_INDEX_SORT = Pattern.compile(
            "(?is)(\\b\\w+\\s+)(?:DESC|ASC)(\\s*[,)])"
    );

    @Test
    @DisplayName("Flyway V1..V26 migrations must apply cleanly and match Hibernate JPA @Entity schema (validate)")
    void flywayMigrationsShouldPassHibernateSchemaValidation(@TempDir Path tempMigrationsDir) throws IOException {
        Path sourceDir = Paths.get("src", "main", "resources", "db", "migration");
        assertTrue(Files.isDirectory(sourceDir), "Migration directory should exist");

        try (Stream<Path> files = Files.list(sourceDir)) {
            for (Path sqlFile : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
                String sql = Files.readString(sqlFile, StandardCharsets.UTF_8);
                // Strip PostgreSQL-only partial WHERE predicate / expression index syntax inside test sandbox only
                sql = PG_PARTIAL_INDEX.matcher(sql).replaceAll(m -> m.group(1).replaceFirst("(?i)CREATE\\s+UNIQUE\\s+INDEX", "CREATE INDEX"));
                sql = PG_LOWER_INDEX.matcher(sql).replaceAll("CREATE INDEX IF NOT EXISTS idx_user_email_lower ON tb_user (email)");
                sql = PG_INDEX_SORT.matcher(sql).replaceAll("$1$2");
                sql = sql.replaceAll("(?im)^DROP\\s+INDEX\\s+IF\\s+EXISTS\\s+idx_[^;]+;\\s*$", "");
                Files.writeString(tempMigrationsDir.resolve(sqlFile.getFileName()), sql, StandardCharsets.UTF_8);
            }
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(
                "jdbc:h2:mem:flyway_schema_parity;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;NON_KEYWORDS=YEAR,MONTH;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("filesystem:" + tempMigrationsDir.toAbsolutePath())
                .load();

        MigrateResult migrateResult = flyway.migrate();
        assertTrue(migrateResult.success, "Flyway migration V1..V26 should succeed");
        assertEquals(26, migrateResult.migrationsExecuted, "Expected all 26 Flyway migrations (V1..V26) to execute");
        assertEquals(0, flyway.info().pending().length, "No pending migrations should remain");

        LocalContainerEntityManagerFactoryBean emfBean = new LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(dataSource);
        emfBean.setPackagesToScan("com.cefet_rj.projeto_final");
        emfBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties jpaProps = new Properties();
        jpaProps.put("hibernate.hbm2ddl.auto", "validate");
        jpaProps.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        jpaProps.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        emfBean.setJpaProperties(jpaProps);

        try {
            emfBean.afterPropertiesSet();
            EntityManagerFactory emf = emfBean.getObject();
            assertNotNull(emf, "EntityManagerFactory should initialize under validate mode");
            assertFalse(emf.getMetamodel().getEntities().isEmpty(), "JPA Metamodel should not be empty");
        } finally {
            emfBean.destroy();
        }
    }
}
