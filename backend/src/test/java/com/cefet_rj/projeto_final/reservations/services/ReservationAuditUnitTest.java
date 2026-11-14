package com.cefet_rj.projeto_final.reservations.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationRequestDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.shared.exceptionhandler.ResourceExceptionHandler;
import com.cefet_rj.projeto_final.shared.exceptionhandler.StandardError;
import com.cefet_rj.projeto_final.shared.exceptionhandler.ValidationError;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationAuditUnitTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private CommonAreaRepository commonAreaRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuthenticatedUserService authenticatedUserService;
    @Mock private CondominiumAccessService condominiumAccessService;
    @Mock private CondominiumFeatureAccessService featureAccessService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private ReservationService reservationService;

    @Test
    void createRejectsResidentBookingWithinMinAdvanceHours() {
        Condominium condo = new Condominium();
        condo.setId(1L);
        condo.setName("Condo Teste");

        CommonArea area = new CommonArea();
        area.setId(10L);
        area.setCondominium(condo);
        area.setName("Salão");
        area.setCapacity(20);
        area.setActive(true);
        area.setMinAdvanceHours(1);
        area.setMaxAdvanceDays(60);
        area.setMaxDurationHours(8);
        area.setMaxWeeklyBookingsPerUser(3);

        User resident = new User();
        resident.setId(7L);
        resident.setEmail("morador@teste.com");

        when(commonAreaRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(area));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(resident);
        when(condominiumAccessService.isManagerOrAdmin(resident, 1L)).thenReturn(false);

        LocalDateTime start = LocalDateTime.now().plusMinutes(30);
        LocalDateTime end = start.plusHours(1);
        ReservationRequestDTO dto = new ReservationRequestDTO(start, end);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> reservationService.create(10L, dto)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void constraintViolationHandlerReturnsBadRequestValidationError() {
        ResourceExceptionHandler handler = new ResourceExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/metrics");

        ResponseEntity<StandardError> response = handler.constraintViolation(
                new ConstraintViolationException("Invalid parameter", Set.of()),
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertInstanceOf(ValidationError.class, response.getBody());
        assertEquals("Validation error", response.getBody().getError());
    }

    @Test
    void flywayMigrationsHaveUniqueVersionsAndValidForeignKeySyntax() throws IOException {
        Path migrationDir = Files.isDirectory(Path.of("src/main/resources/db/migration"))
                ? Path.of("src/main/resources/db/migration")
                : Path.of("backend/src/main/resources/db/migration");

        assertTrue(Files.isDirectory(migrationDir), "Diretório de migrations Flyway deve existir");

        List<Path> sqlFiles;
        try (var stream = Files.list(migrationDir)) {
            sqlFiles = stream
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .sorted()
                    .toList();
        }
        assertFalse(sqlFiles.isEmpty(), "Deve haver migrations Flyway");

        Pattern versionPattern = Pattern.compile("^V(\\d+(?:\\.\\d+)*)__.*\\.sql$");
        Pattern invalidFkPattern = Pattern.compile("\\bNOT\\s+REFERENCES\\b", Pattern.CASE_INSENSITIVE);
        Set<String> versions = new HashSet<>();

        for (Path file : sqlFiles) {
            String name = file.getFileName().toString();
            Matcher matcher = versionPattern.matcher(name);
            assertTrue(matcher.matches(), "Nome de migration inválido: " + name);
            String version = matcher.group(1);
            assertTrue(versions.add(version), "Versão Flyway duplicada encontrada: V" + version + " em " + name);

            String content = Files.readString(file);
            assertFalse(
                    invalidFkPattern.matcher(content).find(),
                    "Sintaxe SQL inválida 'NOT REFERENCES' encontrada em " + name
            );
        }
    }
}
