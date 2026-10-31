package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.dtos.AddManagerRequestDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.MembershipDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.MembershipInvitationDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.MembershipInvitationRequestDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.MembershipInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.MembershipUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.enums.ResidentType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final CondominiumRepository condominiumRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;
    private final AuthenticatedUserService authenticatedUserService;

    @Transactional(readOnly = true)
    public List<MembershipDTO> findMine() {
        Long userId = authenticatedUserService.getAuthenticatedUser().getId();
        return membershipRepository.findAllByUserId(userId).stream()
                .map(this::enrichWithUnitInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipDTO> findMyPendingAndRequests() {
        Long userId = authenticatedUserService.getAuthenticatedUser().getId();
        return membershipRepository.findPendingAndRequestedByUserId(userId).stream()
                .map(this::enrichWithUnitInfo)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableCondominiumDTO> findAvailableCondominiums() {
        return condominiumRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .map(c -> new AvailableCondominiumDTO(c.getId(), c.getName(), c.getAddress()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AvailableUnitDTO> findAvailableUnits(Long condominiumId) {
        ensureCondominiumExists(condominiumId);
        return unitRepository.findAllForCondominium(condominiumId).stream()
                .map(u -> {
                    int count = residentRepository.findAllByUnitIdWithUser(u.getId()).size();
                    return new AvailableUnitDTO(
                            u.getId(),
                            u.getIdentifier(),
                            u.getSector().getId(),
                            u.getSector().getName(),
                            count
                    );
                })
                .toList();
    }

    @Transactional
    public MembershipDTO requestMembership(Long condominiumId, MembershipRequestCreateDTO dto) {
        User user = authenticatedUserService.getAuthenticatedUser();
        Condominium condominium = getCondominiumOrThrow(condominiumId);

        String authority = dto.authority() == null || dto.authority().isBlank()
                ? "ROLE_RESIDENT"
                : dto.authority().trim().toUpperCase(Locale.ROOT);
        if (!authority.startsWith("ROLE_")) {
            authority = "ROLE_" + authority;
        }
        if (!"ROLE_RESIDENT".equals(authority) && !"ROLE_DOORMAN".equals(authority)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A solicitação de vínculo deve ser para ROLE_RESIDENT ou ROLE_DOORMAN."
            );
        }

        List<Membership> existing = membershipRepository.findByUserIdAndCondominiumIdAndAuthority(
                user.getId(),
                condominiumId,
                authority
        );
        for (Membership item : existing) {
            if (item.getStatus() == MembershipStatus.ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Você já possui vínculo ativo neste condomínio."
                );
            }
            if (item.getStatus() == MembershipStatus.PENDING || item.getStatus() == MembershipStatus.REQUESTED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Já existe uma solicitação ou convite pendente para você neste condomínio."
                );
            }
        }

        Unit requestedUnit = null;
        if (dto.unitId() != null) {
            requestedUnit = unitRepository.findById(dto.unitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + dto.unitId()));
            if (!requestedUnit.getSector().getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade não pertence a este condomínio.");
            }
        }

        ResidentType resType = ResidentType.OWNER;
        if (dto.residentType() != null && !dto.residentType().isBlank()) {
            try {
                resType = ResidentType.valueOf(dto.residentType().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Role role = getOrCreateRole(authority);
        Membership req = new Membership();
        req.setUser(user);
        req.setCondominium(condominium);
        req.setRole(role);
        req.setStatus(MembershipStatus.REQUESTED);
        req.setRequestedUnit(requestedUnit);
        req.setRequestedResidentType("ROLE_RESIDENT".equals(authority) ? resType : null);
        req.setRequestMessage(dto.message() == null || dto.message().isBlank() ? null : dto.message().trim());
        req.setCreatedAt(LocalDateTime.now());
        req = membershipRepository.saveAndFlush(req);

        // Notify condominium managers
        List<Membership> managers = membershipRepository.findActiveManagersByCondominiumId(condominiumId);
        String unitDesc = requestedUnit != null
                ? " (" + requestedUnit.getSector().getName() + " • Unid. " + requestedUnit.getIdentifier() + ")"
                : "";
        for (Membership mgr : managers) {
            Notification notice = new Notification();
            notice.setRecipient(mgr.getUser());
            notice.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
            notice.setTitle("Solicitação de vínculo: " + condominium.getName());
            notice.setMessage(user.getFirstName() + " " + user.getLastName()
                    + " solicitou vínculo como "
                    + ("ROLE_DOORMAN".equals(authority) ? "Porteiro" : "Morador")
                    + unitDesc + ".");
            notice.setCondominiumId(condominiumId);
            notice.setCondominiumName(condominium.getName());
            notice.setRead(false);
            notice.setCreatedAt(LocalDateTime.now());
            notificationRepository.save(notice);
        }

        return enrichWithUnitInfo(req);
    }

    @Transactional
    public MembershipDTO approveMembershipRequest(
            Long condominiumId,
            Long membershipId,
            Long unitIdOverride,
            String residentTypeOverride
    ) {
        ensureCondominiumExists(condominiumId);
        ensureViewerIsManagerOrAdminOf(condominiumId);

        Membership membership = membershipRepository.findByIdWithDetails(membershipId)
                .filter(m -> m.getCondominium().getId().equals(condominiumId))
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada neste condomínio."));

        if (membership.getStatus() != MembershipStatus.REQUESTED
                && membership.getStatus() != MembershipStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta solicitação já foi processada.");
        }

        Unit targetUnit = membership.getRequestedUnit();
        if (unitIdOverride != null) {
            targetUnit = unitRepository.findById(unitIdOverride)
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitIdOverride));
            if (!targetUnit.getSector().getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade não pertence a este condomínio.");
            }
            membership.setRequestedUnit(targetUnit);
        }

        ResidentType targetType = membership.getRequestedResidentType() != null
                ? membership.getRequestedResidentType()
                : ResidentType.OWNER;
        if (residentTypeOverride != null && !residentTypeOverride.isBlank()) {
            try {
                targetType = ResidentType.valueOf(residentTypeOverride.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
            membership.setRequestedResidentType(targetType);
        }

        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setRespondedAt(LocalDateTime.now());
        membership = membershipRepository.saveAndFlush(membership);

        User targetUser = membership.getUser();
        targetUser.addRole(membership.getRole());
        userRepository.save(targetUser);

        if ("ROLE_RESIDENT".equals(membership.getRole().getAuthority()) && targetUnit != null) {
            linkUserToUnit(targetUser, targetUnit, targetType);
        }

        Notification notice = new Notification();
        notice.setRecipient(targetUser);
        notice.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
        notice.setTitle("Vínculo aprovado em " + membership.getCondominium().getName());
        notice.setMessage("Sua solicitação de acesso ao condomínio " + membership.getCondominium().getName() + " foi aprovada!");
        notice.setCondominiumId(condominiumId);
        notice.setCondominiumName(membership.getCondominium().getName());
        notice.setRead(false);
        notice.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notice);

        return enrichWithUnitInfo(membership);
    }

    @Transactional
    public void rejectMembershipRequest(Long condominiumId, Long membershipId) {
        ensureCondominiumExists(condominiumId);
        ensureViewerIsManagerOrAdminOf(condominiumId);

        Membership membership = membershipRepository.findByIdWithDetails(membershipId)
                .filter(m -> m.getCondominium().getId().equals(condominiumId))
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada neste condomínio."));

        membership.setStatus(MembershipStatus.DECLINED);
        membership.setRespondedAt(LocalDateTime.now());
        membershipRepository.saveAndFlush(membership);

        Notification notice = new Notification();
        notice.setRecipient(membership.getUser());
        notice.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
        notice.setTitle("Solicitação recusada");
        notice.setMessage("Sua solicitação de vínculo no condomínio " + membership.getCondominium().getName() + " não foi aprovada.");
        notice.setCondominiumId(condominiumId);
        notice.setCondominiumName(membership.getCondominium().getName());
        notice.setRead(false);
        notice.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notice);
    }

    @Transactional
    public MembershipDTO assignResidentUnit(
            Long condominiumId,
            Long membershipId,
            Long unitId,
            String residentType
    ) {
        ensureCondominiumExists(condominiumId);
        ensureViewerIsManagerOrAdminOf(condominiumId);

        Membership membership = membershipRepository.findByIdWithDetails(membershipId)
                .filter(m -> m.getCondominium().getId().equals(condominiumId))
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo não encontrado neste condomínio."));

        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitId));
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade não pertence a este condomínio.");
        }

        ResidentType rType = ResidentType.OWNER;
        if (residentType != null && !residentType.isBlank()) {
            try {
                rType = ResidentType.valueOf(residentType.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        membership.setRequestedUnit(unit);
        membership.setRequestedResidentType(rType);
        membership = membershipRepository.saveAndFlush(membership);

        linkUserToUnit(membership.getUser(), unit, rType);
        return enrichWithUnitInfo(membership);
    }

    private void linkUserToUnit(User user, Unit unit, ResidentType type) {
        Long condoId = unit.getSector().getCondominium().getId();
        Resident resident = residentRepository
                .findFirstByUserIdAndUnitSectorCondominiumId(user.getId(), condoId)
                .orElseGet(Resident::new);
        resident.setUser(user);
        resident.setUnit(unit);
        resident.setType(type != null ? type : ResidentType.OWNER);
        residentRepository.save(resident);
    }

    private MembershipDTO enrichWithUnitInfo(Membership m) {
        MembershipDTO base = new MembershipDTO(m);
        if ( m.getUser() != null && m.getCondominium() != null) {
            var resOpt = residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(
                    m.getUser().getId(),
                    m.getCondominium().getId()
            );
            if (resOpt.isPresent()) {
                Resident r = resOpt.get();
                Unit u = r.getUnit();
                String label = u.getSector().getName() + " • Unid. " + u.getIdentifier();
                String rType = r.getType() != null ? r.getType().name() : null;
                return base.withUnitInfo(u.getId(), label, rType);
            }
        }
        return base;
    }

    @Transactional(readOnly = true)
    public Page<MembershipDTO> findAllPaged(Pageable pageable) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        Page<Membership> list = viewer.hasRole("ROLE_ADMIN")
                ? membershipRepository.findAll(pageable)
                : membershipRepository.findPagedForManager(viewer.getId(), pageable);
        return list.map(this::enrichWithUnitInfo);
    }

    @Transactional(readOnly = true)
    public MembershipDTO findById(Long id) {
        Membership entity = membershipRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        User viewer = authenticatedUserService.getAuthenticatedUser();
        if (!viewer.hasRole("ROLE_ADMIN")
                && !membershipRepository.isManagerOf(viewer.getId(), entity.getCondominium().getId())) {
            throw new ForbiddenException("Você não administra este condomínio.");
        }
        return enrichWithUnitInfo(entity);
    }

    @Transactional
    public MembershipDTO insert(MembershipInsertDTO dto) {
        Membership entity = new Membership();
        linkAssociations(dto.condominiumId(), entity, dto.authority(), dto.userId());
        entity.setStatus(MembershipStatus.ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setRespondedAt(LocalDateTime.now());
        entity = membershipRepository.saveAndFlush(entity);
        syncUserRoles(entity.getUser());
        return enrichWithUnitInfo(entity);
    }

    @Transactional
    public MembershipDTO update(Long id, MembershipUpdateDTO dto) {
        try {
            Membership entity = membershipRepository.getReferenceById(id);
            User previousUser = entity.getUser();
            linkAssociations(dto.condominiumId(), entity, dto.authority(), dto.userId());
            entity = membershipRepository.saveAndFlush(entity);
            if (previousUser != null) {
                syncUserRoles(previousUser);
            }
            syncUserRoles(entity.getUser());
            return enrichWithUnitInfo(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Id " + id + " not found");
        }
    }

    @Transactional
    public void delete(Long id) {
        Membership entity = membershipRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
        User targetUser = entity.getUser();
        try {
            membershipRepository.delete(entity);
            membershipRepository.flush();
            syncUserRoles(targetUser);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Integrity violation");
        }
    }

    @Transactional(readOnly = true)
    public List<MembershipDTO> findByCondominiumForManager(Long condominiumId) {
        ensureCondominiumExists(condominiumId);
        ensureViewerIsManagerOrAdminOf(condominiumId);
        return membershipRepository.findActiveAndPendingByCondominiumId(condominiumId).stream()
                .map(this::enrichWithUnitInfo)
                .toList();
    }

    @Transactional
    public MembershipInvitationDTO inviteMember(
            Long condominiumId,
            MembershipInvitationRequestDTO dto
    ) {
        Condominium condominium = getCondominiumOrThrow(condominiumId);
        ensureViewerIsManagerOrAdminOf(condominiumId);

        String authority = dto.authority() == null ? "" : dto.authority().trim().toUpperCase();
        if (!"ROLE_RESIDENT".equals(authority) && !"ROLE_DOORMAN".equals(authority)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O convite do síndico deve ser para ROLE_RESIDENT ou ROLE_DOORMAN."
            );
        }

        String normalizedEmail = dto.email().trim().toLowerCase();
        User recipient = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma conta encontrada para o email " + normalizedEmail
                ));

        List<Membership> existing = membershipRepository.findByUserIdAndCondominiumIdAndAuthority(
                recipient.getId(),
                condominiumId,
                authority
        );
        for (Membership item : existing) {
            if (item.getStatus() == MembershipStatus.ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Este usuário já possui esse vínculo ativo neste condomínio."
                );
            }
            if (item.getStatus() == MembershipStatus.PENDING) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Já existe um convite pendente para este usuário neste condomínio."
                );
            }
        }

        Role role = getOrCreateRole(authority);

        Unit requestedUnit = null;
        if (dto.unitId() != null) {
            requestedUnit = unitRepository.findById(dto.unitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + dto.unitId()));
        }
        ResidentType requestedType = ResidentType.OWNER;
        if (dto.residentType() != null && !dto.residentType().isBlank()) {
            try {
                requestedType = ResidentType.valueOf(dto.residentType().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Membership invitation = new Membership();
        invitation.setUser(recipient);
        invitation.setCondominium(condominium);
        invitation.setRole(role);
        invitation.setStatus(MembershipStatus.PENDING);
        invitation.setRequestedUnit(requestedUnit);
        invitation.setRequestedResidentType("ROLE_RESIDENT".equals(authority) ? requestedType : null);
        invitation.setCreatedAt(LocalDateTime.now());
        invitation = membershipRepository.saveAndFlush(invitation);

        String roleLabel = "ROLE_DOORMAN".equals(authority) ? "porteiro" : "morador";
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(NotificationType.MEMBERSHIP_INVITATION);
        notification.setTitle("Convite para " + condominium.getName());
        notification.setMessage(
                "Você foi convidado para o condomínio "
                        + condominium.getName()
                        + " como "
                        + roleLabel
                        + ". Aceite o convite para liberar seu acesso."
        );
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setInvitationId(invitation.getId());
        notification.setCondominiumId(condominium.getId());
        notification.setCondominiumName(condominium.getName());
        notification.setAuthority(authority);
        notification.setInvitationStatus(MembershipStatus.PENDING.name());
        notification = notificationRepository.save(notification);

        return new MembershipInvitationDTO(
                invitation.getId(),
                condominium.getId(),
                condominium.getName(),
                authority,
                MembershipStatus.PENDING.name(),
                notification.getId()
        );
    }

    @Transactional
    public void removeDoorman(Long condominiumId, Long membershipId) {
        ensureCondominiumExists(condominiumId);
        ensureViewerIsManagerOrAdminOf(condominiumId);

        Membership membership = membershipRepository.findByIdWithDetails(membershipId)
                .filter(m -> m.getCondominium().getId().equals(condominiumId))
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo não encontrado neste condomínio."));

        String authority = membership.getRole().getAuthority();
        if ("ROLE_MANAGER".equals(authority)) {
            throw new ForbiddenException("Síndicos não podem remover vínculos de outro síndico por esta operação.");
        }

        User targetUser = membership.getUser();
        if ("ROLE_RESIDENT".equals(authority)) {
            residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(targetUser.getId(), condominiumId)
                    .ifPresent(residentRepository::delete);
        }
        updateInvitationNotifications(membership.getId(), MembershipStatus.DECLINED.name());
        membershipRepository.delete(membership);
        membershipRepository.flush();
        syncUserRoles(targetUser);
    }

    @Transactional
    public MembershipDTO acceptInvitation(Long invitationId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        Membership invitation = membershipRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado."));

        if (!invitation.getUser().getId().equals(viewer.getId())) {
            throw new ForbiddenException("Somente o destinatário pode aceitar este convite.");
        }

        if (invitation.getStatus() != MembershipStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este convite já foi respondido."
            );
        }

        invitation.setStatus(MembershipStatus.ACTIVE);
        invitation.setRespondedAt(LocalDateTime.now());
        invitation = membershipRepository.saveAndFlush(invitation);

        viewer.addRole(invitation.getRole());
        userRepository.save(viewer);

        if ("ROLE_RESIDENT".equals(invitation.getRole().getAuthority()) && invitation.getRequestedUnit() != null) {
            linkUserToUnit(viewer, invitation.getRequestedUnit(), invitation.getRequestedResidentType());
        }

        updateInvitationNotifications(invitation.getId(), MembershipStatus.ACTIVE.name());

        return enrichWithUnitInfo(invitation);
    }

    @Transactional
    public void declineInvitation(Long invitationId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        Membership invitation = membershipRepository.findByIdWithDetails(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado."));

        if (!invitation.getUser().getId().equals(viewer.getId())) {
            throw new ForbiddenException("Somente o destinatário pode recusar este convite.");
        }

        if (invitation.getStatus() != MembershipStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este convite já foi respondido."
            );
        }

        invitation.setStatus(MembershipStatus.DECLINED);
        invitation.setRespondedAt(LocalDateTime.now());
        membershipRepository.saveAndFlush(invitation);

        updateInvitationNotifications(invitation.getId(), MembershipStatus.DECLINED.name());
    }

    @Transactional(readOnly = true)
    public List<MembershipDTO> findManagersByCondominiumForAdmin(Long condominiumId) {
        ensureViewerIsSystemAdmin();
        ensureCondominiumExists(condominiumId);
        return membershipRepository.findActiveManagersByCondominiumId(condominiumId).stream()
                .map(this::enrichWithUnitInfo)
                .toList();
    }

    @Transactional
    public MembershipDTO addManagerForAdmin(Long condominiumId, AddManagerRequestDTO dto) {
        ensureViewerIsSystemAdmin();
        Condominium condominium = getCondominiumOrThrow(condominiumId);

        String normalizedEmail = dto.email().trim().toLowerCase();
        User targetUser = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhuma conta encontrada para o email " + normalizedEmail
                ));

        List<Membership> existing = membershipRepository.findByUserIdAndCondominiumIdAndAuthority(
                targetUser.getId(),
                condominiumId,
                "ROLE_MANAGER"
        );
        for (Membership item : existing) {
            if (item.getStatus() == MembershipStatus.ACTIVE) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Este usuário já é síndico deste condomínio."
                );
            }
        }

        Role managerRole = getOrCreateRole("ROLE_MANAGER");

        Membership membership = new Membership();
        membership.setUser(targetUser);
        membership.setCondominium(condominium);
        membership.setRole(managerRole);
        membership.setStatus(MembershipStatus.ACTIVE);
        membership.setCreatedAt(LocalDateTime.now());
        membership.setRespondedAt(LocalDateTime.now());
        membership = membershipRepository.saveAndFlush(membership);

        targetUser.addRole(managerRole);
        userRepository.save(targetUser);

        Notification notification = new Notification();
        notification.setRecipient(targetUser);
        notification.setType(NotificationType.SYSTEM_ANNOUNCEMENT);
        notification.setTitle("Atribuição de síndico");
        notification.setMessage("Você foi adicionado como síndico do condomínio " + condominium.getName() + ".");
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setCondominiumId(condominium.getId());
        notification.setCondominiumName(condominium.getName());
        notification.setAuthority("ROLE_MANAGER");
        notificationRepository.save(notification);

        return enrichWithUnitInfo(membership);
    }

    @Transactional
    public void removeManagerForAdmin(Long condominiumId, Long membershipId) {
        ensureViewerIsSystemAdmin();
        ensureCondominiumExists(condominiumId);

        Membership membership = membershipRepository.findByIdWithDetails(membershipId)
                .filter(m -> m.getCondominium().getId().equals(condominiumId))
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de síndico não encontrado neste condomínio."));

        if (!"ROLE_MANAGER".equals(membership.getRole().getAuthority())) {
            throw new ForbiddenException("Esta operação só remove vínculos de síndico.");
        }

        User targetUser = membership.getUser();
        membershipRepository.delete(membership);
        membershipRepository.flush();
        syncUserRoles(targetUser);
    }

    private void updateInvitationNotifications(Long invitationId, String status) {
        List<Notification> notifications = notificationRepository.findByInvitationId(invitationId);
        if (notifications.isEmpty()) {
            return;
        }
        for (Notification notification : notifications) {
            notification.setRead(true);
            notification.setInvitationStatus(status);
        }
        notificationRepository.saveAll(notifications);
    }

    private void syncUserRoles(User user) {
        User managedUser = userRepository.findByEmailIgnoreCase(user.getEmail()).orElse(user);
        for (String roleName : List.of("ROLE_MANAGER", "ROLE_DOORMAN")) {
            Role role = roleRepository.findByAuthority(roleName);
            if (role == null) {
                continue;
            }
            boolean hasActive = membershipRepository.existsActiveByUserIdAndAuthority(managedUser.getId(), roleName);
            if (hasActive) {
                managedUser.addRole(role);
            } else {
                managedUser.removeRole(role);
            }
        }
        if (managedUser.getRoles().isEmpty()) {
            managedUser.addRole(getOrCreateRole("ROLE_RESIDENT"));
        }
        userRepository.save(managedUser);
    }

    private void ensureViewerIsManagerOrAdminOf(Long condominiumId) {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        if (viewer.hasRole("ROLE_ADMIN")) {
            return;
        }
        if (!membershipRepository.isManagerOf(viewer.getId(), condominiumId)) {
            throw new ForbiddenException("Apenas o síndico deste condomínio pode gerenciar seus vínculos.");
        }
    }

    private void ensureViewerIsSystemAdmin() {
        User viewer = authenticatedUserService.getAuthenticatedUser();
        if (!viewer.hasRole("ROLE_ADMIN")) {
            throw new ForbiddenException("Apenas administradores do sistema podem gerenciar síndicos.");
        }
    }

    private Condominium getCondominiumOrThrow(Long condominiumId) {
        return condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + condominiumId));
    }

    private void ensureCondominiumExists(Long condominiumId) {
        if (!condominiumRepository.existsById(condominiumId)) {
            throw new ResourceNotFoundException("Condominium not found: " + condominiumId);
        }
    }

    private Role getOrCreateRole(String authority) {
        Role role = roleRepository.findByAuthority(authority);
        if (role != null) {
            return role;
        }
        Role created = new Role();
        created.setAuthority(authority);
        return roleRepository.save(created);
    }

    private void linkAssociations(
            Long condominiumId,
            Membership entity,
            String authority,
            Long userId
    ) {
        Condominium condominium = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condominium not found: " + condominiumId));

        entity.setCondominium(condominium);

        Role role = getOrCreateRole(authority);
        entity.setRole(role);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        entity.setUser(user);
    }

    public record AvailableCondominiumDTO(Long id, String name, String address) {}
    public record AvailableUnitDTO(
            Long id,
            String identifier,
            Long sectorId,
            String sectorName,
            int currentResidentsCount
    ) {}
    public record MembershipRequestCreateDTO(
            String authority,
            Long unitId,
            String residentType,
            String message
    ) {}
}
