package com.cefet_rj.projeto_final.reservations.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationDTO;
import com.cefet_rj.projeto_final.reservations.dtos.ReservationRequestDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.enums.ReservationStatus;
import com.cefet_rj.projeto_final.reservations.exceptions.ReservationConflictException;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cefet_rj.projeto_final.reservations.events.ReservationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CommonAreaRepository commonAreaRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private RealtimeEmitterService realtimeEmitterService;

    @Transactional
    public ReservationDTO create(
            Long commonAreaId,
            ReservationRequestDTO dto
    ) {

        CommonArea commonArea =
                findCommonAreaByIdForUpdate(commonAreaId);

        Long condominiumId =
                commonArea.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        if (!Boolean.TRUE.equals(commonArea.getActive())) {
            throw new ForbiddenException(
                    "Common area is not available for reservations"
            );
        }

        User authenticatedUser =
                authenticatedUserService.getAuthenticatedUser();

        User user = authenticatedUser;
        if (condominiumAccessService.isManagerOrAdmin(authenticatedUser, condominiumId) && dto.reservedById() != null) {
            user = userRepository.findById(dto.reservedById())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.reservedById()));
            if (!user.hasRole("ROLE_ADMIN")) {
                condominiumAccessService.validateUserBelongsToCondominium(user.getId(), condominiumId);
            }
        }

        if (dto.startAt() == null || dto.endAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data e horário de início e término são obrigatórios.");
        }
        if (!dto.endAt().isAfter(dto.startAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O horário de término deve ser posterior ao horário de início.");
        }
        if (dto.startAt().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível criar reservas no passado.");
        }

        boolean isManagerOrAdmin = condominiumAccessService.isManagerOrAdmin(authenticatedUser, condominiumId);

        if (!isManagerOrAdmin) {
            validateBookingRules(commonArea, user, dto.startAt(), dto.endAt());
        }

        long userDuplicates = reservationRepository.countUserOverlappingReservationsOrWaitlist(
                commonAreaId, user.getId(), dto.startAt(), dto.endAt()
        );
        if (userDuplicates > 0) {
            throw new ReservationConflictException(
                    "You already have an active reservation or waitlist entry for this period"
            );
        }

        long conflicts =
                reservationRepository
                        .countConflictingReservations(
                                commonAreaId,
                                dto.startAt(),
                                dto.endAt(),
                                ReservationStatus.CANCELLED
                        );

        boolean allowWaitlist = commonArea.getAllowWaitlist() == null || commonArea.getAllowWaitlist();
        boolean joinWaitlistRequested = Boolean.TRUE.equals(dto.joinWaitlist());

        if (conflicts > 0 && (!joinWaitlistRequested || !allowWaitlist)) {
            throw new ReservationConflictException(
                    "Common area is already reserved for the selected period"
            );
        }

        boolean requiresApproval = commonArea.getRequiresApproval() != null
                ? commonArea.getRequiresApproval()
                : true;

        Reservation reservation = new Reservation();
        reservation.setCommonArea(commonArea);
        reservation.setReservedBy(user);
        reservation.setStartAt(dto.startAt());
        reservation.setEndAt(dto.endAt());
        reservation.setContactPhone(
                dto.contactPhone() == null || dto.contactPhone().isBlank()
                        ? null
                        : dto.contactPhone().replaceAll("\\D", "")
        );
        reservation.setNotes(
                dto.notes() == null || dto.notes().isBlank()
                        ? null
                        : dto.notes().trim()
        );
        reservation.setCreatedAt(LocalDateTime.now());

        if (conflicts > 0) {
            int nextPosition = reservationRepository
                    .findWaitlistedOverlapping(commonAreaId, dto.startAt(), dto.endAt())
                    .size() + 1;
            reservation.setStatus(ReservationStatus.WAITLISTED);
            reservation.setWaitlistPosition(nextPosition);
        } else {
            reservation.setStatus(
                    (isManagerOrAdmin || !requiresApproval)
                            ? ReservationStatus.CONFIRMED
                            : ReservationStatus.PENDING
            );
        }

        reservation = reservationRepository.save(reservation);
        emitReservationUpdated(condominiumId, reservation);

        if (reservation.getStatus() != ReservationStatus.WAITLISTED) {
            eventPublisher.publishEvent(
                    new ReservationCreatedEvent(
                            reservation.getId(),
                            condominiumId,
                            user.getId(),
                            commonArea.getName(),
                            reservation.getStartAt(),
                            LocalDateTime.now()
                    )
            );
        }

        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO joinWaitlist(Long commonAreaId, ReservationRequestDTO dto) {
        ReservationRequestDTO waitlistRequest = new ReservationRequestDTO(
                dto.startAt(),
                dto.endAt(),
                dto.reservedById(),
                dto.contactPhone(),
                dto.notes(),
                true
        );
        return create(commonAreaId, waitlistRequest);
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> findWaitlistByCommonArea(Long commonAreaId) {
        CommonArea commonArea = commonAreaRepository.findById(commonAreaId)
                .orElseThrow(() -> new ResourceNotFoundException("Common area not found: " + commonAreaId));
        Long condominiumId = commonArea.getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        validateCondominiumAccess(condominiumId);

        return reservationRepository.findWaitlistedByCommonArea(commonAreaId)
                .stream()
                .map(ReservationDTO::new)
                .toList();
    }

    private void validateBookingRules(
            CommonArea commonArea,
            User user,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        LocalDateTime now = LocalDateTime.now();

        int minAdvanceHours = commonArea.getMinAdvanceHours() != null ? commonArea.getMinAdvanceHours() : 1;
        if (minAdvanceHours > 0 && startAt.isBefore(now.plusHours(minAdvanceHours).minusMinutes(5))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A reserva exige antecedência mínima de " + minAdvanceHours + "h."
            );
        }

        int maxAdvanceDays = commonArea.getMaxAdvanceDays() != null ? commonArea.getMaxAdvanceDays() : 60;
        if (maxAdvanceDays > 0 && startAt.isAfter(now.plusDays(maxAdvanceDays).plusHours(2))) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A reserva não pode ser feita com mais de " + maxAdvanceDays + " dias de antecedência."
            );
        }

        int maxDurationHours = commonArea.getMaxDurationHours() != null ? commonArea.getMaxDurationHours() : 8;
        long durationMinutes = Duration.between(startAt, endAt).toMinutes();
        if (maxDurationHours > 0 && durationMinutes > maxDurationHours * 60L) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A duração máxima permitida para esta área comum é de " + maxDurationHours + "h."
            );
        }

        int maxWeekly = commonArea.getMaxWeeklyBookingsPerUser() != null ? commonArea.getMaxWeeklyBookingsPerUser() : 3;
        if (maxWeekly > 0) {
            LocalDateTime weekStart = startAt.toLocalDate()
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .atStartOfDay();
            LocalDateTime weekEnd = weekStart.plusDays(7);
            long weeklyBookings = reservationRepository.countActiveBookingsByUserInRange(
                    commonArea.getId(), user.getId(), weekStart, weekEnd
            );
            if (weeklyBookings >= maxWeekly) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Limite semanal atingido (" + maxWeekly + " reservas por semana nesta área comum)."
                );
            }
        }
    }

    @Transactional
    public ReservationDTO approve(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        findCommonAreaByIdForUpdate(reservation.getCommonArea().getId());
        Long condominiumId = reservation.getCommonArea().getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        condominiumAccessService.validateStaffAccess(condominiumId);

        User reviewer = authenticatedUserService.getAuthenticatedUser();

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return new ReservationDTO(reservation);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED || reservation.getStatus() == ReservationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível aprovar uma reserva que foi cancelada ou recusada.");
        }
        if (reservation.getEndAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível aprovar uma reserva cujo horário já expirou.");
        }

        List<Reservation> activeOverlapping = reservationRepository.findActiveOverlapping(
                reservation.getCommonArea().getId(),
                reservation.getStartAt(),
                reservation.getEndAt()
        );
        boolean hasConfirmedConflict = activeOverlapping.stream()
                .anyMatch(r -> !r.getId().equals(id) && r.getStatus() == ReservationStatus.CONFIRMED);
        if (hasConfirmedConflict) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma reserva confirmada para este mesmo período.");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setWaitlistPosition(null);
        reservation.setRejectionReason(null);
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);
        emitReservationUpdated(condominiumId, reservation);

        notificationService.createDirectNotification(
                reservation.getReservedBy(),
                NotificationType.RESERVATION,
                "Reserva aprovada!",
                "Sua solicitação de reserva para " + reservation.getCommonArea().getName()
                        + " foi aprovada pela portaria/administração.",
                condominiumId,
                reservation.getCommonArea().getCondominium().getName()
        );

        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO reject(Long id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        Long condominiumId = reservation.getCommonArea().getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        condominiumAccessService.validateStaffAccess(condominiumId);

        ReservationStatus previousStatus = reservation.getStatus();
        User reviewer = authenticatedUserService.getAuthenticatedUser();
        String cleanReason = reason == null || reason.isBlank()
                ? "Solicitação não aprovada pela administração."
                : reason.trim();
        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setWaitlistPosition(null);
        reservation.setRejectionReason(cleanReason);
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);
        emitReservationUpdated(condominiumId, reservation);

        notificationService.createDirectNotification(
                reservation.getReservedBy(),
                NotificationType.RESERVATION,
                "Reserva recusada",
                "Sua solicitação para " + reservation.getCommonArea().getName()
                        + " foi recusada: " + cleanReason,
                condominiumId,
                reservation.getCommonArea().getCondominium().getName()
        );

        if (previousStatus == ReservationStatus.PENDING
                || previousStatus == ReservationStatus.CONFIRMED
                || previousStatus == ReservationStatus.COUNTER_PROPOSED) {
            promoteNextFromWaitlist(reservation);
        }

        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO counterPropose(Long id, ReservationDTO.CounterProposalDTO dto) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        Long condominiumId = reservation.getCommonArea().getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        condominiumAccessService.validateStaffAccess(condominiumId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível enviar contraproposta para uma reserva cancelada ou recusada.");
        }
        if (dto.proposedStartAt() == null || dto.proposedEndAt() == null
                || !dto.proposedEndAt().isAfter(dto.proposedStartAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O horário proposto de término deve ser posterior ao horário de início.");
        }

        User reviewer = authenticatedUserService.getAuthenticatedUser();
        reservation.setStatus(ReservationStatus.COUNTER_PROPOSED);
        reservation.setProposedStartAt(dto.proposedStartAt());
        reservation.setProposedEndAt(dto.proposedEndAt());
        reservation.setCounterProposalNotes(dto.notes() == null ? null : dto.notes().trim());
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);
        emitReservationUpdated(condominiumId, reservation);

        notificationService.createDirectNotification(
                reservation.getReservedBy(),
                NotificationType.RESERVATION_COUNTER_PROPOSAL,
                "Contraproposta de reserva recebida!",
                "A portaria enviou uma contraproposta de horário para a sua reserva de "
                        + reservation.getCommonArea().getName() + ". Abra o app para visualizar e aceitar.",
                condominiumId,
                reservation.getCommonArea().getCondominium().getName()
        );

        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO modifyAndConfirm(Long id, ReservationDTO.ModifyAndConfirmDTO dto) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        findCommonAreaByIdForUpdate(reservation.getCommonArea().getId());
        Long condominiumId = reservation.getCommonArea().getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        condominiumAccessService.validateStaffAccess(condominiumId);

        if (reservation.getStatus() == ReservationStatus.CANCELLED
                || reservation.getStatus() == ReservationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível confirmar uma reserva cancelada ou recusada.");
        }

        LocalDateTime effectiveStart = dto.startAt() != null ? dto.startAt() : reservation.getStartAt();
        LocalDateTime effectiveEnd = dto.endAt() != null ? dto.endAt() : reservation.getEndAt();
        if (effectiveStart == null || effectiveEnd == null || !effectiveEnd.isAfter(effectiveStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O horário de término deve ser posterior ao horário de início.");
        }

        List<Reservation> activeOverlapping = reservationRepository.findActiveOverlapping(
                reservation.getCommonArea().getId(),
                effectiveStart,
                effectiveEnd
        );
        boolean hasConflict = activeOverlapping.stream()
                .anyMatch(r -> !r.getId().equals(id)
                        && (r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.PENDING));
        if (hasConflict) {
            throw new ReservationConflictException("Já existe outra reserva ativa para este mesmo período.");
        }

        User reviewer = authenticatedUserService.getAuthenticatedUser();
        reservation.setStartAt(effectiveStart);
        reservation.setEndAt(effectiveEnd);
        if (dto.notes() != null && !dto.notes().isBlank()) {
            reservation.setNotes(dto.notes().trim());
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setWaitlistPosition(null);
        reservation.setRejectionReason(null);
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);
        emitReservationUpdated(condominiumId, reservation);

        notificationService.createDirectNotification(
                reservation.getReservedBy(),
                NotificationType.RESERVATION,
                "Reserva confirmada com ajustes!",
                "Sua reserva para " + reservation.getCommonArea().getName()
                        + " foi ajustada e confirmada pela portaria.",
                condominiumId,
                reservation.getCommonArea().getCondominium().getName()
        );

        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO acceptCounterProposal(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        findCommonAreaByIdForUpdate(reservation.getCommonArea().getId());
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!reservation.getReservedBy().getId().equals(user.getId()) && !user.hasRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não autorizado.");
        }
        if (reservation.getStatus() != ReservationStatus.COUNTER_PROPOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta reserva não possui uma contraproposta ativa.");
        }

        LocalDateTime effectiveStart = reservation.getProposedStartAt() != null
                ? reservation.getProposedStartAt()
                : reservation.getStartAt();
        LocalDateTime effectiveEnd = reservation.getProposedEndAt() != null
                ? reservation.getProposedEndAt()
                : reservation.getEndAt();
        if (effectiveStart == null || effectiveEnd == null || !effectiveEnd.isAfter(effectiveStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A contraproposta possui intervalo de horários inválido.");
        }

        List<Reservation> activeOverlapping = reservationRepository.findActiveOverlapping(
                reservation.getCommonArea().getId(),
                effectiveStart,
                effectiveEnd
        );
        boolean hasConfirmedConflict = activeOverlapping.stream()
                .anyMatch(r -> !r.getId().equals(id) && r.getStatus() == ReservationStatus.CONFIRMED);
        if (hasConfirmedConflict) {
            throw new ReservationConflictException("O horário da contraproposta não está mais disponível.");
        }

        reservation.setStartAt(effectiveStart);
        reservation.setEndAt(effectiveEnd);
        reservation.setWaitlistPosition(null);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation = reservationRepository.save(reservation);
        emitReservationUpdated(reservation.getCommonArea().getCondominium().getId(), reservation);
        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO declineCounterProposal(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!reservation.getReservedBy().getId().equals(user.getId()) && !user.hasRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não autorizado.");
        }
        if (reservation.getStatus() != ReservationStatus.COUNTER_PROPOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta reserva não possui uma contraproposta ativa.");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setWaitlistPosition(null);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);
        emitReservationUpdated(reservation.getCommonArea().getCondominium().getId(), reservation);
        promoteNextFromWaitlist(reservation);
        return new ReservationDTO(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> findMineByCondominium(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        User user =
                authenticatedUserService.getAuthenticatedUser();

        return reservationRepository
                .findByReservedByIdAndCommonAreaCondominiumIdOrderByStartAtDesc(
                        user.getId(),
                        condominiumId
                )
                .stream()
                .map(ReservationDTO::new)
                .toList();
    }

    @Transactional
    public void cancel(Long id) {

        Reservation reservation =
                reservationRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reservation not found: " + id
                                )
                        );

        findCommonAreaByIdForUpdate(reservation.getCommonArea().getId());

        Long condominiumId =
                reservation.getCommonArea()
                        .getCondominium()
                        .getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        User user =
                authenticatedUserService
                        .getAuthenticatedUser();

        validateCondominiumAccess(
                condominiumId
        );

        boolean isOwner =
                reservation.getReservedBy()
                        .getId()
                        .equals(user.getId());

        boolean isAdministrativeUser =
                condominiumAccessService.isStaffOrAdmin(user, condominiumId);

        if (!isOwner && !isAdministrativeUser) {
            throw new ForbiddenException(
                    "You cannot cancel this reservation"
            );
        }

        ReservationStatus previousStatus = reservation.getStatus();
        if (previousStatus == ReservationStatus.CANCELLED) {
            return;
        }
        if (previousStatus == ReservationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta reserva já foi recusada pela administração.");
        }
        if (reservation.getEndAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível cancelar uma reserva cujo período já foi concluído.");
        }

        reservation.setStatus(
                ReservationStatus.CANCELLED
        );
        reservation.setWaitlistPosition(null);
        reservation.setCancelledAt(
                LocalDateTime.now()
        );

        reservationRepository.save(reservation);
        emitReservationUpdated(condominiumId, reservation);

        if (previousStatus == ReservationStatus.PENDING
                || previousStatus == ReservationStatus.CONFIRMED
                || previousStatus == ReservationStatus.COUNTER_PROPOSED) {
            promoteNextFromWaitlist(reservation);
        } else if (previousStatus == ReservationStatus.WAITLISTED) {
            reindexWaitlist(reservation.getCommonArea().getId(), reservation.getStartAt(), reservation.getEndAt());
        }
    }

    private void promoteNextFromWaitlist(Reservation freedReservation) {
        CommonArea commonArea = freedReservation.getCommonArea();
        Long commonAreaId = commonArea.getId();
        Long condominiumId = commonArea.getCondominium().getId();

        List<Reservation> waitlisted = reservationRepository.findWaitlistedOverlapping(
                commonAreaId,
                freedReservation.getStartAt(),
                freedReservation.getEndAt()
        );
        if (waitlisted.isEmpty()) {
            return;
        }

        boolean promotedOne = false;
        int position = 1;
        for (Reservation candidate : waitlisted) {
            if (!promotedOne) {
                long remainingConflicts = reservationRepository.countConflictingReservations(
                        commonAreaId,
                        candidate.getStartAt(),
                        candidate.getEndAt(),
                        ReservationStatus.CANCELLED
                );
                if (remainingConflicts == 0) {
                    boolean requiresApproval = commonArea.getRequiresApproval() == null || commonArea.getRequiresApproval();
                    candidate.setStatus(requiresApproval ? ReservationStatus.PENDING : ReservationStatus.CONFIRMED);
                    candidate.setWaitlistPosition(null);
                    reservationRepository.save(candidate);
                    promotedOne = true;

                    notificationService.createDirectNotification(
                            candidate.getReservedBy(),
                            NotificationType.RESERVATION,
                            "Vaga liberada na lista de espera!",
                            "O horário que você aguardava em " + commonArea.getName()
                                    + " ficou disponível e sua solicitação foi promovida automaticamente para "
                                    + (requiresApproval ? "aprovação" : "confirmada") + "!",
                            condominiumId,
                            commonArea.getCondominium().getName()
                    );
                    continue;
                }
            }
            candidate.setWaitlistPosition(position++);
            reservationRepository.save(candidate);
        }
    }

    private void reindexWaitlist(Long commonAreaId, LocalDateTime startAt, LocalDateTime endAt) {
        List<Reservation> waitlisted = reservationRepository.findWaitlistedOverlapping(commonAreaId, startAt, endAt);
        int position = 1;
        for (Reservation entry : waitlisted) {
            entry.setWaitlistPosition(position++);
            reservationRepository.save(entry);
        }
    }

    private void emitReservationUpdated(Long condominiumId, Reservation reservation) {
        if (realtimeEmitterService == null || condominiumId == null || reservation == null) {
            return;
        }
        realtimeEmitterService.sendToCondominium(
                condominiumId,
                RealtimeEventDTO.of("RESERVATION_UPDATED", condominiumId, Map.of(
                        "reservationId", reservation.getId() != null ? reservation.getId() : 0L,
                        "status", reservation.getStatus() != null ? reservation.getStatus().name() : ""
                ))
        );
    }

    private CommonArea findCommonAreaByIdForUpdate(
            Long commonAreaId
    ) {

        return commonAreaRepository
                .findByIdForUpdate(commonAreaId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Common area not found: "
                                        + commonAreaId
                        )
                );
    }

    private void validateCondominiumAccess(
            Long condominiumId
    ) {

        User user =
                authenticatedUserService.getAuthenticatedUser();

        if (user.hasRole("ROLE_ADMIN")) {
            return;
        }

        condominiumAccessService
                .validateUserBelongsToCondominium(
                        user.getId(),
                        condominiumId
                );
    }

    @Transactional(readOnly = true)
    public List<ReservationDTO> findAllByCondominium(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        condominiumAccessService.validateStaffAccess(condominiumId);

        return reservationRepository
                .findByCommonAreaCondominiumIdOrderByStartAtDesc(
                        condominiumId
                )
                .stream()
                .map(ReservationDTO::new)
                .toList();
    }
}
