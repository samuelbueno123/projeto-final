package com.cefet_rj.projeto_final.announcements.services;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.announcements.entities.Announcement;
import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cefet_rj.projeto_final.announcements.events.AnnouncementCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementUpdateDTO;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CondominiumRepository condominiumRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;

    @Transactional
    public AnnouncementDTO create(
            Long condominiumId,
            AnnouncementRequestDTO dto
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        validateCondominiumAccess(condominiumId);

        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Condominium not found: " + condominiumId
                        )
                );

        User authenticatedUser =
                authenticatedUserService.getAuthenticatedUser();

        Unit targetUnit = resolveTargetUnit(authenticatedUser, condominiumId, dto.unitId());

        Announcement announcement = new Announcement();
        announcement.setUnit(targetUnit);

        announcement.setTitle(dto.title().trim());
        announcement.setContent(dto.content().trim());
        announcement.setImageUrl(dto.imageUrl());
        announcement.setCondominium(condominium);
        announcement.setCreatedBy(authenticatedUser);
        announcement.setCreatedAt(LocalDateTime.now());
        announcement.setExpiresAt(dto.expiresAt());
        announcement.setActive(true);

        announcement = announcementRepository.save(announcement);

        eventPublisher.publishEvent(
                new AnnouncementCreatedEvent(
                        announcement.getId(),
                        condominiumId,
                        condominium.getName(),
                        targetUnit != null ? targetUnit.getId() : null,
                        announcement.getTitle(),
                        LocalDateTime.now()
                )
        );

        return new AnnouncementDTO(announcement);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDTO> findActiveByCondominium(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        validateCondominiumAccess(condominiumId);

        return announcementRepository
                .findByCondominiumIdAndActiveTrueOrderByCreatedAtDesc(
                        condominiumId
                )
                .stream()
                .filter(this::isNotExpired)
                .filter(visibleTo(authenticatedUserService.getAuthenticatedUser()))
                .map(AnnouncementDTO::new)
                .toList();
    }

    @Transactional
    public void deactivate(Long id) {

        Announcement announcement =
                findAnnouncementById(id);

        Long condominiumId =
                announcement.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        validateCondominiumAccess(condominiumId);

        announcement.setActive(false);

        announcementRepository.save(announcement);
    }

    /**
     * Resolves the announcement target unit. Managers/admins may publish to the whole
     * condominium or to one unit; doormen may only publish to specific units.
     */
    private Unit resolveTargetUnit(User author, Long condominiumId, Long unitId) {

        boolean isStaffWithFullScope =
                author.hasRole("ROLE_ADMIN") || author.hasRole("ROLE_MANAGER");

        if (unitId == null) {
            if (!isStaffWithFullScope) {
                throw new ForbiddenException(
                        "Doormen can only send announcements to specific units"
                );
            }
            return null;
        }

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Unit not found: " + unitId)
                );

        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new ForbiddenException(
                    "Unit does not belong to this condominium"
            );
        }

        return unit;
    }

    /**
     * Unit-targeted announcements are only visible to the unit residents
     * (and to the condominium staff).
     */
    private java.util.function.Predicate<Announcement> visibleTo(User user) {

        boolean isStaff = user.hasRole("ROLE_ADMIN")
                || user.hasRole("ROLE_MANAGER")
                || user.hasRole("ROLE_DOORMAN");

        if (isStaff) {
            return announcement -> true;
        }

        List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(user.getId());

        return announcement -> announcement.getUnit() == null
                || myUnitIds.contains(announcement.getUnit().getId());
    }

    private boolean isNotExpired(Announcement announcement) {

        return announcement.getExpiresAt() == null
                || announcement.getExpiresAt().isAfter(
                LocalDateTime.now()
        );
    }

    private void validateCondominiumAccess(Long condominiumId) {

        User user = authenticatedUserService.getAuthenticatedUser();

        if (user.hasRole("ROLE_ADMIN")) {
            return;
        }

        condominiumAccessService.validateUserBelongsToCondominium(
                user.getId(),
                condominiumId
        );
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDTO> findAllByCondominiumForAdmin(
            Long condominiumId
    ) {

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        validateCondominiumAccess(condominiumId);

        return announcementRepository
                .findByCondominiumIdOrderByCreatedAtDesc(
                        condominiumId
                )
                .stream()
                .map(AnnouncementDTO::new)
                .toList();
    }

    @Transactional
    public AnnouncementDTO update(
            Long id,
            AnnouncementUpdateDTO dto
    ) {

        Announcement announcement =
                findAnnouncementById(id);

        Long condominiumId =
                announcement.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        validateCondominiumAccess(condominiumId);

        announcement.setTitle(
                dto.title().trim()
        );

        announcement.setContent(
                dto.content().trim()
        );

        announcement.setExpiresAt(
                dto.expiresAt()
        );

        announcement =
                announcementRepository.save(announcement);

        return new AnnouncementDTO(announcement);
    }

    @Transactional
    public void activate(Long id) {

        Announcement announcement =
                findAnnouncementById(id);

        Long condominiumId =
                announcement.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        validateCondominiumAccess(condominiumId);

        announcement.setActive(true);

        announcementRepository.save(announcement);
    }

    private Announcement findAnnouncementById(Long id) {

        return announcementRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Announcement not found: " + id
                        )
                );
    }
}
