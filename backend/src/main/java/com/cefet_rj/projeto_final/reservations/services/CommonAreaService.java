package com.cefet_rj.projeto_final.reservations.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaDTO;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaRequestDTO;
import com.cefet_rj.projeto_final.reservations.entities.CommonArea;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.repositories.CommonAreaRepository;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cefet_rj.projeto_final.reservations.dtos.CommonAreaUpdateDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonAreaService {

    private final CommonAreaRepository commonAreaRepository;
    private final ReservationRepository reservationRepository;
    private final CondominiumRepository condominiumRepository;

    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;

    @Transactional
    public CommonAreaDTO create(
            Long condominiumId,
            CommonAreaRequestDTO dto
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        Condominium condominium =
                condominiumRepository.findById(condominiumId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Condominium not found: "
                                                + condominiumId
                                )
                        );

        CommonArea commonArea = new CommonArea();

        commonArea.setName(dto.name().trim());
        commonArea.setDescription(
                dto.description() == null
                        ? null
                        : dto.description().trim()
        );
        commonArea.setCapacity(dto.capacity());
        commonArea.setRequiresApproval(dto.requiresApproval() != null ? dto.requiresApproval() : true);
        commonArea.setRules(dto.rules() == null || dto.rules().isBlank() ? null : dto.rules().trim());
        commonArea.setOpeningHour(dto.openingHour() != null ? dto.openingHour() : 8);
        commonArea.setClosingHour(dto.closingHour() != null ? dto.closingHour() : 22);
        commonArea.setActive(true);
        commonArea.setCondominium(condominium);

        commonArea =
                commonAreaRepository.save(commonArea);

        return new CommonAreaDTO(commonArea);
    }

    @Transactional(readOnly = true)
    public List<CommonAreaDTO> findActiveByCondominium(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        return commonAreaRepository
                .findByCondominiumIdAndActiveTrueOrderByNameAsc(
                        condominiumId
                )
                .stream()
                .map(CommonAreaDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommonAreaDTO> findAllByCondominium(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        return commonAreaRepository
                .findByCondominiumIdOrderByNameAsc(
                        condominiumId
                )
                .stream()
                .map(CommonAreaDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public CommonAreaAvailabilityDTO getAvailabilityForDate(Long commonAreaId, LocalDate date) {
        CommonArea commonArea = findCommonAreaById(commonAreaId);
        Long condominiumId = commonArea.getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        validateCondominiumAccess(condominiumId);

        LocalDate targetDate = date != null ? date : LocalDate.now().plusDays(1);
        int openH = commonArea.getOpeningHour() != null ? commonArea.getOpeningHour() : 8;
        int closeH = commonArea.getClosingHour() != null ? commonArea.getClosingHour() : 22;
        if (closeH <= openH) {
            openH = 8;
            closeH = 22;
        }

        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.plusDays(1).atStartOfDay();

        List<Reservation> overlapping = reservationRepository.findActiveOverlapping(
                commonAreaId, dayStart, dayEnd
        );

        List<OccupiedSlotDTO> occupiedSlots = overlapping.stream()
                .map(r -> new OccupiedSlotDTO(
                        r.getId(),
                        r.getStartAt(),
                        r.getEndAt(),
                        r.getStatus().name(),
                        (r.getReservedBy().getFirstName() + " " + r.getReservedBy().getLastName()).trim()
                ))
                .toList();

        List<FreeWindowDTO> freeWindows = new ArrayList<>();
        LocalDateTime cursor = targetDate.atTime(openH, 0);
        LocalDateTime windowLimit = closeH == 24
                ? targetDate.plusDays(1).atStartOfDay()
                : targetDate.atTime(closeH, 0);

        for (Reservation r : overlapping) {
            LocalDateTime rStart = r.getStartAt().isBefore(cursor) ? cursor : r.getStartAt();
            LocalDateTime rEnd = r.getEndAt().isAfter(windowLimit) ? windowLimit : r.getEndAt();
            if (rStart.isAfter(cursor)) {
                freeWindows.add(new FreeWindowDTO(cursor, rStart));
            }
            if (rEnd.isAfter(cursor)) {
                cursor = rEnd;
            }
        }
        if (cursor.isBefore(windowLimit)) {
            freeWindows.add(new FreeWindowDTO(cursor, windowLimit));
        }

        return new CommonAreaAvailabilityDTO(
                commonArea.getId(),
                commonArea.getName(),
                targetDate,
                openH,
                closeH,
                commonArea.getRequiresApproval() != null ? commonArea.getRequiresApproval() : true,
                commonArea.getRules(),
                occupiedSlots,
                freeWindows
        );
    }

    @Transactional(readOnly = true)
    public AvailabilityCheckDTO checkIntervalAvailability(
            Long commonAreaId,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        CommonArea commonArea = findCommonAreaById(commonAreaId);
        Long condominiumId = commonArea.getCondominium().getId();
        featureAccessService.validateFeatureEnabled(condominiumId, FeatureCode.RESERVATIONS);
        validateCondominiumAccess(condominiumId);

        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            return new AvailabilityCheckDTO(
                    false,
                    "O horário final deve ser posterior ao horário inicial.",
                    commonArea.getRequiresApproval() != null ? commonArea.getRequiresApproval() : true,
                    List.of()
            );
        }

        if (!Boolean.TRUE.equals(commonArea.getActive())) {
            return new AvailabilityCheckDTO(
                    false,
                    "Esta área comum está temporariamente desativada para reservas.",
                    commonArea.getRequiresApproval() != null ? commonArea.getRequiresApproval() : true,
                    List.of()
            );
        }

        List<Reservation> conflicts = reservationRepository.findActiveOverlapping(
                commonAreaId, startAt, endAt
        );
        List<OccupiedSlotDTO> conflictDtos = conflicts.stream()
                .map(r -> new OccupiedSlotDTO(
                        r.getId(),
                        r.getStartAt(),
                        r.getEndAt(),
                        r.getStatus().name(),
                        (r.getReservedBy().getFirstName() + " " + r.getReservedBy().getLastName()).trim()
                ))
                .toList();

        boolean available = conflictDtos.isEmpty();
        boolean requiresApproval = commonArea.getRequiresApproval() != null ? commonArea.getRequiresApproval() : true;
        String message = available
                ? (requiresApproval
                    ? "Horário disponível! Ao enviar, sua solicitação irá para aprovação do síndico."
                    : "Horário disponível para reserva imediata!")
                : "Horário indisponível: já existe solicitação ou reserva confirmada neste intervalo.";

        return new AvailabilityCheckDTO(available, message, requiresApproval, conflictDtos);
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

    @Transactional
    public CommonAreaDTO update(
            Long id,
            CommonAreaUpdateDTO dto
    ) {

        CommonArea commonArea =
                findCommonAreaById(id);

        Long condominiumId =
                commonArea.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        commonArea.setName(
                dto.name().trim()
        );

        commonArea.setDescription(
                dto.description() == null
                        ? null
                        : dto.description().trim()
        );

        commonArea.setCapacity(
                dto.capacity()
        );

        if (dto.requiresApproval() != null) {
            commonArea.setRequiresApproval(dto.requiresApproval());
        }
        if (dto.rules() != null) {
            commonArea.setRules(dto.rules().isBlank() ? null : dto.rules().trim());
        }
        if (dto.openingHour() != null) {
            commonArea.setOpeningHour(dto.openingHour());
        }
        if (dto.closingHour() != null) {
            commonArea.setClosingHour(dto.closingHour());
        }

        commonArea =
                commonAreaRepository.save(commonArea);

        return new CommonAreaDTO(commonArea);
    }

    @Transactional
    public void activate(Long id) {

        CommonArea commonArea =
                findCommonAreaById(id);

        Long condominiumId =
                commonArea.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        commonArea.setActive(true);

        commonAreaRepository.save(commonArea);
    }

    @Transactional
    public void deactivate(Long id) {

        CommonArea commonArea =
                findCommonAreaById(id);

        Long condominiumId =
                commonArea.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.RESERVATIONS
        );

        validateCondominiumAccess(condominiumId);

        commonArea.setActive(false);

        commonAreaRepository.save(commonArea);
    }

    private CommonArea findCommonAreaById(
            Long id
    ) {

        return commonAreaRepository
                .findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Common area not found: " + id
                        )
                );
    }

    public record OccupiedSlotDTO(
            Long reservationId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String status,
            String reservedByName
    ) {}

    public record FreeWindowDTO(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {}

    public record CommonAreaAvailabilityDTO(
            Long commonAreaId,
            String commonAreaName,
            LocalDate date,
            Integer openingHour,
            Integer closingHour,
            Boolean requiresApproval,
            String rules,
            List<OccupiedSlotDTO> occupiedSlots,
            List<FreeWindowDTO> freeWindows
    ) {}

    public record AvailabilityCheckDTO(
            Boolean available,
            String message,
            Boolean requiresApproval,
            List<OccupiedSlotDTO> conflicts
    ) {}
}
