package com.cefet_rj.projeto_final.assemblies.services;

import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.AssemblyTopicDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CastVoteDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CreateAssemblyDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CreateTopicDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.UpdateAssemblyStatusDTO;
import com.cefet_rj.projeto_final.assemblies.entities.AssemblyTopic;
import com.cefet_rj.projeto_final.assemblies.entities.AssemblyVote;
import com.cefet_rj.projeto_final.assemblies.entities.CondominiumAssembly;
import com.cefet_rj.projeto_final.assemblies.repositories.AssemblyTopicRepository;
import com.cefet_rj.projeto_final.assemblies.repositories.AssemblyVoteRepository;
import com.cefet_rj.projeto_final.assemblies.repositories.CondominiumAssemblyRepository;
import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CondominiumRepository condominiumRepository;
    private final ResidentRepository residentRepository;
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

        return assemblies.stream().map(assembly -> {
            List<AssemblyTopicDTO> enrichedTopics = assembly.getTopics().stream().map(t -> {
                String vote = userVotesByTopicId.get(t.getId());
                return new AssemblyTopicDTO(t).withUserVote(vote);
            }).toList();

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
                    assembly.getCreatedBy() != null
                            ? (assembly.getCreatedBy().getFirstName() + " " + assembly.getCreatedBy().getLastName()).trim()
                            : "Administração",
                    assembly.getCreatedAt(),
                    enrichedTopics
            );
        }).toList();
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

        List<AssemblyTopicDTO> enrichedTopics = assembly.getTopics().stream().map(t -> {
            String vote = userVotes.get(t.getId());
            return new AssemblyTopicDTO(t).withUserVote(vote);
        }).toList();

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
                assembly.getCreatedBy() != null
                        ? (assembly.getCreatedBy().getFirstName() + " " + assembly.getCreatedBy().getLastName()).trim()
                        : "Administração",
                assembly.getCreatedAt(),
                enrichedTopics
        );
    }

    @Transactional
    public AssemblyDTO createAssembly(Long condominiumId, CreateAssemblyDTO dto) {
        User caller = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_MANAGER");
        if (!isStaff) {
            throw new ForbiddenException("Apenas o síndico ou administrador pode convocar assembleias.");
        }
        condominiumAccessService.validateAccess(condominiumId);

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
        User caller = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_MANAGER");
        if (!isStaff) {
            throw new ForbiddenException("Apenas o síndico ou administrador pode adicionar pautas.");
        }

        CondominiumAssembly assembly = assemblyRepository.findById(assemblyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assembleia não encontrada: " + assemblyId));

        AssemblyTopic topic = new AssemblyTopic();
        topic.setAssembly(assembly);
        topic.setTitle(dto.title().trim());
        topic.setDescription(dto.description() != null ? dto.description().trim() : null);
        topic.setDisplayOrder(dto.displayOrder() != null ? dto.displayOrder() : (assembly.getTopics().size() + 1));
        topic.setVotingOpen(false);

        topic = topicRepository.save(topic);
        AssemblyTopicDTO result = new AssemblyTopicDTO(topic);

        realtimeEmitterService.sendToCondominium(assembly.getCondominium().getId(),
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", assembly.getCondominium().getId(), result));

        return result;
    }

    @Transactional
    public AssemblyTopicDTO toggleTopicVoting(Long topicId, boolean open) {
        User caller = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_MANAGER");
        if (!isStaff) {
            throw new ForbiddenException("Apenas o síndico ou administrador pode alterar o estado da votação.");
        }

        AssemblyTopic topic = topicRepository.findByIdWithAssembly(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pauta não encontrada: " + topicId));

        topic.setVotingOpen(open);
        topic = topicRepository.save(topic);
        AssemblyTopicDTO result = new AssemblyTopicDTO(topic);

        realtimeEmitterService.sendToCondominium(topic.getAssembly().getCondominium().getId(),
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", topic.getAssembly().getCondominium().getId(), result));

        return result;
    }

    @Transactional
    public AssemblyTopicDTO castVote(Long topicId, CastVoteDTO dto) {
        User caller = authenticatedUserService.getAuthenticatedUser();

        AssemblyTopic topic = topicRepository.findByIdWithAssembly(topicId)
                .orElseThrow(() -> new ResourceNotFoundException("Pauta não encontrada: " + topicId));

        Long condoId = topic.getAssembly().getCondominium().getId();
        condominiumAccessService.validateAccess(condoId);

        if (!topic.isVotingOpen()) {
            throw new ForbiddenException("A votação para esta pauta não está aberta no momento.");
        }

        if (voteRepository.findByTopicIdAndUserId(topicId, caller.getId()).isPresent()) {
            throw new ForbiddenException("Você já registrou seu voto nesta pauta.");
        }

        String choice = dto.voteChoice().trim().toUpperCase();
        if (!List.of("IN_FAVOR", "AGAINST", "ABSTAIN").contains(choice)) {
            throw new IllegalArgumentException("Opção de voto inválida: " + choice + ". Use IN_FAVOR, AGAINST ou ABSTAIN.");
        }

        Optional<Resident> residentOpt = residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(caller.getId(), condoId);

        AssemblyVote vote = new AssemblyVote();
        vote.setTopic(topic);
        vote.setUser(caller);
        residentOpt.ifPresent(r -> vote.setUnit(r.getUnit()));
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
        User caller = authenticatedUserService.getAuthenticatedUser();
        boolean isStaff = caller.hasRole("ROLE_ADMIN") || caller.hasRole("ROLE_MANAGER");
        if (!isStaff) {
            throw new ForbiddenException("Apenas o síndico ou administrador pode atualizar a assembleia.");
        }

        CondominiumAssembly assembly = assemblyRepository.findByIdWithTopics(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assembleia não encontrada: " + id));

        assembly.setStatus(dto.status().trim().toUpperCase());
        if (dto.minutesUrl() != null && !dto.minutesUrl().isBlank()) {
            assembly.setMinutesUrl(dto.minutesUrl().trim());
        }

        assembly = assemblyRepository.save(assembly);
        AssemblyDTO result = new AssemblyDTO(assembly);

        realtimeEmitterService.sendToCondominium(assembly.getCondominium().getId(),
                RealtimeEventDTO.of("ASSEMBLY_UPDATED", assembly.getCondominium().getId(), result));

        return result;
    }
}
