package com.cefet_rj.projeto_final.announcements.services;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.announcements.entities.Announcement;
import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
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

        Announcement announcement = new Announcement();

        announcement.setTitle(dto.title().trim());
        announcement.setContent(dto.content().trim());
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