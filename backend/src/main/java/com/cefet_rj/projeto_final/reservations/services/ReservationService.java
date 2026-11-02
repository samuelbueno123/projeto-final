package com.cefet_rj.projeto_final.reservations.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cefet_rj.projeto_final.reservations.events.ReservationCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

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

        if (!commonArea.getActive()) {
            throw new ForbiddenException(
                    "Common area is not available for reservations"
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

        if (conflicts > 0) {
            throw new ReservationConflictException(
                    "Common area is already reserved "
                            + "for the selected period"
            );
        }

        User authenticatedUser =
                authenticatedUserService.getAuthenticatedUser();

        User user = authenticatedUser;
        if (authenticatedUser.hasRole("ROLE_ADMIN") && dto.reservedById() != null) {
            user = userRepository.findById(dto.reservedById())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + dto.reservedById()));
        }

        boolean isAdminOrManager = authenticatedUser.hasRole("ROLE_ADMIN")
                || authenticatedUser.hasRole("ROLE_MANAGER");
        boolean requiresApproval = commonArea.getRequiresApproval() != null
                ? commonArea.getRequiresApproval()
                : true;

        Reservation reservation =
                new Reservation();

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
        reservation.setStatus(
                isAdminOrManager
                        ? ReservationStatus.CONFIRMED
                        : ReservationStatus.PENDING
        );
        reservation.setCreatedAt(
                LocalDateTime.now()
        );

        reservation =
                reservationRepository.save(reservation);

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

        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO approve(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        Long condominiumId = reservation.getCommonArea().getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        validateCondominiumAccess(condominiumId);

        User reviewer = authenticatedUserService.getAuthenticatedUser();
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setRejectionReason(null);
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

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
        validateCondominiumAccess(condominiumId);

        User reviewer = authenticatedUserService.getAuthenticatedUser();
        String cleanReason = reason == null || reason.isBlank()
                ? "Solicitação não aprovada pela administração."
                : reason.trim();
        reservation.setStatus(ReservationStatus.REJECTED);
        reservation.setRejectionReason(cleanReason);
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

        notificationService.createDirectNotification(
                reservation.getReservedBy(),
                NotificationType.RESERVATION,
                "Reserva recusada",
                "Sua solicitação para " + reservation.getCommonArea().getName()
                        + " foi recusada: " + cleanReason,
                condominiumId,
                reservation.getCommonArea().getCondominium().getName()
        );

        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO counterPropose(Long id, ReservationDTO.CounterProposalDTO dto) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        Long condominiumId = reservation.getCommonArea().getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        validateCondominiumAccess(condominiumId);

        User reviewer = authenticatedUserService.getAuthenticatedUser();
        reservation.setStatus(ReservationStatus.COUNTER_PROPOSED);
        reservation.setProposedStartAt(dto.proposedStartAt());
        reservation.setProposedEndAt(dto.proposedEndAt());
        reservation.setCounterProposalNotes(dto.notes() == null ? null : dto.notes().trim());
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

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
        Long condominiumId = reservation.getCommonArea().getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        validateCondominiumAccess(condominiumId);

        User reviewer = authenticatedUserService.getAuthenticatedUser();
        if (dto.startAt() != null) reservation.setStartAt(dto.startAt());
        if (dto.endAt() != null) reservation.setEndAt(dto.endAt());
        if (dto.notes() != null && !dto.notes().isBlank()) {
            reservation.setNotes(dto.notes().trim());
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setRejectionReason(null);
        reservation.setReviewedBy(reviewer);
        reservation.setReviewedAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);

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
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!reservation.getReservedBy().getId().equals(user.getId()) && !user.hasRole("ROLE_ADMIN")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Não autorizado.");
        }
        if (reservation.getStatus() != ReservationStatus.COUNTER_PROPOSED) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Esta reserva não possui uma contraproposta ativa.");
        }
        if (reservation.getProposedStartAt() != null) {
            reservation.setStartAt(reservation.getProposedStartAt());
        }
        if (reservation.getProposedEndAt() != null) {
            reservation.setEndAt(reservation.getProposedEndAt());
        }
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation = reservationRepository.save(reservation);
        return new ReservationDTO(reservation);
    }

    @Transactional
    public ReservationDTO declineCounterProposal(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!reservation.getReservedBy().getId().equals(user.getId()) && !user.hasRole("ROLE_ADMIN")) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Não autorizado.");
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation = reservationRepository.save(reservation);
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
                user.hasRole("ROLE_ADMIN")
                        || user.hasRole("ROLE_MANAGER");

        if (!isOwner && !isAdministrativeUser) {
            throw new ForbiddenException(
                    "You cannot cancel this reservation"
            );
        }

        if (reservation.getStatus()
                == ReservationStatus.CANCELLED) {
            return;
        }

        reservation.setStatus(
                ReservationStatus.CANCELLED
        );

        reservation.setCancelledAt(
                LocalDateTime.now()
        );

        reservationRepository.save(reservation);
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

        validateCondominiumAccess(condominiumId);

        return reservationRepository
                .findByCommonAreaCondominiumIdOrderByStartAtDesc(
                        condominiumId
                )
                .stream()
                .map(ReservationDTO::new)
                .toList();
    }
}
