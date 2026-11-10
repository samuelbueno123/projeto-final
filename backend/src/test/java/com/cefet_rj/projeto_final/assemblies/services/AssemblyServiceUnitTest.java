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
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
import com.cefet_rj.projeto_final.shared.exceptions.ForbiddenException;
import com.cefet_rj.projeto_final.users.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssemblyServiceUnitTest {

    @Mock
    private CondominiumAssemblyRepository assemblyRepository;

    @Mock
    private AssemblyTopicRepository topicRepository;

    @Mock
    private AssemblyVoteRepository voteRepository;

    @Mock
    private AssemblyParticipantRepository participantRepository;

    @Mock
    private CondominiumRepository condominiumRepository;

    @Mock
    private ResidentRepository residentRepository;

    @Mock
    private UnitPaymentRepository paymentRepository;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private CondominiumAccessService condominiumAccessService;

    @Mock
    private RealtimeEmitterService realtimeEmitterService;

    @InjectMocks
    private AssemblyService assemblyService;

    private Condominium condo;
    private User managerUser;
    private User residentUser;
    private Unit testUnit;
    private Resident testResident;

    @BeforeEach
    void setUp() {
        condo = new Condominium();
        condo.setId(1L);
        condo.setName("Residencial Solar");

        managerUser = new User();
        managerUser.setId(10L);
        managerUser.setFirstName("Roberto");
        managerUser.setLastName("Síndico");
        managerUser.setEmail("sindico@solar.com");

        residentUser = new User();
        residentUser.setId(20L);
        residentUser.setFirstName("Ana");
        residentUser.setLastName("Moradora");
        residentUser.setEmail("ana@solar.com");

        testUnit = new Unit();
        testUnit.setId(101L);
        testUnit.setIdentifier("101");

        testResident = new Resident();
        testResident.setUser(residentUser);
        testResident.setUnit(testUnit);
    }

    @Test
    @DisplayName("createAssembly: throws BadRequest when meeting date is null")
    void createAssembly_throwsBadRequest_whenDateNull() {
        CreateAssemblyDTO dto = new CreateAssemblyDTO("AGO 2026", "Prestação", "ORDINARY", null, null, null, null, true, true, 50);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assemblyService.createAssembly(condo.getId(), dto));
        assertTrue(ex.getReason().contains("obrigatórios"));
    }

    @Test
    @DisplayName("createAssembly: throws BadRequest when meeting date is in the past")
    void createAssembly_throwsBadRequest_whenDateInPast() {
        LocalDateTime past = LocalDateTime.now().minusDays(1);
        CreateAssemblyDTO dto = new CreateAssemblyDTO("AGO 2026", "Prestação", "ORDINARY", past, null, null, null, true, true, 50);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assemblyService.createAssembly(condo.getId(), dto));
        assertTrue(ex.getReason().contains("passada"));
    }

    @Test
    @DisplayName("createAssembly: throws BadRequest when second call date is before meeting date")
    void createAssembly_throwsBadRequest_whenSecondCallBeforeMeeting() {
        LocalDateTime meeting = LocalDateTime.now().plusDays(10);
        LocalDateTime secondCall = meeting.minusHours(1);
        CreateAssemblyDTO dto = new CreateAssemblyDTO("AGO 2026", "Prestação", "ORDINARY", meeting, secondCall, null, null, true, true, 50);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assemblyService.createAssembly(condo.getId(), dto));
        assertTrue(ex.getReason().contains("segunda chamada"));
    }

    @Test
    @DisplayName("createAssembly: successfully creates and persists assembly")
    void createAssembly_success() {
        LocalDateTime meeting = LocalDateTime.now().plusDays(10);
        LocalDateTime secondCall = meeting.plusMinutes(30);
        CreateAssemblyDTO dto = new CreateAssemblyDTO("AGO 2026", "Prestação de Contas", "ORDINARY", meeting, secondCall, "Salão", null, true, true, 50);

        when(condominiumRepository.findById(condo.getId())).thenReturn(Optional.of(condo));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(managerUser);
        when(assemblyRepository.save(any(CondominiumAssembly.class))).thenAnswer(inv -> {
            CondominiumAssembly a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        AssemblyDTO result = assemblyService.createAssembly(condo.getId(), dto);

        assertNotNull(result);
        assertEquals(99L, result.id());
        assertEquals("SCHEDULED", result.status());
        verify(assemblyRepository).save(any(CondominiumAssembly.class));
        verify(realtimeEmitterService).sendToCondominium(eq(condo.getId()), any());
    }

    @Test
    @DisplayName("addTopic: throws Conflict if assembly is CONCLUDED or CANCELLED")
    void addTopic_throwsConflict_whenConcludedOrCancelled() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("CONCLUDED");

        when(assemblyRepository.findById(99L)).thenReturn(Optional.of(assembly));

        CreateTopicDTO dto = new CreateTopicDTO("Pauta 1", "Descrição", 1);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> assemblyService.addTopic(99L, dto));
        assertTrue(ex.getReason().contains("encerrada ou cancelada"));
    }

    @Test
    @DisplayName("toggleTopicVoting: correctly closes voting and computes APPROVED when in favor dominates")
    void toggleTopicVoting_computesTopicResult_whenVotingClosed() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("IN_PROGRESS");

        AssemblyTopic topic = new AssemblyTopic();
        topic.setId(10L);
        topic.setAssembly(assembly);
        topic.setVotingOpen(true);
        topic.setInFavorCount(15);
        topic.setAgainstCount(5);
        topic.setAbstainCount(2);

        when(topicRepository.findByIdWithAssembly(10L)).thenReturn(Optional.of(topic));
        when(topicRepository.save(any(AssemblyTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        AssemblyTopicDTO dto = assemblyService.toggleTopicVoting(10L, false);

        assertFalse(dto.votingOpen());
        assertEquals("APPROVED", dto.resultStatus());
        verify(topicRepository).save(topic);
    }

    @Test
    @DisplayName("castVote: throws Forbidden when voting is not open")
    void castVote_throwsForbidden_whenVotingNotOpen() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("IN_PROGRESS");

        AssemblyTopic topic = new AssemblyTopic();
        topic.setId(10L);
        topic.setAssembly(assembly);
        topic.setVotingOpen(false);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
        when(topicRepository.findByIdWithAssembly(10L)).thenReturn(Optional.of(topic));

        CastVoteDTO dto = new CastVoteDTO("IN_FAVOR");
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> assemblyService.castVote(10L, dto));
        assertTrue(ex.getMessage().contains("não está aberta"));
    }

    @Test
    @DisplayName("castVote: throws Forbidden when user already voted")
    void castVote_throwsForbidden_whenUserAlreadyVoted() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("IN_PROGRESS");

        AssemblyTopic topic = new AssemblyTopic();
        topic.setId(10L);
        topic.setAssembly(assembly);
        topic.setVotingOpen(true);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
        when(topicRepository.findByIdWithAssembly(10L)).thenReturn(Optional.of(topic));
        when(residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(residentUser.getId(), condo.getId()))
                .thenReturn(Optional.of(testResident));
        when(voteRepository.findByTopicIdAndUserId(10L, residentUser.getId()))
                .thenReturn(Optional.of(new AssemblyVote()));

        CastVoteDTO dto = new CastVoteDTO("IN_FAVOR");
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> assemblyService.castVote(10L, dto));
        assertTrue(ex.getMessage().contains("já registrou seu voto"));
    }

    @Test
    @DisplayName("castVote: throws Forbidden when unit already voted and assembly enforces 1 vote per unit")
    void castVote_throwsForbidden_whenUnitAlreadyVoted() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("IN_PROGRESS");
        assembly.setOneVotePerUnit(true);

        AssemblyTopic topic = new AssemblyTopic();
        topic.setId(10L);
        topic.setAssembly(assembly);
        topic.setVotingOpen(true);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
        when(topicRepository.findByIdWithAssembly(10L)).thenReturn(Optional.of(topic));
        when(residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(residentUser.getId(), condo.getId()))
                .thenReturn(Optional.of(testResident));
        when(voteRepository.findByTopicIdAndUserId(10L, residentUser.getId()))
                .thenReturn(Optional.empty());
        when(voteRepository.findByTopicIdAndUnitId(10L, testUnit.getId()))
                .thenReturn(Optional.of(new AssemblyVote()));

        CastVoteDTO dto = new CastVoteDTO("IN_FAVOR");
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> assemblyService.castVote(10L, dto));
        assertTrue(ex.getMessage().contains("regra de 1 voto por unidade"));
    }

    @Test
    @DisplayName("castVote: throws Forbidden when unit is delinquent and assembly blocks delinquent units")
    void castVote_throwsForbidden_whenDelinquent() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("IN_PROGRESS");
        assembly.setBlockDelinquentUnits(true);
        assembly.setOneVotePerUnit(false);

        AssemblyTopic topic = new AssemblyTopic();
        topic.setId(10L);
        topic.setAssembly(assembly);
        topic.setVotingOpen(true);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
        when(topicRepository.findByIdWithAssembly(10L)).thenReturn(Optional.of(topic));
        when(residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(residentUser.getId(), condo.getId()))
                .thenReturn(Optional.of(testResident));
        when(voteRepository.findByTopicIdAndUserId(10L, residentUser.getId()))
                .thenReturn(Optional.empty());

        UnitPayment overduePayment = new UnitPayment();
        overduePayment.setStatus(UnitPaymentStatus.OVERDUE);
        when(paymentRepository.findByUnitId(testUnit.getId()))
                .thenReturn(Optional.of(overduePayment));

        CastVoteDTO dto = new CastVoteDTO("IN_FAVOR");
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> assemblyService.castVote(10L, dto));
        assertTrue(ex.getMessage().contains("pendência financeira"));
    }

    @Test
    @DisplayName("castVote: successfully registers vote, updates counts and check-in")
    void castVote_success() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("IN_PROGRESS");
        assembly.setBlockDelinquentUnits(false);
        assembly.setOneVotePerUnit(false);

        AssemblyTopic topic = new AssemblyTopic();
        topic.setId(10L);
        topic.setAssembly(assembly);
        topic.setVotingOpen(true);
        topic.setInFavorCount(0);
        topic.setAgainstCount(0);
        topic.setAbstainCount(0);

        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(residentUser);
        when(topicRepository.findByIdWithAssembly(10L)).thenReturn(Optional.of(topic));
        when(residentRepository.findFirstByUserIdAndUnitSectorCondominiumId(residentUser.getId(), condo.getId()))
                .thenReturn(Optional.of(testResident));
        when(voteRepository.findByTopicIdAndUserId(10L, residentUser.getId()))
                .thenReturn(Optional.empty());
        when(participantRepository.findByAssemblyIdAndUserId(99L, residentUser.getId()))
                .thenReturn(Optional.empty());
        when(topicRepository.save(any(AssemblyTopic.class))).thenAnswer(inv -> inv.getArgument(0));

        CastVoteDTO dto = new CastVoteDTO("IN_FAVOR");
        AssemblyTopicDTO result = assemblyService.castVote(10L, dto);

        assertEquals(1, result.inFavorCount());
        assertEquals("IN_FAVOR", result.userVote());
        verify(voteRepository).save(any(AssemblyVote.class));
        verify(participantRepository).save(any());
    }

    @Test
    @DisplayName("updateAssemblyStatus: concluding assembly closes all open topic votings")
    void updateAssemblyStatus_concludeClosesAllTopics() {
        CondominiumAssembly assembly = new CondominiumAssembly();
        assembly.setId(99L);
        assembly.setCondominium(condo);
        assembly.setStatus("IN_PROGRESS");

        AssemblyTopic topic1 = new AssemblyTopic();
        topic1.setId(10L);
        topic1.setAssembly(assembly);
        topic1.setVotingOpen(true);
        topic1.setInFavorCount(10);
        topic1.setAgainstCount(2);

        AssemblyTopic topic2 = new AssemblyTopic();
        topic2.setId(11L);
        topic2.setAssembly(assembly);
        topic2.setVotingOpen(true);
        topic2.setInFavorCount(3);
        topic2.setAgainstCount(8);

        assembly.setTopics(new ArrayList<>(List.of(topic1, topic2)));

        when(assemblyRepository.findByIdWithTopics(99L)).thenReturn(Optional.of(assembly));
        when(assemblyRepository.save(any(CondominiumAssembly.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authenticatedUserService.getAuthenticatedUser()).thenReturn(managerUser);

        UpdateAssemblyStatusDTO dto = new UpdateAssemblyStatusDTO("CONCLUDED", "https://docs.condo.com/minutes.pdf");
        AssemblyDTO result = assemblyService.updateAssemblyStatus(99L, dto);

        assertEquals("CONCLUDED", result.status());
        assertEquals("https://docs.condo.com/minutes.pdf", result.minutesUrl());
        assertFalse(topic1.isVotingOpen());
        assertEquals("APPROVED", topic1.getResultStatus());
        assertFalse(topic2.isVotingOpen());
        assertEquals("REJECTED", topic2.getResultStatus());
    }
}
