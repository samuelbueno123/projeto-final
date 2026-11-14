package com.cefet_rj.projeto_final.announcements.services;

import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementDTO.AnnouncementReadDTO;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementRequestDTO;
import com.cefet_rj.projeto_final.announcements.dtos.AnnouncementUpdateDTO;
import com.cefet_rj.projeto_final.announcements.entities.Announcement;
import com.cefet_rj.projeto_final.announcements.entities.AnnouncementRead;
import com.cefet_rj.projeto_final.announcements.events.AnnouncementCreatedEvent;
import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementReadRepository;
import com.cefet_rj.projeto_final.announcements.repositories.AnnouncementRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumFeatureAccessService;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final CondominiumRepository condominiumRepository;
    private final SectorRepository sectorRepository;
    private final MembershipRepository membershipRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final CondominiumFeatureAccessService featureAccessService;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService realtimeEmitterService;

    @Transactional
    public AnnouncementDTO create(
            Long condominiumId,
            AnnouncementRequestDTO dto
    ) {
        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        condominiumAccessService.validateStaffAccess(condominiumId);

        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Condominium not found: " + condominiumId
                        )
                );

        User authenticatedUser =
                authenticatedUserService.getAuthenticatedUser();

        Unit targetUnit = resolveTargetUnit(authenticatedUser, condominiumId, dto.unitId());
        Sector targetSector = resolveTargetSector(authenticatedUser, condominiumId, dto.sectorId());

        String normalizedRole = null;
        if (dto.targetRole() != null && !dto.targetRole().isBlank()) {
            normalizedRole = dto.targetRole().trim().toUpperCase(Locale.ROOT);
            if (!normalizedRole.startsWith("ROLE_")) {
                normalizedRole = "ROLE_" + normalizedRole;
            }
        }

        Announcement announcement = new Announcement();
        announcement.setUnit(targetUnit);
        announcement.setSector(targetSector);
        announcement.setTargetRole(normalizedRole);

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

        AnnouncementDTO dtoResult = new AnnouncementDTO(announcement, 0L, false);
        if (realtimeEmitterService != null) {
            realtimeEmitterService.sendToCondominium(
                    condominiumId,
                    com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO.of(
                            "ANNOUNCEMENT_CREATED",
                            condominiumId,
                            dtoResult
                    )
            );
        }
        return dtoResult;
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

        User currentUser = authenticatedUserService.getAuthenticatedUser();
        List<Announcement> announcements = announcementRepository
                .findByCondominiumIdAndActiveTrueOrderByCreatedAtDesc(condominiumId)
                .stream()
                .filter(this::isNotExpired)
                .filter(visibleTo(currentUser, condominiumId))
                .toList();

        return enrichWithReadStats(announcements, currentUser.getId());
    }

    @Transactional
    public AnnouncementDTO markAsRead(Long id) {
        Announcement announcement = findAnnouncementById(id);
        Long condominiumId = announcement.getCondominium().getId();
        validateCondominiumAccess(condominiumId);

        User user = authenticatedUserService.getAuthenticatedUser();
        Optional<AnnouncementRead> existing = announcementReadRepository.findByAnnouncementIdAndUserId(id, user.getId());
        if (existing.isEmpty()) {
            AnnouncementRead read = new AnnouncementRead();
            read.setAnnouncement(announcement);
            read.setUser(user);
            read.setReadAt(LocalDateTime.now());
            announcementReadRepository.save(read);
        }

        long count = announcementReadRepository.countByAnnouncementId(id);
        return new AnnouncementDTO(announcement, count, true);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementReadDTO> listReads(Long id) {
        Announcement announcement = findAnnouncementById(id);
        Long condominiumId = announcement.getCondominium().getId();
        condominiumAccessService.validateStaffAccess(condominiumId);

        return announcementReadRepository.findByAnnouncementIdOrderByReadAtDesc(id)
                .stream()
                .map(AnnouncementReadDTO::new)
                .toList();
    }

    @Transactional
    public void deactivate(Long id) {
        Announcement announcement = findAnnouncementById(id);
        Long condominiumId = announcement.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        condominiumAccessService.validateManagerAccess(condominiumId);

        announcement.setActive(false);
        announcementRepository.save(announcement);
    }

    private Unit resolveTargetUnit(User author, Long condominiumId, Long unitId) {
        boolean isManagerOrAdmin = condominiumAccessService.isManagerOrAdmin(author, condominiumId);

        if (unitId == null) {
            if (!isManagerOrAdmin) {
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

    private Sector resolveTargetSector(User author, Long condominiumId, Long sectorId) {
        if (sectorId == null) {
            return null;
        }
        if (!condominiumAccessService.isManagerOrAdmin(author, condominiumId)) {
            throw new ForbiddenException("Apenas o síndico ou administrador pode enviar comunicados por bloco/setor.");
        }
        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector not found: " + sectorId));
        if (!sector.getCondominium().getId().equals(condominiumId)) {
            throw new ForbiddenException("Sector does not belong to this condominium");
        }
        return sector;
    }

    private java.util.function.Predicate<Announcement> visibleTo(User user, Long condominiumId) {
        boolean isManagerOrAdmin = condominiumAccessService.isManagerOrAdmin(user, condominiumId);
        if (isManagerOrAdmin) {
            return announcement -> true;
        }

        boolean isDoorman = membershipRepository.hasActiveRoleInCondominium(user.getId(), condominiumId, "ROLE_DOORMAN");
        List<Resident> myResidents = residentRepository.findByUserIdAndCondominiumId(user.getId(), condominiumId);
        Set<Long> myUnitIds = new HashSet<>();
        Set<Long> mySectorIds = new HashSet<>();
        for (Resident r : myResidents) {
            if (r.getUnit() != null) {
                myUnitIds.add(r.getUnit().getId());
                if (r.getUnit().getSector() != null) {
                    mySectorIds.add(r.getUnit().getSector().getId());
                }
            }
        }

        return announcement -> {
            if (announcement.getTargetRole() != null && !announcement.getTargetRole().isBlank()) {
                String role = announcement.getTargetRole();
                boolean matchesRole = membershipRepository.hasActiveRoleInCondominium(user.getId(), condominiumId, role)
                        || user.hasRole(role);
                if (!matchesRole) {
                    return false;
                }
            }
            if (announcement.getUnit() != null) {
                return isDoorman || myUnitIds.contains(announcement.getUnit().getId());
            }
            if (announcement.getSector() != null) {
                return isDoorman || mySectorIds.contains(announcement.getSector().getId());
            }
            return true;
        };
    }

    private List<AnnouncementDTO> enrichWithReadStats(List<Announcement> announcements, Long viewerId) {
        if (announcements.isEmpty()) {
            return List.of();
        }
        List<Long> ids = announcements.stream().map(Announcement::getId).toList();
        List<AnnouncementRead> reads = announcementReadRepository.findAllByAnnouncementIds(ids);

        Map<Long, Long> countsByAnnouncement = new HashMap<>();
        Set<Long> readByViewer = new HashSet<>();
        for (AnnouncementRead r : reads) {
            Long aid = r.getAnnouncement().getId();
            countsByAnnouncement.merge(aid, 1L, Long::sum);
            if (r.getUser().getId().equals(viewerId)) {
                readByViewer.add(aid);
            }
        }

        return announcements.stream()
                .map(a -> new AnnouncementDTO(
                        a,
                        countsByAnnouncement.getOrDefault(a.getId(), 0L),
                        readByViewer.contains(a.getId())
                ))
                .toList();
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

        condominiumAccessService.validateManagerAccess(condominiumId);

        User currentUser = authenticatedUserService.getAuthenticatedUser();
        List<Announcement> list = announcementRepository
                .findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        return enrichWithReadStats(list, currentUser.getId());
    }

    @Transactional
    public AnnouncementDTO update(
            Long id,
            AnnouncementUpdateDTO dto
    ) {
        Announcement announcement = findAnnouncementById(id);
        Long condominiumId = announcement.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        condominiumAccessService.validateManagerAccess(condominiumId);

        announcement.setTitle(dto.title().trim());
        announcement.setContent(dto.content().trim());
        announcement.setExpiresAt(dto.expiresAt());

        announcement = announcementRepository.save(announcement);

        long count = announcementReadRepository.countByAnnouncementId(id);
        User currentUser = authenticatedUserService.getAuthenticatedUser();
        boolean readByMe = announcementReadRepository.findByAnnouncementIdAndUserId(id, currentUser.getId()).isPresent();

        return new AnnouncementDTO(announcement, count, readByMe);
    }

    @Transactional
    public void activate(Long id) {
        Announcement announcement = findAnnouncementById(id);
        Long condominiumId = announcement.getCondominium().getId();

        featureAccessService.validateFeatureEnabled(
                condominiumId,
                FeatureCode.ANNOUNCEMENTS
        );

        condominiumAccessService.validateManagerAccess(condominiumId);

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
