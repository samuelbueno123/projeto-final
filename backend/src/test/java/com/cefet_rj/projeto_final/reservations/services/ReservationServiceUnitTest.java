package com.cefet_rj.projeto_final.reservations.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationDTO;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationRequestDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import com.cefet_rj.projeto_final.reservations.exceptions.ReservationConflictException;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceUnitTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private CommonAreaRepository commonAreaRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private CondominiumAccessService condominiumAccessService;

    @Mock
    private CondominiumFeatureAccessService featureAccessService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ReservationService reservationService;

    private Condominium condo;
    private CommonArea area;
    private User residentUser;

    @BeforeEach
    void setUp() {
        condo = new Condominium();
        condo.setId(1L);
        condo.setName("Residencial Horizon");

        area = new CommonArea();
        area.setId(10L);
        area.setName("Salão de Festas");
        area.setCondominium(condo);
        area.setActive(true);
        area.setRequiresApproval(true);
        area.setMinAdvanceHours(2);
        area.setMaxAdvanceDays(30);
        area.setMaxDurationHours(6);
        area.setMaxWeeklyBookingsPerUser(2);
        area.setAllowWaitlist(true);

        residentUser = new User();
        residentUser.setId(20L);
        residentUser.setFirstName("Carlos");
        residentUser.setLastName("Morador");
        residentUser.setEmail("carlos@example.com");
        Role role = new Role();
        role.setAuthority("ROLE_USER");
        residentUser.addRole(role);

        lenient().when(commonAreaRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(area));
    }

    private void setupCommonMocks() {
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
    }

    @Test
    @DisplayName("create: throws BadRequest when start or end is null")
    void create_throwsBadRequest_whenStartOrEndIsNull() {
        setupCommonMocks();
        ReservationRequestDTO dto = new ReservationRequestDTO(null, null, null, null, null, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getReason().contains("obrigatórios"));
    }

    @Test
    @DisplayName("create: throws BadRequest when end date is before start date")
    void create_throwsBadRequest_whenEndBeforeStart() {
        setupCommonMocks();
        LocalDateTime now = LocalDateTime.now().plusDays(2);
        ReservationRequestDTO dto = new ReservationRequestDTO(now, now.minusHours(1), null, null, null, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getReason().contains("posterior ao horário de início"));
    }

    @Test
    @DisplayName("create: throws BadRequest when start date is in the past")
    void create_throwsBadRequest_whenStartInPast() {
        setupCommonMocks();
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        ReservationRequestDTO dto = new ReservationRequestDTO(past, past.plusHours(2), null, null, null, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getReason().contains("passado"));
    }

    @Test
    @DisplayName("create: throws BadRequest when min advance hours violated")
    void create_throwsBadRequest_whenMinAdvanceHoursViolated() {
        setupCommonMocks();
        LocalDateTime tooSoon = LocalDateTime.now().plusMinutes(30);
        ReservationRequestDTO dto = new ReservationRequestDTO(tooSoon, tooSoon.plusHours(2), null, null, null, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getReason().contains("antecedência mínima de 2h"));
    }

    @Test
    @DisplayName("create: throws BadRequest when max advance days exceeded")
    void create_throwsBadRequest_whenMaxAdvanceDaysExceeded() {
        setupCommonMocks();
        LocalDateTime tooFar = LocalDateTime.now().plusDays(40);
        ReservationRequestDTO dto = new ReservationRequestDTO(tooFar, tooFar.plusHours(2), null, null, null, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getReason().contains("30 dias de antecedência"));
    }

    @Test
    @DisplayName("create: throws BadRequest when max duration exceeded")
    void create_throwsBadRequest_whenMaxDurationHoursExceeded() {
        setupCommonMocks();
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusHours(8);
        ReservationRequestDTO dto = new ReservationRequestDTO(start, end, null, null, null, false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getReason().contains("duração máxima permitida para esta área comum é de 6h"));
    }

    @Test
    @DisplayName("create: throws BadRequest when weekly booking limit reached")
    void create_throwsBadRequest_whenMaxWeeklyLimitReached() {
        setupCommonMocks();
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusHours(3);
        ReservationRequestDTO dto = new ReservationRequestDTO(start, end, null, null, null, false);

        when(reservationRepository.countActiveBookingsByUserInRange(eq(area.getId()), eq(residentUser.getId()), any(), any()))
                .thenReturn(2L); // Limit is 2

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getReason().contains("Limite semanal atingido"));
    }

    @Test
    @DisplayName("create: throws Conflict when user already has overlapping reservation")
    void create_throwsConflict_whenUserHasDuplicateBooking() {
        setupCommonMocks();
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusHours(3);
        ReservationRequestDTO dto = new ReservationRequestDTO(start, end, null, null, null, false);

        when(reservationRepository.countUserOverlappingReservationsOrWaitlist(area.getId(), residentUser.getId(), start, end))
                .thenReturn(1L);

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getMessage().contains("already have an active reservation"));
    }

    @Test
    @DisplayName("create: throws Conflict when slot is busy and waitlist is not requested")
    void create_throwsConflict_whenSlotBusyAndWaitlistNotRequested() {
        setupCommonMocks();
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusHours(3);
        ReservationRequestDTO dto = new ReservationRequestDTO(start, end, null, null, null, false);

        when(reservationRepository.countConflictingReservations(eq(area.getId()), eq(start), eq(end), eq(ReservationStatus.CANCELLED)))
                .thenReturn(1L);

        ReservationConflictException ex = assertThrows(ReservationConflictException.class,
                () -> reservationService.create(area.getId(), dto));
        assertTrue(ex.getMessage().contains("already reserved"));
    }

    @Test
    @DisplayName("create: puts into waitlist when slot is busy and waitlist requested")
    void create_joinsWaitlist_whenSlotBusyAndWaitlistRequested() {
        setupCommonMocks();
        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusHours(3);
        ReservationRequestDTO dto = new ReservationRequestDTO(start, end, null, "11999998888", "Festa", true);

        when(reservationRepository.countConflictingReservations(eq(area.getId()), eq(start), eq(end), eq(ReservationStatus.CANCELLED)))
                .thenReturn(1L);
        when(reservationRepository.findWaitlistedOverlapping(area.getId(), start, end))
                .thenReturn(List.of());

        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> {
            Reservation r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        ReservationDTO result = reservationService.create(area.getId(), dto);

        assertEquals(ReservationStatus.WAITLISTED, result.status());
        assertEquals(1, result.waitlistPosition());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("approve: throws Conflict when reservation already cancelled or rejected")
    void approve_throwsConflict_whenAlreadyCancelledOrRejected() {
        Reservation res = new Reservation();
        res.setId(50L);
        res.setCommonArea(area);
        res.setStatus(ReservationStatus.CANCELLED);
        res.setEndAt(LocalDateTime.now().plusDays(1));

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(res));

        assertThrows(ResponseStatusException.class, () -> reservationService.approve(50L));
    }

    @Test
    @DisplayName("approve: throws Conflict when another confirmed reservation overlaps")
    void approve_throwsConflict_whenConfirmedConflictExists() {
        Reservation res = new Reservation();
        res.setId(50L);
        res.setCommonArea(area);
        res.setStatus(ReservationStatus.PENDING);
        res.setStartAt(LocalDateTime.now().plusDays(2));
        res.setEndAt(LocalDateTime.now().plusDays(2).plusHours(4));

        Reservation confirmedConflict = new Reservation();
        confirmedConflict.setId(51L);
        confirmedConflict.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(res));
        when(reservationRepository.findActiveOverlapping(eq(area.getId()), eq(res.getStartAt()), eq(res.getEndAt())))
                .thenReturn(List.of(confirmedConflict));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> reservationService.approve(50L));
        assertTrue(ex.getReason().contains("Já existe uma reserva confirmada"));
    }

    @Test
    @DisplayName("approve: successfully confirms pending reservation and notifies resident")
    void approve_success_confirmsAndNotifies() {
        Reservation res = new Reservation();
        res.setId(50L);
        res.setCommonArea(area);
        res.setReservedBy(residentUser);
        res.setStatus(ReservationStatus.PENDING);
        res.setStartAt(LocalDateTime.now().plusDays(2));
        res.setEndAt(LocalDateTime.now().plusDays(2).plusHours(4));

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(res));
        when(reservationRepository.findActiveOverlapping(eq(area.getId()), any(), any())).thenReturn(List.of(res));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservationDTO approved = reservationService.approve(50L);

        assertEquals(ReservationStatus.CONFIRMED, approved.status());
        verify(notificationService).createDirectNotification(eq(residentUser), any(), anyString(), anyString(), eq(condo.getId()), anyString());
    }

    @Test
    @DisplayName("reject: successfully sets status to REJECTED and auto-promotes waitlist")
    void reject_promotesNextFromWaitlist() {
        Reservation res = new Reservation();
        res.setId(50L);
        res.setCommonArea(area);
        res.setReservedBy(residentUser);
        res.setStatus(ReservationStatus.PENDING);
        res.setStartAt(LocalDateTime.now().plusDays(2));
        res.setEndAt(LocalDateTime.now().plusDays(2).plusHours(4));

        Reservation waitlisted = new Reservation();
        waitlisted.setId(51L);
        waitlisted.setCommonArea(area);
        waitlisted.setReservedBy(residentUser);
        waitlisted.setStatus(ReservationStatus.WAITLISTED);
        waitlisted.setWaitlistPosition(1);

        when(reservationRepository.findById(50L)).thenReturn(Optional.of(res));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(reservationRepository.findWaitlistedOverlapping(eq(area.getId()), any(), any()))
                .thenReturn(List.of(waitlisted));

        ReservationDTO rejected = reservationService.reject(50L, "Motivo administrativo");

        assertEquals(ReservationStatus.REJECTED, rejected.status());
        assertEquals("Motivo administrativo", rejected.rejectionReason());

        assertEquals(ReservationStatus.PENDING, waitlisted.getStatus());
        assertNull(waitlisted.getWaitlistPosition());
        verify(reservationRepository, atLeast(2)).save(any(Reservation.class));
    }
}
