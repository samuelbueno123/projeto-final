package com.cefet_rj.projeto_final.assemblies.services;

import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.AssemblyParticipantDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.AssemblyTopicDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CastVoteDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CreateAssemblyDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CreateTopicDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.UpdateAssemblyStatusDTO;
import com.cefet_rj.projeto_final.assemblies.entities.AssemblyParticipant;
import com.cefet_rj.projeto_final.assemblies.entities.AssemblyTopic;
import com.cefet_rj.projeto_final.assemblies.entities.AssemblyVote;
import com.cefet_rj.projeto_final.assemblies.entities.CondominiumAssembly;
import com.cefet_rj.projeto_final.assemblies.repositories.AssemblyParticipantRepository;
import com.cefet_rj.projeto_final.assemblies.repositories.AssemblyTopicRepository;
import com.cefet_rj.projeto_final.assemblies.repositories.AssemblyVoteRepository;
import com.cefet_rj.projeto_final.assemblies.repositories.CondominiumAssemblyRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.manager.entities.UnitPayment;
import com.cefet_rj.projeto_final.manager.enums.UnitPaymentStatus;
import com.cefet_rj.projeto_final.manager.repositories.UnitPaymentRepository;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssemblyService {

    private final CondominiumAssemblyRepository assemblyRepository;
    private final AssemblyTopicRepository topicRepository;
    private final AssemblyVoteRepository voteRepository;
    private final AssemblyParticipantRepository participantRepository;
    private final CondominiumRepository condominiumRepository;
    private final ResidentRepository residentRepository;
    private final UnitPaymentRepository paymentRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;
    private final RealtimeEmitterService realtimeEmitterService;

    @Transactional(readOnly = true)
    public List<AssemblyDTO> listAssemblies(Long condominiumId) {
        condominiumAccessService.validateAccess(condominiumId);
        User caller = authenticatedUserService.getAuthenticatedUser();

        List<CondominiumAssembly> assemblies = assemblyRepository.findAllByCondominiumIdWithTopics(condominiumId);

        List<Long> allTopicIds = assemblies.stream()
                .flatMap(a -> a.getTopics().stream().map(AssemblyTopic::getId))
                .toList();

        Map<Long, String> userVotesByTopicId = allTopicIds.isEmpty()
                ? Map.of()
                : voteRepository.findAllByTopicIdsAndUserId(allTopicIds, caller.getId()).stream()
                .collect(Collectors.toMap(v -> v.getTopic().getId(), AssemblyVote::getVoteChoice, (v1, v2) -> v1));

        return assemblies.stream().map(assembly -> buildAssemblyDTO(assembly, caller.getId(), userVotesByTopicId)).toList();
    }

    @Transactional(readOnly = true)
    public AssemblyDTO getAssembly(Long id) {
        CondominiumAssembly assembly = assemblyRepository.findByIdWithTopics(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assembleia não encontrada: " + id));
        condominiumAccessService.validateAccess(assembly.getCondominium().getId());
        User caller = authenticatedUserService.getAuthenticatedUser();

        List<Long> topicIds = assembly.getTopics().stream().map(AssemblyTopic::getId).toList();
        Map<Long, String> userVotes = topicIds.isEmpty() ? Map.of() :
                voteRepository.findAllByTopicIdsAndUserId(topicIds, caller.getId()).stream()
                        .collect(Collectors.toMap(v -> v.getTopic().getId(), AssemblyVote::getVoteChoice, (v1, v2) -> v1));

        return buildAssemblyDTO(assembly, caller.getId(), userVotes);
    }

    @Transactional
    public AssemblyDTO createAssembly(Long condominiumId, CreateAssemblyDTO dto) {
        condominiumAccessService.validateManagerAccess(condominiumId);
        User caller = authenticatedUserService.getAuthenticatedUser();

        if (dto.meetingDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data e horário da assembleia são obrigatórios.");
        }
        if (dto.meetingDate().isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível agendar uma assembleia em data passada.");
        }
        if (dto.secondCallDate() != null && dto.secondCallDate().isBefore(dto.meetingDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data da segunda chamada não pode ser anterior à primeira chamada.");
        }

        Condominium condo = condominiumRepository.findById(condominiumId)
                .orElseThrow(() -> new ResourceNotFoundException("Condomínio não encontrado: " + condominiumId));

        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setCondominium(condo);
        assembly.setTitle(dto.title().trim());
        assembly.setDescription(dto.description() != null ? dto.description().trim() : null);
        assembly.setType(dto.type() != null ? dto.type().trim().toUpperCase() : "ORDINARY");
        assembly.setMeetingDate(dto.meetingDate());
        assembly.setSecondCallDate(dto.secondCallDate());
        assembly.setLocationOrLink(dto.locationOrLink() != null ? dto.locationOrLink().trim() : null);
        assembly.setCallNoticeUrl(dto.callNoticeUrl() != null ? dto.callNoticeUrl().trim() : null);
        assembly.setBlockDelinquentUnits(dto.blockDelinquentUnits() == null || dto.blockDelinquentUnits());
        assembly.setOneVotePerUnit(dto.oneVotePerUnit() == null || dto.oneVotePerUnit());
        assembly.setQuorumMinimumPercent(dto.quorumMinimumPercent() != null ? dto.quorumMinimumPercent() : 50);
        assembly.setStatus("SCHEDULED");
        assembly.setCreatedBy(caller);
        assembly.setCreatedAt(LocalDateTime.now());

        assembly = assemblyRepository.save(assembly);
        AssemblyDTO result = new AssemblyDTO(assembly);

        realtimeEmitterService.sendToCondominium(condominiumId,
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", condominiumId, result));

        return result;
    }

    @Transactional
    public AssemblyTopicDTO addTopic(Long assemblyId, CreateTopicDTO dto) {
        CondominiumAssembly assembly = assemblyRepository.findById(assemblyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assembleia não encontrada: " + assemblyId));
        condominiumAccessService.validateManagerAccess(assembly.getCondominium().getId());

        if ("CONCLUDED".equalsIgnoreCase(assembly.getStatus()) || "CANCELLED".equalsIgnoreCase(assembly.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível adicionar pautas a uma assembleia encerrada ou cancelada.");
        }

        AssemblyTopic topic = new AssemblyTopic();
        topic.setAssembly(assembly);
        topic.setTitle(dto.title().trim());
        topic.setDescription(dto.description() != null ? dto.description().trim() : null);
        topic.setDisplayOrder(dto.displayOrder() != null ? dto.displayOrder() : (assembly.getTopics().size() + 1));
        topic.setVotingOpen(false);
        topic.setResultStatus("PENDING");

        topic = topicRepository.save(topic);
        AssemblyTopicDTO result = new AssemblyTopicDTO(topic);

        realtimeEmitterService.sendToCondominium(assembly.getCondominium().getId(),
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", assembly.getCondominium().getId(), result));

        return result;
    }

    @Transactional
    public AssemblyTopicDTO toggleTopicVoting(Long topicId, boolean open) {
        AssemblyTopic topic = topicRepository.findByIdWithAssembly(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pauta não encontrada: " + topicId));
        Long condoId = topic.getAssembly().getCondominium().getId();
        condominiumAccessService.validateManagerAccess(condoId);

        if ("CONCLUDED".equalsIgnoreCase(topic.getAssembly().getStatus()) || "CANCELLED".equalsIgnoreCase(topic.getAssembly().getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível alterar a votação de uma assembleia encerrada ou cancelada.");
        }

        topic.setVotingOpen(open);
        if (!open) {
            topic.setResultStatus(computeTopicResult(topic));
        } else {
            topic.setResultStatus("OPEN");
        }
        topic = topicRepository.save(topic);
        AssemblyTopicDTO result = new AssemblyTopicDTO(topic);

        realtimeEmitterService.sendToCondominium(condoId,
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", condoId, result));

        return result;
    }

    @Transactional
    public AssemblyParticipantDTO checkInParticipant(Long assemblyId, String proxyInfo) {
        CondominiumAssembly assembly = assemblyRepository.findById(assemblyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assembleia não encontrada: " + assemblyId));
        Long condoId = assembly.getCondominium().getId();
        condominiumAccessService.validateAccess(condoId);

        if ("CONCLUDED".equalsIgnoreCase(assembly.getStatus()) || "CANCELLED".equalsIgnoreCase(assembly.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é possível registrar presença em uma assembleia encerrada ou cancelada.");
        }

        User caller = authenticatedUserService.getAuthenticatedUser();
        Optional<Resident> residentOpt = residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(caller.getId(), condoId);
        if (residentOpt.isEmpty() && !condominiumAccessService.isManagerOrAdmin(caller, condoId)) {
            throw new ForbiddenException("Apenas condôminos vinculados a uma unidade ou a administração podem registrar presença na assembleia.");
        }

        AssemblyParticipant participant = participantRepository.findByAssemblyIdAndUserId(assemblyId, caller.getId())
                .orElseGet(AssemblyParticipant::new);
        participant.setAssembly(assembly);
        participant.setUser(caller);
        if (residentOpt.isPresent()) {
            participant.setUnit(residentOpt.get().getUnit());
        }
        if (proxyInfo != null && !proxyInfo.isBlank()) {
            participant.setProxyInfo(proxyInfo.trim());
        }
        participant.setCheckedInAt(LocalDateTime.now());
        participant = participantRepository.save(participant);

        return new AssemblyParticipantDTO(participant);
    }

    @Transactional(readOnly = true)
    public List<AssemblyParticipantDTO> listParticipants(Long assemblyId) {
        CondominiumAssembly assembly = assemblyRepository.findById(assemblyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assembleia não encontrada: " + assemblyId));
        condominiumAccessService.validateAccess(assembly.getCondominium().getId());
        return participantRepository.findByAssemblyIdOrderByCheckedInAtAsc(assemblyId).stream()
                .map(AssemblyParticipantDTO::new)
                .toList();
    }

    @Transactional
    public AssemblyTopicDTO castVote(Long topicId, CastVoteDTO dto) {
        User caller = authenticatedUserService.getAuthenticatedUser();

        AssemblyTopic topic = topicRepository.findByIdWithAssembly(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pauta não encontrada: " + topicId));

        CondominiumAssembly assembly = topic.getAssembly();
        Long condoId = assembly.getCondominium().getId();
        condominiumAccessService.validateAccess(condoId);

        if (!topic.isVotingOpen() || "CONCLUDED".equalsIgnoreCase(assembly.getStatus()) || "CANCELLED".equalsIgnoreCase(assembly.getStatus())) {
            throw new ForbiddenException("A votação para esta pauta não está aberta no momento.");
        }

        Optional<Resident> residentOpt = residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(caller.getId(), condoId);
        if (residentOpt.isEmpty() && !condominiumAccessService.isManagerOrAdmin(caller, condoId)) {
            throw new ForbiddenException("Apenas moradores vinculados a uma unidade podem votar nas pautas da assembleia.");
        }

        Unit unit = residentOpt.map(Resident::getUnit).orElse(null);

        if (voteRepository.findByTopicIdAndUserId(topicId, caller.getId()).isPresent()) {
            throw new ForbiddenException("Você já registrou seu voto nesta pauta.");
        }

        if (assembly.isOneVotePerUnit() && unit != null) {
            if (voteRepository.findByTopicIdAndUnitId(topicId, unit.getId()).isPresent()) {
                throw new ForbiddenException("Sua unidade já registrou voto nesta pauta (regra de 1 voto por unidade).");
            }
        }

        if (assembly.isBlockDelinquentUnits() && unit != null) {
            Optional<UnitPayment> paymentOpt = paymentRepository.findByUnitId(unit.getId());
            if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == UnitPaymentStatus.OVERDUE) {
                throw new ForbiddenException("Unidades com pendência financeira em atraso não estão elegíveis para votar nesta assembleia (Art. 1.335 III CC).");
            }
        }

        String choice = dto.voteChoice().trim().toUpperCase();
        if (!List.of("IN_FAVOR", "AGAINST", "ABSTAIN").contains(choice)) {
            throw new IllegalArgumentException("Opção de voto inválida: " + choice + ". Use IN_FAVOR, AGAINST ou ABSTAIN.");
        }

        // Automatically record presence if not yet checked in
        if (participantRepository.findByAssemblyIdAndUserId(assembly.getId(), caller.getId()).isEmpty()) {
            AssemblyParticipant part = new AssemblyParticipant();
            part.setAssembly(assembly);
            part.setUser(caller);
            part.setUnit(unit);
            part.setCheckedInAt(LocalDateTime.now());
            participantRepository.save(part);
        }

        AssemblyVote vote = new AssemblyVote();
        vote.setTopic(topic);
        vote.setUser(caller);
        vote.setUnit(unit);
        vote.setVoteChoice(choice);
        vote.setVotedAt(LocalDateTime.now());
        voteRepository.save(vote);

        switch (choice) {
            case "IN_FAVOR" -> topic.setInFavorCount(topic.getInFavorCount() + 1);
            case "AGAINST" -> topic.setAgainstCount(topic.getAgainstCount() + 1);
            case "ABSTAIN" -> topic.setAbstainCount(topic.getAbstainCount() + 1);
        }
        topic = topicRepository.save(topic);

        AssemblyTopicDTO result = new AssemblyTopicDTO(topic).withUserVote(choice);

        realtimeEmitterService.sendToCondominium(condoId,
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", condoId, result));

        return result;
    }

    @Transactional
    public AssemblyDTO updateAssemblyStatus(Long id, UpdateAssemblyStatusDTO dto) {
        CondominiumAssembly assembly = assemblyRepository.findByIdWithTopics(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assembleia não encontrada: " + id));
        Long condoId = assembly.getCondominium().getId();
        condominiumAccessService.validateManagerAccess(condoId);

        String currentStatus = assembly.getStatus() != null ? assembly.getStatus().toUpperCase() : "SCHEDULED";
        String newStatus = dto.status().trim().toUpperCase();

        if (!List.of("SCHEDULED", "IN_PROGRESS", "CONCLUDED", "CANCELLED").contains(newStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de assembleia inválido: " + newStatus);
        }

        if ("CONCLUDED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            if (!currentStatus.equals(newStatus)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Uma assembleia já concluída ou cancelada não pode ser reaberta.");
            }
        }

        assembly.setStatus(newStatus);
        if (dto.minutesUrl() != null && !dto.minutesUrl().isBlank()) {
            assembly.setMinutesUrl(dto.minutesUrl().trim());
        }
        if ("CONCLUDED".equals(newStatus)) {
            for (AssemblyTopic t : assembly.getTopics()) {
                t.setVotingOpen(false);
                t.setResultStatus(computeTopicResult(t));
            }
        }

        assembly = assemblyRepository.save(assembly);
        User caller = authenticatedUserService.getAuthenticatedUser();
        AssemblyDTO result = buildAssemblyDTO(assembly, caller.getId(), Map.of());

        realtimeEmitterService.sendToCondominium(condoId,
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", condoId, result));

        return result;
    }

    private AssemblyDTO buildAssemblyDTO(CondominiumAssembly assembly, Long viewerId, Map<Long, String> userVotes) {
        List<AssemblyTopicDTO> enrichedTopics = assembly.getTopics().stream().map(t -> {
            String vote = userVotes.get(t.getId());
            return new AssemblyTopicDTO(t).withUserVote(vote);
        }).toList();

        List<AssemblyParticipant> partEntities = participantRepository.findByAssemblyIdOrderByCheckedInAtAsc(assembly.getId());
        boolean checkedIn = partEntities.stream().anyMatch(p -> p.getUser().getId().equals(viewerId));
        List<AssemblyParticipantDTO> partDtos = partEntities.stream().map(AssemblyParticipantDTO::new).toList();

        return new AssemblyDTO(
                assembly.getId(),
                assembly.getCondominium().getId(),
                assembly.getTitle(),
                assembly.getDescription(),
                assembly.getType(),
                assembly.getMeetingDate(),
                assembly.getSecondCallDate(),
                assembly.getLocationOrLink(),
                assembly.getCallNoticeUrl(),
                assembly.getMinutesUrl(),
                assembly.getStatus(),
                assembly.isBlockDelinquentUnits(),
                assembly.isOneVotePerUnit(),
                assembly.getQuorumMinimumPercent(),
                partDtos.size(),
                checkedIn,
                assembly.getCreatedBy() != null
                        ? (assembly.getCreatedBy().getFirstName() + " " + assembly.getCreatedBy().getLastName()).trim()
                        : "Administração",
                assembly.getCreatedAt(),
                enrichedTopics,
                partDtos
        );
    }

    private String computeTopicResult(AssemblyTopic topic) {
        int total = topic.getInFavorCount() + topic.getAgainstCount() + topic.getAbstainCount();
        if (total == 0) return "NO_VOTES";
        if (topic.getInFavorCount() > topic.getAgainstCount()) return "APPROVED";
        if (topic.getAgainstCount() > topic.getInFavorCount()) return "REJECTED";
        return "TIED";
    }
}
