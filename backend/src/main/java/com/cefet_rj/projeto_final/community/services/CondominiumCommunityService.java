package com.cefet_rj.projeto_final.community.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.community.entities.*;
import com.cefet_rj.projeto_final.community.repositories.*;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.reservations.entities.Reservation;
import com.cefet_rj.projeto_final.reservations.repositories.ReservationRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CondominiumCommunityService {

    private final OccurrenceTicketRepository ticketRepository;
    private final VisitorAccessRepository visitorRepository;
    private final PollRepository pollRepository;
    private final PollOptionRepository pollOptionRepository;
    private final PollVoteRepository pollVoteRepository;
    private final LostFoundItemRepository lostFoundRepository;
    private final LostFoundMessageRepository lostFoundMessageRepository;
    private final ClassifiedAdRepository classifiedRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final PetRepository petRepository;
    private final MovingRequestRepository movingRequestRepository;
    private final CondominiumDocumentRepository documentRepository;
    private final SmartLockerCompartmentRepository lockerRepository;
    private final EventGuestListRepository guestListRepository;
    private final GuestEntryRepository guestEntryRepository;
    private final CondominiumInfractionRepository infractionRepository;
    private final ReservationRepository reservationRepository;

    private final CondominiumRepository condominiumRepository;
    private final UnitRepository unitRepository;
    private final ResidentRepository residentRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final NotificationService notificationService;

    // =========================================================================
    // 1. OCCURRENCE TICKETS (Livro de Ocorrências e Chamados de Manutenção)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<OccurrenceTicketDTO> listTickets(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        List<OccurrenceTicket> list = onlyMine
                ? ticketRepository.findByCondominiumIdAndAuthorIdOrderByCreatedAtDesc(condominiumId, user.getId())
                : ticketRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        return list.stream().map(OccurrenceTicketDTO::new).toList();
    }

    @Transactional
    public OccurrenceTicketDTO createTicket(Long condominiumId, Map<String, Object> body) {
        User user = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);
        Unit unit = resolveUserUnit(user.getId(), condominiumId, body.get("unitId"));

        String title = stringVal(body, "title");
        String description = stringVal(body, "description");
        if (title.isBlank() || description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe título e descrição da ocorrência.");
        }

        OccurrenceTicket ticket = new OccurrenceTicket();
        ticket.setCondominium(condo);
        ticket.setAuthor(user);
        ticket.setUnit(unit);
        ticket.setCategory(defaultIfBlank(stringVal(body, "category"), "MAINTENANCE").toUpperCase(Locale.ROOT));
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setLocation(blankToNull(stringVal(body, "location")));
        ticket.setStatus("OPEN");
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        notifyCondominiumManagers(
                condominiumId,
                condo.getName(),
                "Novo chamado: " + ticket.getTitle(),
                user.getFirstName() + " registrou um chamado (" + ticket.getCategory() + "): " + ticket.getTitle()
        );

        return new OccurrenceTicketDTO(ticket);
    }

    @Transactional
    public OccurrenceTicketDTO updateTicketStatus(Long id, Map<String, Object> body) {
        OccurrenceTicket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ocorrência não encontrada"));
        Long condoId = ticket.getCondominium().getId();
        validateStaffOrOwner(condoId, ticket.getAuthor().getId());

        String status = defaultIfBlank(stringVal(body, "status"), "IN_PROGRESS").toUpperCase(Locale.ROOT);
        String response = blankToNull(stringVal(body, "adminResponse"));

        ticket.setStatus(status);
        if (response != null) {
            ticket.setAdminResponse(response);
        }
        ticket.setUpdatedAt(LocalDateTime.now());
        if ("RESOLVED".equals(status) || "CLOSED".equals(status)) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        ticket = ticketRepository.save(ticket);

        notificationService.createDirectNotification(
                ticket.getAuthor(),
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "Atualização no chamado: " + ticket.getTitle(),
                "Status atualizado para " + status + (response != null ? " — Resposta: " + response : "."),
                condoId,
                ticket.getCondominium().getName()
        );

        return new OccurrenceTicketDTO(ticket);
    }

    // =========================================================================
    // 2. VISITOR ACCESS & PRE-AUTHORIZATION (Controle Avançado de Visitantes)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<VisitorAccessDTO> listVisitors(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        boolean isStaff = condominiumAccessService.isStaffOrAdmin(user, condominiumId);
        if (onlyMine || !isStaff) {
            List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(user.getId());
            return visitorRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId).stream()
                    .filter(v -> v.getResident().getId().equals(user.getId())
                            || (v.getUnit() != null && myUnitIds.contains(v.getUnit().getId())))
                    .map(VisitorAccessDTO::new)
                    .toList();
        }
        return visitorRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .map(VisitorAccessDTO::new)
                .toList();
    }

    @Transactional
    public VisitorAccessDTO createVisitorAccess(Long condominiumId, Map<String, Object> body) {
        User caller = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        String visitorName = stringVal(body, "visitorName");
        if (visitorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do visitante ou prestador.");
        }

        boolean isStaff = condominiumAccessService.isStaffOrAdmin(caller, condominiumId);
        boolean requestResidentApproval = Boolean.TRUE.equals(body.get("requestApproval"));
        Long targetResidentId = numberVal(body.get("residentId"));
        Long targetUnitId = numberVal(body.get("unitId"));

        Unit unit = resolveUserUnit(
                (isStaff && targetResidentId != null) ? targetResidentId : caller.getId(),
                condominiumId,
                targetUnitId
        );

        User resident = caller;
        if (isStaff && targetResidentId != null) {
            resident = userRepository.findById(targetResidentId).orElse(caller);
        } else if (isStaff && unit != null) {
            var resOpt = residentRepository.findFirstByUnitId(unit.getId());
            if (resOpt.isPresent()) {
                resident = resOpt.get().getUser();
            }
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expectedAt = parseDateTime(stringVal(body, "expectedAt"), now);
        LocalDateTime validFrom = parseDateTime(stringVal(body, "validFrom"), expectedAt.minusMinutes(15));
        Long validityHours = numberVal(body.get("validityHours"));
        LocalDateTime defaultValidUntil = validityHours != null && validityHours > 0
                ? validFrom.plusHours(validityHours)
                : validFrom.plusHours(24);
        LocalDateTime validUntil = parseDateTime(stringVal(body, "validUntil"), defaultValidUntil);
        if (validUntil.isBefore(validFrom)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data final de validade deve ser posterior ao início.");
        }

        String accessCode = "AC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        String qrPayload = "CONDO:" + condominiumId + "|CODE:" + accessCode + "|TOKEN:"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);

        VisitorAccess access = new VisitorAccess();
        access.setCondominium(condo);
        access.setUnit(unit);
        access.setResident(resident);
        access.setVisitorName(visitorName);
        access.setDocumentNumber(blankToNull(stringVal(body, "documentNumber")));
        access.setPhone(blankToNull(stringVal(body, "phone")));
        access.setVisitorType(defaultIfBlank(stringVal(body, "visitorType"), "VISITOR").toUpperCase(Locale.ROOT));
        access.setVehiclePlate(blankToNull(stringVal(body, "vehiclePlate").toUpperCase(Locale.ROOT)));
        access.setAccessCode(accessCode);
        access.setQrCodePayload(qrPayload);
        access.setNotes(blankToNull(stringVal(body, "notes")));
        access.setExpectedAt(expectedAt);
        access.setValidFrom(validFrom);
        access.setValidUntil(validUntil);
        access.setStatus(requestResidentApproval ? "REQUESTED" : "AUTHORIZED");
        access.setCreatedAt(now);
        access = visitorRepository.save(access);

        if (requestResidentApproval) {
            notificationService.createDirectNotification(
                    resident,
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Portaria: solicitação de entrada",
                    visitorName + " (" + access.getVisitorType() + ") aguarda autorização na portaria. Aprove ou recuse no aplicativo.",
                    condominiumId,
                    condo.getName()
            );
        }

        return new VisitorAccessDTO(access);
    }

    @Transactional
    public VisitorAccessDTO updateVisitorStatus(Long id, String nextStatus) {
        VisitorAccess access = visitorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de visitante não encontrado"));
        Long condoId = access.getCondominium().getId();
        User caller = validateMemberAccess(condoId);

        String normalized = nextStatus.toUpperCase(Locale.ROOT);
        if ("CHECKED_IN".equals(normalized) || "CHECKED_OUT".equals(normalized)) {
            validateStaffAccess(condoId);
            if ("CHECKED_IN".equals(normalized)) {
                if ("DENIED".equals(access.getStatus()) || "EXPIRED".equals(access.getStatus())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este convite foi negado ou expirou.");
                }
                if (access.getValidUntil() != null && LocalDateTime.now().isAfter(access.getValidUntil())) {
                    access.setStatus("EXPIRED");
                    visitorRepository.save(access);
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este convite de visitante está expirado.");
                }
                access.setCheckedInAt(LocalDateTime.now());
                access.setCheckedInBy(caller);
                notificationService.createDirectNotification(
                        access.getResident(),
                        NotificationType.SYSTEM_ANNOUNCEMENT,
                        "Entrada registrada na portaria",
                        access.getVisitorName() + " acabou de entrar no condomínio.",
                        condoId,
                        access.getCondominium().getName()
                );
            } else {
                access.setCheckedOutAt(LocalDateTime.now());
                access.setCheckedOutBy(caller);
            }
        } else {
            boolean isStaff = condominiumAccessService.isStaffOrAdmin(caller, condoId);
            boolean isOwner = access.getResident().getId().equals(caller.getId());
            boolean isUnitResident = access.getUnit() != null
                    && residentRepository.findUnitIdsByUserId(caller.getId()).contains(access.getUnit().getId());
            if (!isStaff && !isOwner && !isUnitResident) {
                throw new ForbiddenException("Apenas o morador responsável ou a portaria pode autorizar/recusar este visitante.");
            }
        }

        access.setStatus(normalized);
        access = visitorRepository.save(access);
        return new VisitorAccessDTO(access);
    }

    @Transactional
    public VisitorAccessDTO validateVisitorQrCode(Long condominiumId, Map<String, Object> body) {
        User staff = validateStaffAccess(condominiumId);
        String codeOrPayload = stringVal(body, "qrCode");
        if (codeOrPayload.isBlank()) {
            codeOrPayload = stringVal(body, "qrCodeOrAccessCode");
        }
        if (codeOrPayload.isBlank()) {
            codeOrPayload = stringVal(body, "qrCodePayload");
        }
        if (codeOrPayload.isBlank()) {
            codeOrPayload = stringVal(body, "accessCode");
        }
        if (codeOrPayload.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o código de acesso ou QR Code do convite.");
        }

        String finalCode = codeOrPayload.trim();
        Optional<VisitorAccess> found = visitorRepository.findByCondominiumIdAndQrCodePayload(condominiumId, finalCode);
        if (found.isEmpty()) {
            found = visitorRepository.findByCondominiumIdAndAccessCodeIgnoreCase(condominiumId, finalCode);
        }
        if (found.isEmpty() && finalCode.contains("CODE:")) {
            for (String part : finalCode.split("\\|")) {
                if (part.startsWith("CODE:")) {
                    String extracted = part.substring("CODE:".length()).trim();
                    found = visitorRepository.findByCondominiumIdAndAccessCodeIgnoreCase(condominiumId, extracted);
                    break;
                }
            }
        }

        VisitorAccess access = found.orElseThrow(() ->
                new ResourceNotFoundException("Nenhum convite válido encontrado para o código informado."));

        LocalDateTime now = LocalDateTime.now();
        if (access.getValidUntil() != null && now.isAfter(access.getValidUntil())) {
            access.setStatus("EXPIRED");
            visitorRepository.save(access);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Convite expirado em " + access.getValidUntil());
        }
        if (access.getValidFrom() != null && now.isBefore(access.getValidFrom().minusMinutes(30))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Convite válido somente a partir de " + access.getValidFrom());
        }
        if ("DENIED".equals(access.getStatus()) || "CHECKED_OUT".equals(access.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Convite não está autorizado (status atual: " + access.getStatus() + ").");
        }

        boolean autoCheckIn = body == null || body.get("autoCheckIn") == null || Boolean.TRUE.equals(body.get("autoCheckIn"));
        if (autoCheckIn && !"CHECKED_IN".equals(access.getStatus())) {
            access.setStatus("CHECKED_IN");
            access.setCheckedInAt(now);
            access.setCheckedInBy(staff);
            access = visitorRepository.save(access);

            notificationService.createDirectNotification(
                    access.getResident(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "QR Code validado na portaria",
                    access.getVisitorName() + " teve entrada liberada via QR Code.",
                    condominiumId,
                    access.getCondominium().getName()
            );
        }

        return new VisitorAccessDTO(access);
    }

    // =========================================================================
    // 3. POLLS & VIRTUAL ASSEMBLIES (Enquetes Rápidas)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PollDTO> listPolls(Long condominiumId) {
        User user = validateMemberAccess(condominiumId);
        List<Poll> polls = pollRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        return polls.stream().map(poll -> toPollDTO(poll, user.getId())).toList();
    }

    @Transactional
    public PollDTO createPoll(Long condominiumId, Map<String, Object> body) {
        User user = validateManagerAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        String title = stringVal(body, "title");
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o título da enquete/pauta.");
        }

        List<String> optionLabels = new ArrayList<>();
        Object rawOptions = body.get("options");
        if (rawOptions instanceof Collection<?> col) {
            for (Object item : col) {
                if (item != null && !item.toString().trim().isEmpty()) {
                    optionLabels.add(item.toString().trim());
                }
            }
        }
        if (optionLabels.size() < 2) {
            optionLabels = List.of("Aprovar / Sim", "Reprovar / Não", "Abstenção");
        }

        Poll poll = new Poll();
        poll.setCondominium(condo);
        poll.setCreatedBy(user);
        poll.setTitle(title);
        poll.setDescription(blankToNull(stringVal(body, "description")));
        poll.setEndsAt(parseDateTime(stringVal(body, "endsAt"), LocalDateTime.now().plusDays(7)));
        poll.setActive(true);
        poll.setCreatedAt(LocalDateTime.now());

        for (String label : optionLabels) {
            PollOption opt = new PollOption();
            opt.setPoll(poll);
            opt.setLabel(label);
            poll.getOptions().add(opt);
        }

        poll = pollRepository.save(poll);

        List<Long> residentIds = membershipRepository.findResidentUserIdsByCondominiumId(condominiumId);
        notificationService.createAnnouncementNotifications(
                residentIds,
                "Nova votação: " + poll.getTitle(),
                condominiumId,
                condo.getName()
        );

        return toPollDTO(poll, user.getId());
    }

    @Transactional
    public PollDTO voteOnPoll(Long pollId, Long optionId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquete não encontrada"));
        User user = validateMemberAccess(poll.getCondominium().getId());

        if (!Boolean.TRUE.equals(poll.getActive())
                || (poll.getEndsAt() != null && LocalDateTime.now().isAfter(poll.getEndsAt()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta votação já foi encerrada.");
        }

        PollOption option = pollOptionRepository.findById(optionId)
                .filter(o -> o.getPoll().getId().equals(pollId))
                .orElseThrow(() -> new ResourceNotFoundException("Opção de voto inválida"));

        PollVote vote = pollVoteRepository.findByPollIdAndVoterId(pollId, user.getId())
                .orElseGet(PollVote::new);
        vote.setPoll(poll);
        vote.setOption(option);
        vote.setVoter(user);
        vote.setVotedAt(LocalDateTime.now());
        pollVoteRepository.save(vote);

        return toPollDTO(poll, user.getId());
    }

    @Transactional
    public PollDTO closePoll(Long pollId) {
        Poll poll = pollRepository.findById(pollId)
                .orElseThrow(() -> new ResourceNotFoundException("Enquete não encontrada"));
        User user = validateManagerAccess(poll.getCondominium().getId());
        poll.setActive(false);
        poll = pollRepository.save(poll);
        return toPollDTO(poll, user.getId());
    }

    private PollDTO toPollDTO(Poll poll, Long viewerUserId) {
        List<PollVote> votes = pollVoteRepository.findByPollId(poll.getId());
        Map<Long, Long> counts = new HashMap<>();
        Long myOptionId = null;
        for (PollVote v : votes) {
            counts.merge(v.getOption().getId(), 1L, Long::sum);
            if (v.getVoter().getId().equals(viewerUserId)) {
                myOptionId = v.getOption().getId();
            }
        }
        int total = votes.size();
        List<PollOptionDTO> optDtos = poll.getOptions().stream()
                .map(o -> new PollOptionDTO(
                        o.getId(),
                        o.getLabel(),
                        counts.getOrDefault(o.getId(), 0L).intValue()
                ))
                .toList();

        boolean effectiveActive = Boolean.TRUE.equals(poll.getActive())
                && (poll.getEndsAt() == null || LocalDateTime.now().isBefore(poll.getEndsAt()));

        return new PollDTO(
                poll.getId(),
                poll.getCondominium().getId(),
                poll.getTitle(),
                poll.getDescription(),
                effectiveActive,
                poll.getEndsAt(),
                poll.getCreatedAt(),
                total,
                myOptionId,
                optDtos,
                formatUser(poll.getCreatedBy())
        );
    }

    // =========================================================================
    // 4. LOST AND FOUND (Achados e Perdidos com Categorias, Fotos e Mensagens)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<LostFoundItemDTO> listLostFound(Long condominiumId) {
        validateMemberAccess(condominiumId);
        return lostFoundRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .map(item -> new LostFoundItemDTO(item, (int) lostFoundMessageRepository.countByItemId(item.getId())))
                .toList();
    }

    @Transactional
    public LostFoundItemDTO createLostFound(Long condominiumId, Map<String, Object> body) {
        User user = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        String title = stringVal(body, "title");
        String description = stringVal(body, "description");
        if (title.isBlank() || description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o item e a descrição.");
        }

        LostFoundItem item = new LostFoundItem();
        item.setCondominium(condo);
        item.setReportedBy(user);
        item.setType(defaultIfBlank(stringVal(body, "type"), "FOUND").toUpperCase(Locale.ROOT));
        item.setCategory(defaultIfBlank(stringVal(body, "category"), "GENERAL").toUpperCase(Locale.ROOT));
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(blankToNull(stringVal(body, "location")));
        item.setImageUrl(blankToNull(stringVal(body, "imageUrl")));
        item.setContactInfo(blankToNull(stringVal(body, "contactInfo")));
        item.setStatus("OPEN");
        item.setCreatedAt(LocalDateTime.now());
        item = lostFoundRepository.save(item);

        return new LostFoundItemDTO(item, 0);
    }

    @Transactional
    public LostFoundItemDTO requestClaimLostFound(Long id) {
        return requestClaimLostFound(id, null);
    }

    @Transactional
    public LostFoundItemDTO requestClaimLostFound(Long id, Map<String, Object> body) {
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado"));
        User user = validateMemberAccess(item.getCondominium().getId());

        String claimMsg = blankToNull(stringVal(body, "message"));
        item.setClaimedBy(user);
        if (claimMsg != null) {
            item.setClaimMessage(claimMsg);
            LostFoundMessage msg = new LostFoundMessage();
            msg.setItem(item);
            msg.setSender(user);
            msg.setMessage(claimMsg);
            msg.setCreatedAt(LocalDateTime.now());
            lostFoundMessageRepository.save(msg);
        }
        item.setStatus("CLAIM_REQUESTED");
        item = lostFoundRepository.save(item);

        notificationService.createDirectNotification(
                item.getReportedBy(),
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "Achados e Perdidos: solicitação de retirada",
                user.getFirstName() + " " + user.getLastName() + " informou ser proprietário(a) de: " + item.getTitle(),
                item.getCondominium().getId(),
                item.getCondominium().getName()
        );

        return new LostFoundItemDTO(item, (int) lostFoundMessageRepository.countByItemId(item.getId()));
    }

    @Transactional
    public LostFoundItemDTO markLostFoundReturned(Long id) {
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado"));
        validateStaffOrOwner(item.getCondominium().getId(), item.getReportedBy().getId());

        item.setStatus("RETURNED");
        item.setReturnedAt(LocalDateTime.now());
        item = lostFoundRepository.save(item);

        return new LostFoundItemDTO(item, (int) lostFoundMessageRepository.countByItemId(item.getId()));
    }

    @Transactional
    public LostFoundItemDTO closeLostFound(Long id, Map<String, Object> body) {
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado"));
        validateStaffOrOwner(item.getCondominium().getId(), item.getReportedBy().getId());

        String status = defaultIfBlank(stringVal(body, "status"), "CLOSED").toUpperCase(Locale.ROOT);
        String notes = blankToNull(stringVal(body, "resolutionNotes"));
        item.setStatus(status);
        if (notes != null) {
            item.setResolutionNotes(notes);
        }
        if ("RETURNED".equals(status) || "CLOSED".equals(status) || "ARCHIVED".equals(status)) {
            item.setReturnedAt(LocalDateTime.now());
        }
        item = lostFoundRepository.save(item);
        return new LostFoundItemDTO(item, (int) lostFoundMessageRepository.countByItemId(item.getId()));
    }

    @Transactional(readOnly = true)
    public List<LostFoundMessageDTO> listLostFoundMessages(Long itemId) {
        LostFoundItem item = lostFoundRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado"));
        validateMemberAccess(item.getCondominium().getId());
        return lostFoundMessageRepository.findByItemIdOrderByCreatedAtAsc(itemId).stream()
                .map(LostFoundMessageDTO::new)
                .toList();
    }

    @Transactional
    public LostFoundMessageDTO addLostFoundMessage(Long itemId, Map<String, Object> body) {
        LostFoundItem item = lostFoundRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado"));
        User sender = validateMemberAccess(item.getCondominium().getId());
        String text = stringVal(body, "message");
        if (text.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a mensagem.");
        }

        LostFoundMessage msg = new LostFoundMessage();
        msg.setItem(item);
        msg.setSender(sender);
        msg.setMessage(text);
        msg.setCreatedAt(LocalDateTime.now());
        msg = lostFoundMessageRepository.save(msg);

        if (!item.getReportedBy().getId().equals(sender.getId())) {
            notificationService.createDirectNotification(
                    item.getReportedBy(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Nova mensagem em Achados e Perdidos: " + item.getTitle(),
                    sender.getFirstName() + ": \"" + text + "\"",
                    item.getCondominium().getId(),
                    item.getCondominium().getName()
            );
        }
        if (item.getClaimedBy() != null && !item.getClaimedBy().getId().equals(sender.getId())) {
            notificationService.createDirectNotification(
                    item.getClaimedBy(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Nova mensagem em Achados e Perdidos: " + item.getTitle(),
                    sender.getFirstName() + ": \"" + text + "\"",
                    item.getCondominium().getId(),
                    item.getCondominium().getName()
            );
        }

        return new LostFoundMessageDTO(msg);
    }

    // =========================================================================
    // 5. CLASSIFIEDS & NEIGHBOR SERVICES (Classificados e Mural de Serviços)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ClassifiedAdDTO> listClassifieds(Long condominiumId) {
        validateMemberAccess(condominiumId);
        return classifiedRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .map(ClassifiedAdDTO::new)
                .toList();
    }

    @Transactional
    public ClassifiedAdDTO createClassified(Long condominiumId, Map<String, Object> body) {
        User user = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);
        Unit unit = resolveUserUnit(user.getId(), condominiumId, body.get("unitId"));

        String title = stringVal(body, "title");
        String description = stringVal(body, "description");
        if (title.isBlank() || description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe título e descrição do anúncio.");
        }

        BigDecimal price = null;
        Object rawPrice = body.get("price");
        if (rawPrice != null && !rawPrice.toString().isBlank()) {
            try {
                price = new BigDecimal(rawPrice.toString().replace(",", ".").trim());
            } catch (NumberFormatException ignored) {
            }
        }

        ClassifiedAd ad = new ClassifiedAd();
        ad.setCondominium(condo);
        ad.setAuthor(user);
        ad.setUnit(unit);
        ad.setCategory(defaultIfBlank(stringVal(body, "category"), "SALE").toUpperCase(Locale.ROOT));
        ad.setTitle(title);
        ad.setDescription(description);
        ad.setPrice(price);
        ad.setContactPhone(blankToNull(stringVal(body, "contactPhone")));
        ad.setStatus("ACTIVE");
        ad.setInterestCount(0);
        ad.setCreatedAt(LocalDateTime.now());
        ad = classifiedRepository.save(ad);

        return new ClassifiedAdDTO(ad);
    }

    @Transactional
    public ClassifiedAdDTO sendClassifiedInterest(Long id, Map<String, Object> body) {
        ClassifiedAd ad = classifiedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado"));
        User user = validateMemberAccess(ad.getCondominium().getId());

        String message = defaultIfBlank(
                stringVal(body, "message"),
                "Tenho interesse no seu anúncio!"
        );
        ad.setInterestCount((ad.getInterestCount() == null ? 0 : ad.getInterestCount()) + 1);
        ad = classifiedRepository.save(ad);

        notificationService.createDirectNotification(
                ad.getAuthor(),
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "Interesse no classificado: " + ad.getTitle(),
                user.getFirstName() + " " + user.getLastName() + " (" + user.getEmail() + ") enviou: \"" + message + "\"",
                ad.getCondominium().getId(),
                ad.getCondominium().getName()
        );

        return new ClassifiedAdDTO(ad);
    }

    @Transactional
    public ClassifiedAdDTO updateClassifiedStatus(Long id, String status) {
        ClassifiedAd ad = classifiedRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anúncio não encontrado"));
        validateStaffOrOwner(ad.getCondominium().getId(), ad.getAuthor().getId());

        ad.setStatus(defaultIfBlank(status, "CLOSED").toUpperCase(Locale.ROOT));
        ad = classifiedRepository.save(ad);
        return new ClassifiedAdDTO(ad);
    }

    // =========================================================================
    // 6. VEHICLES, PARKING SPOTS & PLATE LOOKUP (Gestão de Veículos e Vagas)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<ParkingSpotDTO> listParkingSpots(Long condominiumId) {
        validateMemberAccess(condominiumId);
        return parkingSpotRepository.findByCondominiumIdOrderByCodeAsc(condominiumId)
                .stream()
                .map(ParkingSpotDTO::new)
                .toList();
    }

    @Transactional
    public ParkingSpotDTO createParkingSpot(Long condominiumId, Map<String, Object> body) {
        validateManagerAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);
        String code = stringVal(body, "code").toUpperCase(Locale.ROOT);
        if (code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o código/identificador da vaga.");
        }
        if (parkingSpotRepository.existsByCondominiumIdAndCodeIgnoreCase(condominiumId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma vaga com o código " + code + " neste condomínio.");
        }

        Unit unit = null;
        Long unitId = numberVal(body.get("unitId"));
        if (unitId != null) {
            unit = unitRepository.findById(unitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitId));
            if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade não pertence a este condomínio.");
            }
        }

        ParkingSpot spot = new ParkingSpot();
        spot.setCondominium(condo);
        spot.setUnit(unit);
        spot.setCode(code);
        spot.setSpotType(defaultIfBlank(stringVal(body, "spotType"), "CAR").toUpperCase(Locale.ROOT));
        spot.setLocationDescription(blankToNull(stringVal(body, "locationDescription")));
        spot.setStatus(defaultIfBlank(stringVal(body, "status"), unit != null ? "OCCUPIED" : "AVAILABLE").toUpperCase(Locale.ROOT));
        spot.setCreatedAt(LocalDateTime.now());
        spot = parkingSpotRepository.save(spot);
        return new ParkingSpotDTO(spot);
    }

    @Transactional
    public ParkingSpotDTO updateParkingSpot(Long id, Map<String, Object> body) {
        ParkingSpot spot = parkingSpotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada"));
        Long condoId = spot.getCondominium().getId();
        validateManagerAccess(condoId);

        String code = stringVal(body, "code").toUpperCase(Locale.ROOT);
        if (!code.isBlank() && !code.equalsIgnoreCase(spot.getCode())) {
            if (parkingSpotRepository.existsByCondominiumIdAndCodeIgnoreCase(condoId, code)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma vaga com o código " + code);
            }
            spot.setCode(code);
        }
        if (body.containsKey("spotType") && !stringVal(body, "spotType").isBlank()) {
            spot.setSpotType(stringVal(body, "spotType").toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("locationDescription")) {
            spot.setLocationDescription(blankToNull(stringVal(body, "locationDescription")));
        }
        if (body.containsKey("unitId")) {
            Long unitId = numberVal(body.get("unitId"));
            if (unitId == null) {
                spot.setUnit(null);
            } else {
                Unit unit = unitRepository.findById(unitId)
                        .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + unitId));
                if (!unit.getSector().getCondominium().getId().equals(condoId)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade não pertence a este condomínio.");
                }
                spot.setUnit(unit);
            }
        }
        if (body.containsKey("status") && !stringVal(body, "status").isBlank()) {
            spot.setStatus(stringVal(body, "status").toUpperCase(Locale.ROOT));
        }
        spot = parkingSpotRepository.save(spot);
        return new ParkingSpotDTO(spot);
    }

    @Transactional
    public void deleteParkingSpot(Long id) {
        ParkingSpot spot = parkingSpotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaga não encontrada"));
        validateManagerAccess(spot.getCondominium().getId());
        for (Vehicle v : vehicleRepository.findByParkingSpotEntityId(id)) {
            v.setParkingSpotEntity(null);
            vehicleRepository.save(v);
        }
        parkingSpotRepository.delete(spot);
    }

    @Transactional(readOnly = true)
    public List<VehicleDTO> listVehicles(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        List<Vehicle> list = onlyMine
                ? vehicleRepository.findByCondominiumIdAndOwnerIdOrderByCreatedAtDesc(condominiumId, user.getId())
                : vehicleRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        return list.stream().map(VehicleDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public PlateCheckResultDTO checkVehiclePlate(Long condominiumId, String plateQuery) {
        validateMemberAccess(condominiumId);
        String normalized = plateQuery == null ? "" : plateQuery.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return new PlateCheckResultDTO(plateQuery, false, "Informe uma placa para consultar.", List.of(), List.of());
        }

        List<VehicleDTO> matchedVehicles = vehicleRepository
                .findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .filter(v -> v.getLicensePlate() != null
                        && v.getLicensePlate().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT).contains(normalized))
                .map(VehicleDTO::new)
                .toList();

        List<VisitorAccessDTO> matchedVisitors = visitorRepository
                .findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .filter(v -> v.getVehiclePlate() != null
                        && v.getVehiclePlate().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT).contains(normalized))
                .map(VisitorAccessDTO::new)
                .toList();

        boolean found = !matchedVehicles.isEmpty() || !matchedVisitors.isEmpty();
        String message = found
                ? "Foram encontrados " + matchedVehicles.size() + " veículo(s) de moradores e "
                    + matchedVisitors.size() + " visitante(s) para a placa consultada."
                : "Nenhum veículo de morador ou visitante autorizado encontrado com esta placa.";

        return new PlateCheckResultDTO(normalized, found, message, matchedVehicles, matchedVisitors);
    }

    @Transactional
    public VehicleDTO createVehicle(Long condominiumId, Map<String, Object> body) {
        User user = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);
        Unit unit = resolveUserUnit(user.getId(), condominiumId, body.get("unitId"));

        String brandModel = stringVal(body, "brandModel");
        String licensePlate = stringVal(body, "licensePlate").toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        if (brandModel.isBlank() || licensePlate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o modelo e a placa do veículo.");
        }
        if (vehicleRepository.existsByCondominiumIdAndLicensePlateIgnoreCase(condominiumId, licensePlate)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um veículo cadastrado com a placa " + licensePlate + " neste condomínio.");
        }

        boolean isManager = condominiumAccessService.isManagerOrAdmin(user, condominiumId);

        ParkingSpot spotEntity = null;
        Long parkingSpotId = numberVal(body.get("parkingSpotId"));
        if (parkingSpotId != null) {
            spotEntity = parkingSpotRepository.findById(parkingSpotId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vaga de garagem não encontrada: " + parkingSpotId));
            if (!spotEntity.getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A vaga não pertence a este condomínio.");
            }
            if (spotEntity.getUnit() != null && unit != null && !spotEntity.getUnit().getId().equals(unit.getId()) && !isManager) {
                throw new ForbiddenException("Esta vaga pertence a outra unidade.");
            }
            spotEntity.setStatus("OCCUPIED");
            if (spotEntity.getUnit() == null && unit != null) {
                spotEntity.setUnit(unit);
            }
            parkingSpotRepository.save(spotEntity);
        }

        Vehicle v = new Vehicle();
        v.setCondominium(condo);
        v.setUnit(unit);
        v.setOwner(user);
        v.setParkingSpotEntity(spotEntity);
        v.setType(defaultIfBlank(stringVal(body, "type"), "CAR").toUpperCase(Locale.ROOT));
        v.setBrandModel(brandModel);
        v.setColor(defaultIfBlank(stringVal(body, "color"), "Prata"));
        v.setLicensePlate(licensePlate);
        v.setParkingSpot(spotEntity != null ? spotEntity.getCode() : blankToNull(stringVal(body, "parkingSpot")));
        v.setStatus(isManager ? "APPROVED" : "PENDING");
        v.setCreatedAt(LocalDateTime.now());
        v = vehicleRepository.save(v);

        if (!isManager) {
            notifyCondominiumManagers(
                    condominiumId,
                    condo.getName(),
                    "Solicitação de cadastro de veículo",
                    user.getFirstName() + " solicitou aprovação do veículo " + brandModel + " (" + licensePlate + ")."
            );
        }

        return new VehicleDTO(v);
    }

    @Transactional
    public VehicleDTO updateVehicleStatus(Long id, String status) {
        return updateVehicleStatus(id, status, null);
    }

    @Transactional
    public VehicleDTO updateVehicleStatus(Long id, String status, String rejectionReason) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado"));
        validateManagerAccess(v.getCondominium().getId());

        String normalized = status.toUpperCase(Locale.ROOT);
        v.setStatus(normalized);
        if (rejectionReason != null && !rejectionReason.isBlank()) {
            v.setRejectionReason(rejectionReason.trim());
        }
        v = vehicleRepository.save(v);

        notificationService.createDirectNotification(
                v.getOwner(),
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "Cadastro de veículo " + ("APPROVED".equals(normalized) ? "aprovado" : "atualizado"),
                "O cadastro do veículo " + v.getBrandModel() + " (" + v.getLicensePlate() + ") foi marcado como " + normalized + ".",
                v.getCondominium().getId(),
                v.getCondominium().getName()
        );

        return new VehicleDTO(v);
    }

    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado"));
        validateStaffOrOwner(v.getCondominium().getId(), v.getOwner().getId());
        if (v.getParkingSpotEntity() != null) {
            ParkingSpot spot = v.getParkingSpotEntity();
            spot.setStatus("AVAILABLE");
            parkingSpotRepository.save(spot);
        }
        vehicleRepository.delete(v);
    }

    // =========================================================================
    // 7. PET REGISTRY (Gestão de Animais de Estimação)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PetDTO> listPets(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        List<Pet> list = onlyMine
                ? petRepository.findByCondominiumIdAndOwnerIdOrderByCreatedAtDesc(condominiumId, user.getId())
                : petRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        return list.stream().map(PetDTO::new).toList();
    }

    @Transactional
    public PetDTO createPet(Long condominiumId, Map<String, Object> body) {
        User user = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);
        Unit unit = resolveUserUnit(user.getId(), condominiumId, body.get("unitId"));

        String name = stringVal(body, "name");
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do pet.");
        }

        Pet pet = new Pet();
        pet.setCondominium(condo);
        pet.setUnit(unit);
        pet.setOwner(user);
        pet.setName(name);
        pet.setSpecies(defaultIfBlank(stringVal(body, "species"), "DOG").toUpperCase(Locale.ROOT));
        pet.setBreed(blankToNull(stringVal(body, "breed")));
        pet.setSize(defaultIfBlank(stringVal(body, "size"), "MEDIUM").toUpperCase(Locale.ROOT));
        pet.setColor(blankToNull(stringVal(body, "color")));
        pet.setVaccinated(body.get("vaccinated") == null || Boolean.TRUE.equals(body.get("vaccinated")));
        pet.setLastVaccineDate(parseLocalDate(stringVal(body, "lastVaccineDate"), null));
        pet.setImageUrl(blankToNull(stringVal(body, "imageUrl")));
        pet.setNotes(blankToNull(stringVal(body, "notes")));
        pet.setStatus("ACTIVE");
        pet.setCreatedAt(LocalDateTime.now());
        pet = petRepository.save(pet);

        return new PetDTO(pet);
    }

    @Transactional
    public PetDTO updatePet(Long id, Map<String, Object> body) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado"));
        Long condoId = pet.getCondominium().getId();
        User caller = validateStaffOrOwner(condoId, pet.getOwner().getId());

        if (!stringVal(body, "name").isBlank()) {
            pet.setName(stringVal(body, "name"));
        }
        if (!stringVal(body, "species").isBlank()) {
            pet.setSpecies(stringVal(body, "species").toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("breed")) {
            pet.setBreed(blankToNull(stringVal(body, "breed")));
        }
        if (!stringVal(body, "size").isBlank()) {
            pet.setSize(stringVal(body, "size").toUpperCase(Locale.ROOT));
        }
        if (body.containsKey("color")) {
            pet.setColor(blankToNull(stringVal(body, "color")));
        }
        if (body.containsKey("vaccinated") && body.get("vaccinated") != null) {
            pet.setVaccinated(Boolean.TRUE.equals(body.get("vaccinated")));
        }
        if (body.containsKey("lastVaccineDate")) {
            pet.setLastVaccineDate(parseLocalDate(stringVal(body, "lastVaccineDate"), pet.getLastVaccineDate()));
        }
        if (body.containsKey("imageUrl")) {
            pet.setImageUrl(blankToNull(stringVal(body, "imageUrl")));
        }
        if (body.containsKey("notes")) {
            pet.setNotes(blankToNull(stringVal(body, "notes")));
        }
        if (condominiumAccessService.isManagerOrAdmin(caller, condoId)) {
            if (!stringVal(body, "status").isBlank()) {
                pet.setStatus(stringVal(body, "status").toUpperCase(Locale.ROOT));
            }
            if (body.containsKey("managerNotes")) {
                pet.setManagerNotes(blankToNull(stringVal(body, "managerNotes")));
            }
        }
        pet = petRepository.save(pet);
        return new PetDTO(pet);
    }

    @Transactional
    public void deletePet(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet não encontrado"));
        validateStaffOrOwner(pet.getCondominium().getId(), pet.getOwner().getId());
        petRepository.delete(pet);
    }

    // =========================================================================
    // 8. MOVING & LOADING SCHEDULE (Agendamento de Mudanças e Carga/Descarga)
    // =========================================================================

    @Transactional(readOnly = true)
    public MovingAvailabilityDTO checkMovingAvailability(Long condominiumId, LocalDate date) {
        validateMemberAccess(condominiumId);
        LocalDate target = date != null ? date : LocalDate.now().plusDays(1);
        List<MovingRequest> existing = movingRequestRepository
                .findByCondominiumIdAndMovingDateOrderByCreatedAtAsc(condominiumId, target)
                .stream()
                .filter(m -> !"CANCELLED".equals(m.getStatus()) && !"REJECTED".equals(m.getStatus()))
                .toList();

        boolean morningFree = existing.stream().noneMatch(m -> "MORNING".equals(m.getPeriod()) || "FULL_DAY".equals(m.getPeriod()));
        boolean afternoonFree = existing.stream().noneMatch(m -> "AFTERNOON".equals(m.getPeriod()) || "FULL_DAY".equals(m.getPeriod()));

        List<MovingRequestDTO> existingDtos = existing.stream().map(MovingRequestDTO::new).toList();
        String message = (morningFree || afternoonFree)
                ? "Data disponível! Turnos livres: "
                    + (morningFree && afternoonFree ? "Manhã (08h-12h) e Tarde (13h-17h)"
                        : (morningFree ? "Manhã (08h-12h)" : "Tarde (13h-17h)"))
                : "Todos os turnos de mudança e elevador de carga já estão ocupados nesta data.";

        return new MovingAvailabilityDTO(target, morningFree, afternoonFree, message, existingDtos);
    }

    @Transactional(readOnly = true)
    public List<MovingRequestDTO> listMovingRequests(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        List<MovingRequest> list = onlyMine
                ? movingRequestRepository.findByCondominiumIdAndRequestedByIdOrderByMovingDateDescCreatedAtDesc(condominiumId, user.getId())
                : movingRequestRepository.findByCondominiumIdOrderByMovingDateDescCreatedAtDesc(condominiumId);
        return list.stream().map(MovingRequestDTO::new).toList();
    }

    @Transactional
    public MovingRequestDTO createMovingRequest(Long condominiumId, Map<String, Object> body) {
        User user = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);
        Unit unit = resolveUserUnit(user.getId(), condominiumId, body.get("unitId"));

        LocalDate movingDate = parseLocalDate(stringVal(body, "movingDate"), LocalDate.now().plusDays(2));
        String period = defaultIfBlank(stringVal(body, "period"), "MORNING").toUpperCase(Locale.ROOT);

        MovingAvailabilityDTO availability = checkMovingAvailability(condominiumId, movingDate);
        if (("MORNING".equals(period) && !availability.morningAvailable())
                || ("AFTERNOON".equals(period) && !availability.afternoonAvailable())
                || ("FULL_DAY".equals(period) && (!availability.morningAvailable() || !availability.afternoonAvailable()))) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O turno selecionado não está disponível nesta data. Verifique a disponibilidade."
            );
        }

        MovingRequest req = new MovingRequest();
        req.setCondominium(condo);
        req.setUnit(unit);
        req.setRequestedBy(user);
        req.setType(defaultIfBlank(stringVal(body, "type"), "MOVE_IN").toUpperCase(Locale.ROOT));
        req.setMovingDate(movingDate);
        req.setPeriod(period);
        req.setCompanyName(blankToNull(stringVal(body, "companyName")));
        req.setVehiclePlates(blankToNull(stringVal(body, "vehiclePlates")));
        req.setContactPhone(blankToNull(stringVal(body, "contactPhone")));
        req.setNotes(blankToNull(stringVal(body, "notes")));
        req.setStatus("PENDING");
        req.setCreatedAt(LocalDateTime.now());
        req = movingRequestRepository.save(req);

        notifyCondominiumManagers(
                condominiumId,
                condo.getName(),
                "Solicitação de mudança / carga",
                user.getFirstName() + " solicitou agendamento (" + req.getType() + ") para " + movingDate + " (" + period + ")."
        );

        return new MovingRequestDTO(req);
    }

    @Transactional
    public MovingRequestDTO updateMovingStatus(Long id, String status, String reason) {
        MovingRequest req = movingRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de mudança não encontrada"));
        String normalized = status.toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(normalized)) {
            validateStaffOrOwner(req.getCondominium().getId(), req.getRequestedBy().getId());
        } else {
            validateManagerAccess(req.getCondominium().getId());
        }

        req.setStatus(normalized);
        if (reason != null && !reason.isBlank()) {
            req.setRejectionReason(reason.trim());
        }
        req = movingRequestRepository.save(req);

        notificationService.createDirectNotification(
                req.getRequestedBy(),
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "Agendamento de mudança: " + normalized,
                "Sua solicitação de mudança para " + req.getMovingDate() + " foi atualizada para " + normalized
                        + (req.getRejectionReason() != null ? " (" + req.getRejectionReason() + ")" : "."),
                req.getCondominium().getId(),
                req.getCondominium().getName()
        );

        return new MovingRequestDTO(req);
    }

    // =========================================================================
    // 9. DIGITAL DOCUMENTS, RULES & EMERGENCY CONTACTS (Documentos e Contatos)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CondominiumDocumentDTO> listDocuments(Long condominiumId) {
        validateMemberAccess(condominiumId);
        return documentRepository.findByCondominiumIdOrderByPinnedDescCreatedAtDesc(condominiumId)
                .stream()
                .map(CondominiumDocumentDTO::new)
                .toList();
    }

    @Transactional
    public CondominiumDocumentDTO createDocument(Long condominiumId, Map<String, Object> body) {
        User user = validateManagerAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        String title = stringVal(body, "title");
        String contentOrUrl = stringVal(body, "contentOrUrl");
        if (title.isBlank() || contentOrUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o título e o conteúdo/link do documento.");
        }

        CondominiumDocument doc = new CondominiumDocument();
        doc.setCondominium(condo);
        doc.setCreatedBy(user);
        doc.setCategory(defaultIfBlank(stringVal(body, "category"), "INTERNAL_RULES").toUpperCase(Locale.ROOT));
        doc.setTitle(title);
        doc.setSummary(blankToNull(stringVal(body, "summary")));
        doc.setContentOrUrl(contentOrUrl);
        doc.setContactPhone(blankToNull(stringVal(body, "contactPhone")));
        doc.setPinned(Boolean.TRUE.equals(body.get("pinned")));
        doc.setCreatedAt(LocalDateTime.now());
        doc = documentRepository.save(doc);

        return new CondominiumDocumentDTO(doc);
    }

    @Transactional
    public void deleteDocument(Long id) {
        CondominiumDocument doc = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado"));
        validateManagerAccess(doc.getCondominium().getId());
        documentRepository.delete(doc);
    }

    // =========================================================================
    // 10. SMART LOCKERS & DIGITAL POSTAL COMPARTMENTS (Armários Inteligentes)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<SmartLockerDTO> listLockers(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        boolean isStaff = condominiumAccessService.isStaffOrAdmin(user, condominiumId);
        if (onlyMine || !isStaff) {
            List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(user.getId());
            return lockerRepository.findByCondominiumIdOrderByDepositedAtDesc(condominiumId).stream()
                    .filter(c -> c.getRecipient().getId().equals(user.getId())
                            || (c.getUnit() != null && myUnitIds.contains(c.getUnit().getId())))
                    .map(SmartLockerDTO::new)
                    .toList();
        }
        return lockerRepository.findByCondominiumIdOrderByDepositedAtDesc(condominiumId)
                .stream()
                .map(SmartLockerDTO::new)
                .toList();
    }

    @Transactional
    public SmartLockerDTO depositLocker(Long condominiumId, Map<String, Object> body) {
        User depositor = validateStaffAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        Long unitId = numberVal(body.get("unitId"));
        if (unitId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a unidade de destino.");
        }
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada"));
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidade fora deste condomínio.");
        }

        Long recipientId = numberVal(body.get("recipientId"));
        User recipient;
        if (recipientId != null) {
            recipient = userRepository.findById(recipientId).orElse(null);
        } else {
            recipient = residentRepository.findFirstByUnitId(unitId).map(Resident::getUser).orElse(null);
        }
        if (recipient == null) {
            recipient = depositor;
        }

        String compNum = defaultIfBlank(stringVal(body, "compartmentNumber"), String.valueOf((int) (Math.random() * 50 + 1)));
        String pin = defaultIfBlank(stringVal(body, "pinCode"), String.format("%04d", (int) (Math.random() * 9000 + 1000)));

        SmartLockerCompartment comp = new SmartLockerCompartment();
        comp.setCondominium(condo);
        comp.setUnit(unit);
        comp.setRecipient(recipient);
        comp.setDepositedBy(depositor);
        comp.setLockerName(defaultIfBlank(stringVal(body, "lockerName"), "Armário Inteligente Bloco Central"));
        comp.setCompartmentNumber(compNum);
        comp.setPinCode(pin);
        comp.setDescription(blankToNull(stringVal(body, "description")));
        comp.setStatus("OCCUPIED");
        comp.setDepositedAt(LocalDateTime.now());
        comp = lockerRepository.save(comp);

        notificationService.createDirectNotification(
                recipient,
                NotificationType.SYSTEM_ANNOUNCEMENT,
                "Nova encomenda no Armário Inteligente!",
                "Sua encomenda está no compartimento " + compNum + " do " + comp.getLockerName()
                        + ". Código PIN de abertura: " + pin,
                condominiumId,
                condo.getName()
        );

        return new SmartLockerDTO(comp);
    }

    @Transactional
    public SmartLockerDTO retrieveLocker(Long id, String pin) {
        SmartLockerCompartment comp = lockerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Compartimento não encontrado"));
        Long condoId = comp.getCondominium().getId();
        User user = validateMemberAccess(condoId);

        boolean isStaff = condominiumAccessService.isStaffOrAdmin(user, condoId);
        boolean isRecipient = comp.getRecipient().getId().equals(user.getId())
                || (comp.getUnit() != null && residentRepository.findUnitIdsByUserId(user.getId()).contains(comp.getUnit().getId()));

        if (!isStaff && !isRecipient) {
            throw new ForbiddenException("Você não possui permissão para retirar encomendas de outra unidade.");
        }
        if (!isStaff && pin != null && !pin.isBlank() && !pin.trim().equals(comp.getPinCode())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Código PIN incorreto.");
        }

        comp.setStatus("RETRIEVED");
        comp.setRetrievedAt(LocalDateTime.now());
        comp = lockerRepository.save(comp);

        return new SmartLockerDTO(comp);
    }

    // =========================================================================
    // 11. EVENT GUEST LISTS & ACCESS CONTROL (Lista de Convidados de Festas)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<EventGuestListDTO> listGuestLists(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        boolean isStaff = condominiumAccessService.isStaffOrAdmin(user, condominiumId);
        List<EventGuestList> lists = (onlyMine || !isStaff)
                ? guestListRepository.findByCondominiumIdAndHostIdOrderByEventDateDesc(condominiumId, user.getId())
                : guestListRepository.findByCondominiumIdOrderByEventDateDesc(condominiumId);
        return lists.stream().map(EventGuestListDTO::new).toList();
    }

    @Transactional
    public EventGuestListDTO createGuestList(Long condominiumId, Map<String, Object> body) {
        User user = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        String title = stringVal(body, "eventTitle");
        if (title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o título do evento.");
        }

        Reservation reservation = null;
        Long reservationId = numberVal(body.get("reservationId"));
        if (reservationId != null) {
            reservation = reservationRepository.findById(reservationId).orElse(null);
        }

        LocalDateTime eventDate = parseDateTime(stringVal(body, "eventDate"), LocalDateTime.now().plusDays(1));

        EventGuestList list = new EventGuestList();
        list.setCondominium(condo);
        list.setHost(user);
        list.setReservation(reservation);
        list.setEventTitle(title);
        list.setEventDate(eventDate);
        list.setNotes(blankToNull(stringVal(body, "notes")));
        list.setCreatedAt(LocalDateTime.now());

        Object rawGuests = body.get("guests");
        if (rawGuests instanceof Collection<?> col) {
            for (Object obj : col) {
                if (obj instanceof Map<?, ?> m) {
                    String gName = m.get("guestName") != null ? m.get("guestName").toString().trim() : "";
                    String doc = m.get("documentNumber") != null ? m.get("documentNumber").toString().trim() : null;
                    if (!gName.isEmpty()) {
                        GuestEntry g = new GuestEntry();
                        g.setGuestList(list);
                        g.setGuestName(gName);
                        g.setDocumentNumber(doc);
                        g.setCheckedIn(false);
                        list.getGuests().add(g);
                    }
                }
            }
        }

        list = guestListRepository.save(list);
        return new EventGuestListDTO(list);
    }

    @Transactional
    public EventGuestListDTO addGuest(Long listId, Map<String, Object> body) {
        EventGuestList list = guestListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("Lista de convidados não encontrada"));
        validateStaffOrOwner(list.getCondominium().getId(), list.getHost().getId());

        String guestName = stringVal(body, "guestName");
        if (guestName.isBlank()) {
            guestName = stringVal(body, "name");
        }
        if (guestName.isBlank()) {
            guestName = stringVal(body, "fullName");
        }
        if (guestName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do convidado.");
        }

        GuestEntry g = new GuestEntry();
        g.setGuestList(list);
        g.setGuestName(guestName);
        g.setDocumentNumber(blankToNull(stringVal(body, "documentNumber")));
        g.setCheckedIn(false);
        guestEntryRepository.save(g);
        list.getGuests().add(g);

        return new EventGuestListDTO(list);
    }

    @Transactional
    public EventGuestListDTO checkInGuest(Long guestId) {
        GuestEntry g = guestEntryRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado"));
        validateStaffAccess(g.getGuestList().getCondominium().getId());

        g.setCheckedIn(true);
        g.setCheckedInAt(LocalDateTime.now());
        g = guestEntryRepository.save(g);

        return new EventGuestListDTO(g.getGuestList());
    }

    @Transactional
    public EventGuestListDTO checkOutGuest(Long guestId) {
        GuestEntry g = guestEntryRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado"));
        validateStaffAccess(g.getGuestList().getCondominium().getId());

        g.setCheckedOutAt(LocalDateTime.now());
        g = guestEntryRepository.save(g);

        return new EventGuestListDTO(g.getGuestList());
    }

    @Transactional
    public EventGuestListDTO removeGuest(Long guestId) {
        GuestEntry g = guestEntryRepository.findById(guestId)
                .orElseThrow(() -> new ResourceNotFoundException("Convidado não encontrado"));
        EventGuestList list = g.getGuestList();
        validateStaffOrOwner(list.getCondominium().getId(), list.getHost().getId());

        list.getGuests().remove(g);
        guestEntryRepository.delete(g);
        return new EventGuestListDTO(list);
    }

    // =========================================================================
    // 12. INFRACTIONS & WARNINGS WITH DEFENSE (Advertências e Multas)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<CondominiumInfractionDTO> listInfractions(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        List<CondominiumInfraction> list;
        if (onlyMine || !condominiumAccessService.isStaffOrAdmin(user, condominiumId)) {
            List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(user.getId());
            list = infractionRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId).stream()
                    .filter(inf -> myUnitIds.contains(inf.getUnit().getId()))
                    .toList();
        } else {
            list = infractionRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        }
        return list.stream().map(CondominiumInfractionDTO::new).toList();
    }

    @Transactional
    public CondominiumInfractionDTO createInfraction(Long condominiumId, Map<String, Object> body) {
        User staff = validateManagerAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        Long unitId = numberVal(body.get("unitId"));
        if (unitId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a unidade notificada.");
        }
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada"));
        if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidade fora deste condomínio.");
        }

        String title = stringVal(body, "title");
        String description = stringVal(body, "description");
        if (title.isBlank() || description.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe título e descrição da ocorrência/infração.");
        }

        BigDecimal penalty = null;
        Object rawPenalty = body.get("penaltyAmount");
        if (rawPenalty != null && !rawPenalty.toString().isBlank()) {
            try {
                penalty = new BigDecimal(rawPenalty.toString().replace(",", ".").trim());
            } catch (Exception ignored) {
            }
        }

        CondominiumInfraction inf = new CondominiumInfraction();
        inf.setCondominium(condo);
        inf.setUnit(unit);
        inf.setIssuedBy(staff);
        inf.setType(defaultIfBlank(stringVal(body, "type"), "WARNING").toUpperCase(Locale.ROOT));
        inf.setTitle(title);
        inf.setDescription(description);
        inf.setPenaltyAmount(penalty);
        inf.setDefenseDeadline(LocalDate.now().plusDays(10));
        inf.setStatus("ISSUED");
        inf.setCreatedAt(LocalDateTime.now());
        inf.setUpdatedAt(LocalDateTime.now());
        inf = infractionRepository.save(inf);

        List<Resident> unitResidents = residentRepository.findAllByUnitIdWithUser(unitId);
        for (Resident r : unitResidents) {
            notificationService.createDirectNotification(
                    r.getUser(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Notificação de Condomínio: " + inf.getType(),
                    "Sua unidade recebeu uma notificação (" + inf.getTitle() + "). Prazo para envio de recurso/defesa até "
                            + inf.getDefenseDeadline() + ".",
                    condominiumId,
                    condo.getName()
            );
        }

        return new CondominiumInfractionDTO(inf);
    }

    @Transactional
    public CondominiumInfractionDTO submitInfractionDefense(Long id, String defenseText) {
        CondominiumInfraction inf = infractionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        Long condoId = inf.getCondominium().getId();
        User user = validateMemberAccess(condoId);

        if (!condominiumAccessService.isManagerOrAdmin(user, condoId)) {
            List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(user.getId());
            if (!myUnitIds.contains(inf.getUnit().getId())) {
                throw new ForbiddenException("Apenas moradores da unidade autuada podem protocolar recurso.");
            }
        }

        if (defenseText == null || defenseText.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a fundamentação da sua defesa/recurso.");
        }

        inf.setResidentDefense(defenseText.trim());
        inf.setDefenseSubmittedAt(LocalDateTime.now());
        inf.setStatus("DEFENSE_SUBMITTED");
        inf.setUpdatedAt(LocalDateTime.now());
        inf = infractionRepository.save(inf);

        notifyCondominiumManagers(
                condoId,
                inf.getCondominium().getName(),
                "Recurso de Infração protocolado",
                "O morador da unidade " + inf.getUnit().getIdentifier() + " protocolou defesa para: " + inf.getTitle()
        );

        return new CondominiumInfractionDTO(inf);
    }

    @Transactional
    public CondominiumInfractionDTO judgeInfraction(Long id, String status, String judgment) {
        CondominiumInfraction inf = infractionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        validateManagerAccess(inf.getCondominium().getId());

        String normalized = defaultIfBlank(status, "ACCEPTED").toUpperCase(Locale.ROOT);
        inf.setStatus(normalized);
        if (judgment != null && !judgment.isBlank()) {
            inf.setManagerJudgment(judgment.trim());
        }
        inf.setUpdatedAt(LocalDateTime.now());
        inf = infractionRepository.save(inf);

        List<Resident> unitResidents = residentRepository.findAllByUnitIdWithUser(inf.getUnit().getId());
        for (Resident r : unitResidents) {
            notificationService.createDirectNotification(
                    r.getUser(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Julgamento de recurso: " + inf.getTitle(),
                    "O recurso da sua unidade foi julgado como: " + normalized
                            + (inf.getManagerJudgment() != null ? " — Parecer: " + inf.getManagerJudgment() : "."),
                    inf.getCondominium().getId(),
                    inf.getCondominium().getName()
            );
        }

        return new CondominiumInfractionDTO(inf);
    }

    // =========================================================================
    // HELPERS & DTOs
    // =========================================================================

    private User validateMemberAccess(Long condominiumId) {
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!user.hasRole("ROLE_ADMIN")) {
            condominiumAccessService.validateUserBelongsToCondominium(user.getId(), condominiumId);
        }
        return user;
    }

    private User validateStaffAccess(Long condominiumId) {
        condominiumAccessService.validateStaffAccess(condominiumId);
        return authenticatedUserService.getAuthenticatedUser();
    }

    private User validateManagerAccess(Long condominiumId) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        return authenticatedUserService.getAuthenticatedUser();
    }

    private User validateStaffOrOwner(Long condominiumId, Long ownerId) {
        User user = validateMemberAccess(condominiumId);
        if (user.getId().equals(ownerId)
                || condominiumAccessService.isStaffOrAdmin(user, condominiumId)) {
            return user;
        }
        throw new ForbiddenException("Sem permissão para alterar este registro.");
    }

    private Condominium getCondominium(Long condominiumId) {
        return condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));
    }

    private Unit resolveUserUnit(Long userId, Long condominiumId, Object rawUnitId) {
        Long explicitUnitId = numberVal(rawUnitId);
        if (explicitUnitId != null) {
            Unit unit = unitRepository.findById(explicitUnitId)
                    .orElseThrow(() -> new ResourceNotFoundException("Unidade não encontrada: " + explicitUnitId));
            if (!unit.getSector().getCondominium().getId().equals(condominiumId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A unidade não pertence a este condomínio.");
            }
            User caller = authenticatedUserService.getAuthenticatedUser();
            if (!condominiumAccessService.isStaffOrAdmin(caller, condominiumId)) {
                List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(userId);
                if (!myUnitIds.isEmpty() && !myUnitIds.contains(explicitUnitId)) {
                    throw new ForbiddenException("Você não pertence à unidade informada.");
                }
            }
            return unit;
        }
        return residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(userId, condominiumId)
                .map(Resident::getUnit)
                .orElse(null);
    }

    private void notifyCondominiumManagers(Long condominiumId, String condominiumName, String title, String message) {
        var managers = membershipRepository.findActiveManagersByCondominiumId(condominiumId);
        for (var mgr : managers) {
            notificationService.createDirectNotification(
                    mgr.getUser(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    title,
                    message,
                    condominiumId,
                    condominiumName
            );
        }
    }

    private static LocalDateTime parseDateTime(String raw, LocalDateTime fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            String clean = raw.trim();
            if (clean.length() == 10) {
                return LocalDate.parse(clean).atStartOfDay();
            }
            return LocalDateTime.parse(clean.substring(0, Math.min(19, clean.length())));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static LocalDate parseLocalDate(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return LocalDate.parse(raw.trim().substring(0, 10));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String stringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key);
        return val == null ? "" : val.toString().trim();
    }

    private static String blankToNull(String val) {
        return val == null || val.isBlank() ? null : val.trim();
    }

    private static String defaultIfBlank(String val, String fallback) {
        return val == null || val.isBlank() ? fallback : val.trim();
    }

    private static Long numberVal(Object raw) {
        if (raw instanceof Number n) return n.longValue();
        if (raw != null && !raw.toString().isBlank()) {
            try {
                return Long.parseLong(raw.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private static String formatUnit(Unit unit) {
        if (unit == null) return null;
        return unit.getSector().getName() + " • Unid. " + unit.getIdentifier();
    }

    private static String formatUser(User user) {
        if (user == null) return "";
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    public record OccurrenceTicketDTO(
            Long id,
            Long condominiumId,
            Long authorId,
            String authorName,
            String unitLabel,
            String category,
            String title,
            String description,
            String location,
            String status,
            String adminResponse,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public OccurrenceTicketDTO(OccurrenceTicket e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getAuthor().getId(),
                    formatUser(e.getAuthor()),
                    formatUnit(e.getUnit()),
                    e.getCategory(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getLocation(),
                    e.getStatus(),
                    e.getAdminResponse(),
                    e.getCreatedAt(),
                    e.getUpdatedAt()
            );
        }
    }

    public record VisitorAccessDTO(
            Long id,
            Long condominiumId,
            Long residentId,
            String residentName,
            String unitLabel,
            String visitorName,
            String documentNumber,
            String phone,
            String visitorType,
            String vehiclePlate,
            String accessCode,
            String qrCodePayload,
            String status,
            String notes,
            LocalDateTime expectedAt,
            LocalDateTime validFrom,
            LocalDateTime validUntil,
            LocalDateTime checkedInAt,
            String checkedInByName,
            LocalDateTime checkedOutAt,
            String checkedOutByName,
            LocalDateTime createdAt
    ) {
        public VisitorAccessDTO(VisitorAccess e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getResident().getId(),
                    formatUser(e.getResident()),
                    formatUnit(e.getUnit()),
                    e.getVisitorName(),
                    e.getDocumentNumber(),
                    e.getPhone(),
                    e.getVisitorType(),
                    e.getVehiclePlate(),
                    e.getAccessCode(),
                    e.getQrCodePayload(),
                    e.getStatus(),
                    e.getNotes(),
                    e.getExpectedAt(),
                    e.getValidFrom(),
                    e.getValidUntil(),
                    e.getCheckedInAt(),
                    formatUser(e.getCheckedInBy()),
                    e.getCheckedOutAt(),
                    formatUser(e.getCheckedOutBy()),
                    e.getCreatedAt()
            );
        }
    }

    public record PollOptionDTO(Long id, String label, int votes) {}

    public record PollDTO(
            Long id,
            Long condominiumId,
            String title,
            String description,
            Boolean active,
            LocalDateTime endsAt,
            LocalDateTime createdAt,
            int totalVotes,
            Long myOptionId,
            List<PollOptionDTO> options,
            String createdByName
    ) {}

    public record LostFoundItemDTO(
            Long id,
            Long condominiumId,
            Long reportedById,
            String reportedByName,
            String claimedByName,
            String type,
            String category,
            String title,
            String description,
            String location,
            String imageUrl,
            String contactInfo,
            String claimMessage,
            String resolutionNotes,
            String status,
            int messageCount,
            LocalDateTime createdAt
    ) {
        public LostFoundItemDTO(LostFoundItem e) {
            this(e, 0);
        }

        public LostFoundItemDTO(LostFoundItem e, int messageCount) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getReportedBy().getId(),
                    formatUser(e.getReportedBy()),
                    e.getClaimedBy() != null ? formatUser(e.getClaimedBy()) : null,
                    e.getType(),
                    e.getCategory(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getLocation(),
                    e.getImageUrl(),
                    e.getContactInfo(),
                    e.getClaimMessage(),
                    e.getResolutionNotes(),
                    e.getStatus(),
                    messageCount,
                    e.getCreatedAt()
            );
        }
    }

    public record LostFoundMessageDTO(
            Long id,
            Long itemId,
            Long senderId,
            String senderName,
            String message,
            LocalDateTime createdAt
    ) {
        public LostFoundMessageDTO(LostFoundMessage m) {
            this(
                    m.getId(),
                    m.getItem().getId(),
                    m.getSender().getId(),
                    formatUser(m.getSender()),
                    m.getMessage(),
                    m.getCreatedAt()
            );
        }
    }

    public record ClassifiedAdDTO(
            Long id,
            Long condominiumId,
            Long authorId,
            String authorName,
            String unitLabel,
            String category,
            String title,
            String description,
            BigDecimal price,
            String contactPhone,
            String status,
            Integer interestCount,
            LocalDateTime createdAt
    ) {
        public ClassifiedAdDTO(ClassifiedAd e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getAuthor().getId(),
                    formatUser(e.getAuthor()),
                    formatUnit(e.getUnit()),
                    e.getCategory(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getPrice(),
                    e.getContactPhone(),
                    e.getStatus(),
                    e.getInterestCount() != null ? e.getInterestCount() : 0,
                    e.getCreatedAt()
            );
        }
    }

    public record ParkingSpotDTO(
            Long id,
            Long condominiumId,
            Long unitId,
            String unitLabel,
            String code,
            String spotType,
            String locationDescription,
            String status,
            LocalDateTime createdAt
    ) {
        public ParkingSpotDTO(ParkingSpot s) {
            this(
                    s.getId(),
                    s.getCondominium().getId(),
                    s.getUnit() != null ? s.getUnit().getId() : null,
                    formatUnit(s.getUnit()),
                    s.getCode(),
                    s.getSpotType(),
                    s.getLocationDescription(),
                    s.getStatus(),
                    s.getCreatedAt()
            );
        }
    }

    public record VehicleDTO(
            Long id,
            Long condominiumId,
            Long ownerId,
            String ownerName,
            String unitLabel,
            Long parkingSpotId,
            String type,
            String brandModel,
            String color,
            String licensePlate,
            String parkingSpot,
            String status,
            String rejectionReason,
            LocalDateTime createdAt
    ) {
        public VehicleDTO(Vehicle e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getOwner().getId(),
                    formatUser(e.getOwner()),
                    formatUnit(e.getUnit()),
                    e.getParkingSpotEntity() != null ? e.getParkingSpotEntity().getId() : null,
                    e.getType(),
                    e.getBrandModel(),
                    e.getColor(),
                    e.getLicensePlate(),
                    e.getParkingSpot(),
                    e.getStatus(),
                    e.getRejectionReason(),
                    e.getCreatedAt()
            );
        }
    }

    public record PlateCheckResultDTO(
            String plate,
            boolean found,
            String message,
            List<VehicleDTO> residentVehicles,
            List<VisitorAccessDTO> visitorVehicles
    ) {}

    public record PetDTO(
            Long id,
            Long condominiumId,
            Long ownerId,
            String ownerName,
            String unitLabel,
            String name,
            String species,
            String breed,
            String size,
            String color,
            Boolean vaccinated,
            LocalDate lastVaccineDate,
            String imageUrl,
            String notes,
            String status,
            String managerNotes,
            LocalDateTime createdAt
    ) {
        public PetDTO(Pet e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getOwner().getId(),
                    formatUser(e.getOwner()),
                    formatUnit(e.getUnit()),
                    e.getName(),
                    e.getSpecies(),
                    e.getBreed(),
                    e.getSize(),
                    e.getColor(),
                    e.getVaccinated(),
                    e.getLastVaccineDate(),
                    e.getImageUrl(),
                    e.getNotes(),
                    e.getStatus(),
                    e.getManagerNotes(),
                    e.getCreatedAt()
            );
        }
    }

    public record MovingRequestDTO(
            Long id,
            Long condominiumId,
            Long requestedById,
            String requestedByName,
            String unitLabel,
            String type,
            LocalDate movingDate,
            String period,
            String companyName,
            String vehiclePlates,
            String contactPhone,
            String notes,
            String rejectionReason,
            String status,
            LocalDateTime createdAt
    ) {
        public MovingRequestDTO(MovingRequest e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getRequestedBy().getId(),
                    formatUser(e.getRequestedBy()),
                    formatUnit(e.getUnit()),
                    e.getType(),
                    e.getMovingDate(),
                    e.getPeriod(),
                    e.getCompanyName(),
                    e.getVehiclePlates(),
                    e.getContactPhone(),
                    e.getNotes(),
                    e.getRejectionReason(),
                    e.getStatus(),
                    e.getCreatedAt()
            );
        }
    }

    public record MovingAvailabilityDTO(
            LocalDate date,
            boolean morningAvailable,
            boolean afternoonAvailable,
            String message,
            List<MovingRequestDTO> scheduledMoves
    ) {}

    public record CondominiumDocumentDTO(
            Long id,
            Long condominiumId,
            String createdByName,
            String category,
            String title,
            String summary,
            String contentOrUrl,
            String contactPhone,
            Boolean pinned,
            LocalDateTime createdAt
    ) {
        public CondominiumDocumentDTO(CondominiumDocument e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    formatUser(e.getCreatedBy()),
                    e.getCategory(),
                    e.getTitle(),
                    e.getSummary(),
                    e.getContentOrUrl(),
                    e.getContactPhone(),
                    e.getPinned(),
                    e.getCreatedAt()
            );
        }
    }

    public record SmartLockerDTO(
            Long id,
            Long condominiumId,
            Long unitId,
            String unitLabel,
            Long recipientId,
            String recipientName,
            String depositedByName,
            String lockerName,
            String compartmentNumber,
            String pinCode,
            String description,
            String status,
            LocalDateTime depositedAt,
            LocalDateTime retrievedAt
    ) {
        public SmartLockerDTO(SmartLockerCompartment e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getUnit().getId(),
                    formatUnit(e.getUnit()),
                    e.getRecipient().getId(),
                    formatUser(e.getRecipient()),
                    e.getDepositedBy() != null ? formatUser(e.getDepositedBy()) : null,
                    e.getLockerName(),
                    e.getCompartmentNumber(),
                    e.getPinCode(),
                    e.getDescription(),
                    e.getStatus(),
                    e.getDepositedAt(),
                    e.getRetrievedAt()
            );
        }
    }

    public record GuestEntryDTO(
            Long id,
            String guestName,
            String documentNumber,
            Boolean checkedIn,
            LocalDateTime checkedInAt,
            LocalDateTime checkedOutAt
    ) {
        public GuestEntryDTO(GuestEntry e) {
            this(e.getId(), e.getGuestName(), e.getDocumentNumber(), e.getCheckedIn(), e.getCheckedInAt(), e.getCheckedOutAt());
        }
    }

    public record EventGuestListDTO(
            Long id,
            Long condominiumId,
            Long hostId,
            String hostName,
            Long reservationId,
            String eventTitle,
            LocalDateTime eventDate,
            String notes,
            int totalGuests,
            int presentGuests,
            LocalDateTime createdAt,
            List<GuestEntryDTO> guests
    ) {
        public EventGuestListDTO(EventGuestList e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getHost().getId(),
                    formatUser(e.getHost()),
                    e.getReservation() != null ? e.getReservation().getId() : null,
                    e.getEventTitle(),
                    e.getEventDate(),
                    e.getNotes(),
                    e.getGuests() != null ? e.getGuests().size() : 0,
                    e.getGuests() != null ? (int) e.getGuests().stream().filter(g -> Boolean.TRUE.equals(g.getCheckedIn())).count() : 0,
                    e.getCreatedAt(),
                    e.getGuests() != null ? e.getGuests().stream().map(GuestEntryDTO::new).toList() : List.of()
            );
        }
    }

    public record CondominiumInfractionDTO(
            Long id,
            Long condominiumId,
            Long unitId,
            String unitLabel,
            Long issuedById,
            String issuedByName,
            String type,
            String title,
            String description,
            BigDecimal penaltyAmount,
            LocalDate defenseDeadline,
            String status,
            String residentDefense,
            LocalDateTime defenseSubmittedAt,
            String managerJudgment,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public CondominiumInfractionDTO(CondominiumInfraction e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getUnit().getId(),
                    formatUnit(e.getUnit()),
                    e.getIssuedBy().getId(),
                    formatUser(e.getIssuedBy()),
                    e.getType(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getPenaltyAmount(),
                    e.getDefenseDeadline(),
                    e.getStatus(),
                    e.getResidentDefense(),
                    e.getDefenseSubmittedAt(),
                    e.getManagerJudgment(),
                    e.getCreatedAt(),
                    e.getUpdatedAt()
            );
        }
    }
}
