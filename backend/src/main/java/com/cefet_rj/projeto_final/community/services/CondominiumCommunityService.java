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
    private final ClassifiedAdRepository classifiedRepository;
    private final VehicleRepository vehicleRepository;
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
    // 2. VISITOR ACCESS & PRE-AUTHORIZATION (Visitantes e Prestadores)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<VisitorAccessDTO> listVisitors(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        List<VisitorAccess> list = onlyMine
                ? visitorRepository.findByCondominiumIdAndResidentIdOrderByCreatedAtDesc(condominiumId, user.getId())
                : visitorRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        return list.stream().map(VisitorAccessDTO::new).toList();
    }

    @Transactional
    public VisitorAccessDTO createVisitorAccess(Long condominiumId, Map<String, Object> body) {
        User caller = validateMemberAccess(condominiumId);
        Condominium condo = getCondominium(condominiumId);

        String visitorName = stringVal(body, "visitorName");
        if (visitorName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome do visitante ou prestador.");
        }

        boolean requestResidentApproval = Boolean.TRUE.equals(body.get("requestApproval"));
        Long targetResidentId = numberVal(body.get("residentId"));
        Long targetUnitId = numberVal(body.get("unitId"));

        Unit unit = resolveUserUnit(
                targetResidentId != null ? targetResidentId : caller.getId(),
                condominiumId,
                targetUnitId
        );

        User resident = caller;
        if (targetResidentId != null) {
            resident = userRepository.findById(targetResidentId).orElse(caller);
        } else if (unit != null && (caller.hasRole("ROLE_DOORMAN") || caller.hasRole("ROLE_MANAGER") || caller.hasRole("ROLE_ADMIN"))) {
            var resOpt = residentRepository.findFirstByUnitId(unit.getId());
            if (resOpt.isPresent()) {
                resident = resOpt.get().getUser();
            }
        }

        VisitorAccess access = new VisitorAccess();
        access.setCondominium(condo);
        access.setUnit(unit);
        access.setResident(resident);
        access.setVisitorName(visitorName);
        access.setDocumentNumber(blankToNull(stringVal(body, "documentNumber")));
        access.setPhone(blankToNull(stringVal(body, "phone")));
        access.setVisitorType(defaultIfBlank(stringVal(body, "visitorType"), "VISITOR").toUpperCase(Locale.ROOT));
        access.setVehiclePlate(blankToNull(stringVal(body, "vehiclePlate").toUpperCase(Locale.ROOT)));
        access.setAccessCode("AC-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT));
        access.setNotes(blankToNull(stringVal(body, "notes")));
        access.setExpectedAt(LocalDateTime.now());
        access.setStatus(requestResidentApproval ? "REQUESTED" : "AUTHORIZED");
        access.setCreatedAt(LocalDateTime.now());
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
        validateMemberAccess(access.getCondominium().getId());

        String normalized = nextStatus.toUpperCase(Locale.ROOT);
        access.setStatus(normalized);
        if ("CHECKED_IN".equals(normalized)) {
            access.setCheckedInAt(LocalDateTime.now());
            notificationService.createDirectNotification(
                    access.getResident(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    "Entrada registrada na portaria",
                    access.getVisitorName() + " acabou de entrar no condomínio.",
                    access.getCondominium().getId(),
                    access.getCondominium().getName()
            );
        } else if ("CHECKED_OUT".equals(normalized)) {
            access.setCheckedOutAt(LocalDateTime.now());
        }
        access = visitorRepository.save(access);
        return new VisitorAccessDTO(access);
    }

    // =========================================================================
    // 3. POLLS & VIRTUAL ASSEMBLIES (Enquetes e Assembleias Virtuais)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PollDTO> listPolls(Long condominiumId) {
        User user = validateMemberAccess(condominiumId);
        List<Poll> polls = pollRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        return polls.stream().map(poll -> toPollDTO(poll, user.getId())).toList();
    }

    @Transactional
    public PollDTO createPoll(Long condominiumId, Map<String, Object> body) {
        User user = validateStaffAccess(condominiumId);
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
        poll.setEndsAt(LocalDateTime.now().plusDays(7));
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

        if (!Boolean.TRUE.equals(poll.getActive())) {
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
        User user = validateStaffAccess(poll.getCondominium().getId());
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

        return new PollDTO(
                poll.getId(),
                poll.getCondominium().getId(),
                poll.getTitle(),
                poll.getDescription(),
                poll.getActive(),
                poll.getEndsAt(),
                poll.getCreatedAt(),
                total,
                myOptionId,
                optDtos
        );
    }

    // =========================================================================
    // 4. LOST AND FOUND (Achados e Perdidos da Portaria)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<LostFoundItemDTO> listLostFound(Long condominiumId) {
        validateMemberAccess(condominiumId);
        return lostFoundRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId)
                .stream()
                .map(LostFoundItemDTO::new)
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
        item.setTitle(title);
        item.setDescription(description);
        item.setLocation(blankToNull(stringVal(body, "location")));
        item.setContactInfo(blankToNull(stringVal(body, "contactInfo")));
        item.setStatus("OPEN");
        item.setCreatedAt(LocalDateTime.now());
        item = lostFoundRepository.save(item);

        return new LostFoundItemDTO(item);
    }

    @Transactional
    public LostFoundItemDTO requestClaimLostFound(Long id) {
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado"));
        User user = validateMemberAccess(item.getCondominium().getId());

        item.setClaimedBy(user);
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

        return new LostFoundItemDTO(item);
    }

    @Transactional
    public LostFoundItemDTO markLostFoundReturned(Long id) {
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Registro não encontrado"));
        validateMemberAccess(item.getCondominium().getId());

        item.setStatus("RETURNED");
        item.setReturnedAt(LocalDateTime.now());
        item = lostFoundRepository.save(item);

        return new LostFoundItemDTO(item);
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
    // 6. VEHICLES & PLATE LOOKUP (Cadastro de Veículos e Consulta de Placas)
    // =========================================================================

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
        String licensePlate = stringVal(body, "licensePlate").toUpperCase(Locale.ROOT);
        if (brandModel.isBlank() || licensePlate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o modelo e a placa do veículo.");
        }

        boolean isStaff = user.hasRole("ROLE_ADMIN") || user.hasRole("ROLE_MANAGER");

        Vehicle v = new Vehicle();
        v.setCondominium(condo);
        v.setUnit(unit);
        v.setOwner(user);
        v.setType(defaultIfBlank(stringVal(body, "type"), "CAR").toUpperCase(Locale.ROOT));
        v.setBrandModel(brandModel);
        v.setColor(defaultIfBlank(stringVal(body, "color"), "Prata"));
        v.setLicensePlate(licensePlate);
        v.setParkingSpot(blankToNull(stringVal(body, "parkingSpot")));
        v.setStatus(isStaff ? "APPROVED" : "PENDING");
        v.setCreatedAt(LocalDateTime.now());
        v = vehicleRepository.save(v);

        if (!isStaff) {
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
        Vehicle v = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado"));
        validateStaffAccess(v.getCondominium().getId());

        String normalized = status.toUpperCase(Locale.ROOT);
        v.setStatus(normalized);
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
        vehicleRepository.delete(v);
    }

    // =========================================================================
    // 7. PET REGISTRY (Censo de Pets)
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
        pet.setNotes(blankToNull(stringVal(body, "notes")));
        pet.setCreatedAt(LocalDateTime.now());
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

        LocalDate movingDate = LocalDate.now().plusDays(2);
        String rawDate = stringVal(body, "movingDate");
        if (!rawDate.isBlank()) {
            try {
                movingDate = LocalDate.parse(rawDate.substring(0, 10));
            } catch (Exception ignored) {
            }
        }
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
            validateStaffAccess(req.getCondominium().getId());
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
        User user = validateStaffAccess(condominiumId);
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
        validateStaffAccess(doc.getCondominium().getId());
        documentRepository.delete(doc);
    }

    // =========================================================================
    // 10. SMART LOCKERS & DIGITAL POSTAL COMPARTMENTS (Armários Inteligentes)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<SmartLockerDTO> listLockers(Long condominiumId, boolean onlyMine) {
        User user = validateMemberAccess(condominiumId);
        List<SmartLockerCompartment> list = onlyMine
                ? lockerRepository.findByCondominiumIdAndRecipientIdOrderByDepositedAtDesc(condominiumId, user.getId())
                : lockerRepository.findByCondominiumIdOrderByDepositedAtDesc(condominiumId);
        return list.stream().map(SmartLockerDTO::new).toList();
    }

    @Transactional
    public SmartLockerDTO depositLocker(Long condominiumId, Map<String, Object> body) {
        User depositor = validateMemberAccess(condominiumId);
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
        User user = validateMemberAccess(comp.getCondominium().getId());

        boolean isStaff = user.hasRole("ROLE_ADMIN") || user.hasRole("ROLE_MANAGER") || user.hasRole("ROLE_DOORMAN");
        boolean isRecipient = comp.getRecipient().getId().equals(user.getId());

        if (!isStaff && !isRecipient) {
            if (pin == null || !pin.trim().equals(comp.getPinCode())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Código PIN incorreto.");
            }
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
        List<EventGuestList> lists = onlyMine
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

        LocalDateTime eventDate = LocalDateTime.now().plusDays(1);
        String rawDate = stringVal(body, "eventDate");
        if (!rawDate.isBlank()) {
            try {
                eventDate = LocalDateTime.parse(rawDate.substring(0, 19));
            } catch (Exception ignored) {
            }
        }

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
        validateMemberAccess(g.getGuestList().getCondominium().getId());

        g.setCheckedIn(true);
        g.setCheckedInAt(LocalDateTime.now());
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
        if (onlyMine) {
            List<Long> myUnitIds = residentRepository.findUnitIdsByUserId(user.getId());
            list = infractionRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId).stream()
                    .filter(inf -> myUnitIds.contains(inf.getUnit().getId()))
                    .toList();
        } else {
            validateStaffAccess(condominiumId);
            list = infractionRepository.findByCondominiumIdOrderByCreatedAtDesc(condominiumId);
        }
        return list.stream().map(CondominiumInfractionDTO::new).toList();
    }

    @Transactional
    public CondominiumInfractionDTO createInfraction(Long condominiumId, Map<String, Object> body) {
        User staff = validateStaffAccess(condominiumId);
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
        User user = validateMemberAccess(inf.getCondominium().getId());

        if (defenseText == null || defenseText.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a fundamentação da sua defesa/recurso.");
        }

        inf.setResidentDefense(defenseText.trim());
        inf.setDefenseSubmittedAt(LocalDateTime.now());
        inf.setStatus("DEFENSE_SUBMITTED");
        inf.setUpdatedAt(LocalDateTime.now());
        inf = infractionRepository.save(inf);

        notifyCondominiumManagers(
                inf.getCondominium().getId(),
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
        validateStaffAccess(inf.getCondominium().getId());

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
        User user = validateMemberAccess(condominiumId);
        if (!user.hasRole("ROLE_ADMIN") && !user.hasRole("ROLE_MANAGER") && !user.hasRole("ROLE_DOORMAN")) {
            throw new ForbiddenException("Apenas a administração ou portaria pode realizar esta operação.");
        }
        return user;
    }

    private User validateStaffOrOwner(Long condominiumId, Long ownerId) {
        User user = validateMemberAccess(condominiumId);
        if (user.getId().equals(ownerId)
                || user.hasRole("ROLE_ADMIN")
                || user.hasRole("ROLE_MANAGER")
                || user.hasRole("ROLE_DOORMAN")) {
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
            return unitRepository.findById(explicitUnitId).orElse(null);
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
            String status,
            String notes,
            LocalDateTime expectedAt,
            LocalDateTime checkedInAt,
            LocalDateTime checkedOutAt,
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
                    e.getStatus(),
                    e.getNotes(),
                    e.getExpectedAt(),
                    e.getCheckedInAt(),
                    e.getCheckedOutAt(),
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
            List<PollOptionDTO> options
    ) {}

    public record LostFoundItemDTO(
            Long id,
            Long condominiumId,
            Long reportedById,
            String reportedByName,
            String claimedByName,
            String type,
            String title,
            String description,
            String location,
            String contactInfo,
            String status,
            LocalDateTime createdAt
    ) {
        public LostFoundItemDTO(LostFoundItem e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getReportedBy().getId(),
                    formatUser(e.getReportedBy()),
                    e.getClaimedBy() != null ? formatUser(e.getClaimedBy()) : null,
                    e.getType(),
                    e.getTitle(),
                    e.getDescription(),
                    e.getLocation(),
                    e.getContactInfo(),
                    e.getStatus(),
                    e.getCreatedAt()
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

    public record VehicleDTO(
            Long id,
            Long condominiumId,
            Long ownerId,
            String ownerName,
            String unitLabel,
            String type,
            String brandModel,
            String color,
            String licensePlate,
            String parkingSpot,
            String status,
            LocalDateTime createdAt
    ) {
        public VehicleDTO(Vehicle e) {
            this(
                    e.getId(),
                    e.getCondominium().getId(),
                    e.getOwner().getId(),
                    formatUser(e.getOwner()),
                    formatUnit(e.getUnit()),
                    e.getType(),
                    e.getBrandModel(),
                    e.getColor(),
                    e.getLicensePlate(),
                    e.getParkingSpot(),
                    e.getStatus(),
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
            String notes,
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
                    e.getNotes(),
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
            LocalDateTime checkedInAt
    ) {
        public GuestEntryDTO(GuestEntry e) {
            this(e.getId(), e.getGuestName(), e.getDocumentNumber(), e.getCheckedIn(), e.getCheckedInAt());
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
