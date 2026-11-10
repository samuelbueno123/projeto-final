# ==============================================================================
# Multi-stage Dockerfile for Gestão Condominial Backend API (Spring Boot - Java 17)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. BUILDER STAGE
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Cache maven dependencies by copying pom.xml first
COPY pom.xml ./
RUN mvn dependency:go-offline -B || true

# Copy application source code
COPY src ./src

# Compile and package production executable JAR (skipping unit tests during packaging)
RUN mvn -B -DskipTests clean package

# ------------------------------------------------------------------------------
# 2. RUNNER STAGE (Hardened, unprivileged container)
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy AS runner

LABEL org.opencontainers.image.title="Gestao Condominial Backend API" \
      org.opencontainers.image.description="Secure Spring Boot REST API for condominium management" \
      org.opencontainers.image.version="1.0.0" \
      org.opencontainers.image.licenses="MIT"

# Security: Create non-root system group and user (UID/GID 10001)
RUN groupadd -g 10001 -r appgroup && \
    useradd -u 10001 -r -g appgroup -s /usr/sbin/nologin -d /app -c "Application Runner" appuser

WORKDIR /app

# Create necessary application directories and assign ownership to appuser
RUN mkdir -p /app/.secrets /app/media/pictures /app/logs && \
    chown -R appuser:appgroup /app

# Copy executable jar from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/projeto-final-0.0.1-SNAPSHOT.jar /app/app.jar

# Switch to unprivileged non-root user
USER 10001:10001

# Standard container ports
EXPOSE 8080

# Production JVM Ergonomics & Container Memory Support
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Automated container health check via Spring Boot Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
