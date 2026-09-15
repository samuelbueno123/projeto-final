package com.cefet_rj.projeto_final.reservations.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
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
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;

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

        User user =
                authenticatedUserService.getAuthenticatedUser();

        Reservation reservation =
                new Reservation();

        reservation.setCommonArea(commonArea);
        reservation.setReservedBy(user);
        reservation.setStartAt(dto.startAt());
        reservation.setEndAt(dto.endAt());
        reservation.setStatus(
                ReservationStatus.CONFIRMED
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