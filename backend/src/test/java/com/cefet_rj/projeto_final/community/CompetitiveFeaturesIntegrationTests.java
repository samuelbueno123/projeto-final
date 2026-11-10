package com.cefet_rj.projeto_final.community;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.community.entities.*;
import com.cefet_rj.projeto_final.community.repositories.*;
import com.cefet_rj.projeto_final.condominiums.entities.*;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.enums.ResidentType;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.*;
import com.cefet_rj.projeto_final.financial.repositories.BudgetForecastRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.MaintenanceChecklistRepository;
import com.cefet_rj.projeto_final.maintenance.repositories.ServiceContractRepository;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        properties = {
                "security.client-id=test-client",
                "security.client-secret=test-secret"
        }
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CompetitiveFeaturesIntegrationTests {

    private static final String DEFAULT_PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private CondominiumRepository condominiumRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private ResidentRepository residentRepository;

    @Autowired
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VisitorAccessRepository visitorAccessRepository;

    private Condominium condo;
    private Sector sectorA;
    private Unit unit101;
    private User managerUser;
    private User residentUser;
    private User doormanUser;

    @BeforeEach
    void setUp() {
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");
        Role doormanRole = getOrCreateRole("ROLE_DOORMAN");

        BillingPlan plan = new BillingPlan();
        plan.setName("COMPETITIVE_PRO_PLAN");
        plan.setMonthlyFee(new BigDecimal("499.00"));
        plan.setActive(true);
        plan = billingPlanRepository.save(plan);

        condo = new Condominium();
        condo.setName("Residencial Grand Horizon");
        condo.setAddress("Avenida das Américas, 3000");
        condo.setBillingPlan(plan);
        condo.setIsActive(true);
        condo = condominiumRepository.save(condo);

        sectorA = new Sector();
        sectorA.setName("Torre Alpha");
        sectorA.setStructuralType(SectorStructuralType.BLOCK);
        sectorA.setCondominium(condo);
        sectorA = sectorRepository.save(sectorA);

        unit101 = new Unit();
        unit101.setIdentifier("101");
        unit101.setSector(sectorA);
        unit101 = unitRepository.save(unit101);

        managerUser = createUser("Marcos", "Síndico", "sindico.comp@condo.com", managerRole);
        residentUser = createUser("Juliana", "Moradora", "moradora.comp@condo.com", residentRole);
        doormanUser = createUser("José", "Porteiro", "porteiro.comp@condo.com", doormanRole);

        createMembership(managerUser, managerRole, null);
        createMembership(residentUser, residentRole, unit101);
        createMembership(doormanUser, doormanRole, null);

        Resident resident = new Resident();
        resident.setUser(residentUser);
        resident.setUnit(unit101);
        resident.setType(ResidentType.OWNER);
        residentRepository.save(resident);
    }

    // =========================================================================
    // 1. OBRAS E REFORMAS (NBR 16.280)
    // =========================================================================
    @Test
    @DisplayName("Feature 1: Criação e aprovação de solicitação de obra NBR 16.280")
    void testRenovationRequestLifecycle() throws Exception {
        String residentToken = login(residentUser.getEmail());

        // 1. Morador submete solicitação de reforma
        String resContent = mockMvc.perform(post("/condominiums/" + condo.getId() + "/renovations")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "unitId", unit101.getId(),
                                "title", "Reforma de Piso e Pintura",
                                "description", "Troca do piso por porcelanato e pintura geral",
                                "artRrtNumber", "ART-2026-998811",
                                "noiseLevel", "MEDIUM",
                                "contractorName", "Engenharia Total Ltda",
                                "contractorPhone", "(11) 98888-7777",
                                "startDate", LocalDate.now().plusDays(2).toString(),
                                "expectedEndDate", LocalDate.now().plusDays(15).toString()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode createdNode = objectMapper.readTree(resContent);
        Long renovationId = createdNode.get("id").asLong();
        assertEquals("REQUESTED", createdNode.get("status").asText());
        assertEquals("ART-2026-998811", createdNode.get("artRrtNumber").asText());

        // 2. Síndico aprova a reforma
        String managerToken = login(managerUser.getEmail());
        mockMvc.perform(patch("/renovations/" + renovationId + "/approve")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("notes", "Aprovado com base na ART apresentada"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        // 3. Consulta de lista de reformas do condomínio
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/renovations")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Reforma de Piso e Pintura"));
    }

    // =========================================================================
    // 2. LIVRO DIGITAL DE PASSAGEM DE TURNO DA PORTARIA
    // =========================================================================
    @Test
    @DisplayName("Feature 2: Registro de passagem de turno da portaria e ciência do porteiro sucessor")
    void testConciergeShiftLogFlow() throws Exception {
        String doormanToken = login(doormanUser.getEmail());

        // Porteiro registra o fechamento de seu turno
        String response = mockMvc.perform(post("/condominiums/" + condo.getId() + "/shift-logs")
                        .header("Authorization", "Bearer " + doormanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "shiftType", "DAY",
                                "occurrencesSummary", "Portão de pedestres com pequena lentidão na mola hidráulica. 3 encomendas pendentes na sala de entregas.",
                                "generalNotes", "Todas as chaves no chaveiro mestre conferidas.",
                                "gateStatus", "ALL_NORMAL",
                                "pendingKeysCount", 0,
                                "pendingPackagesCount", 3
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        Long logId = jsonNode.get("id").asLong();
        assertTrue(jsonNode.get("acknowledgedAt").isNull());

        // Porteiro sucessor ou síndico toma ciência formal
        mockMvc.perform(patch("/shift-logs/" + logId + "/acknowledge")
                        .header("Authorization", "Bearer " + doormanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acknowledgedAt").isNotEmpty());
    }

    // =========================================================================
    // 3. CONTROLE DE CHAVES E TAGS DA PORTARIA
    // =========================================================================
    @Test
    @DisplayName("Feature 3: Retirada e devolução de chaves da portaria com rastreabilidade")
    void testKeyRegistryCheckoutAndReturn() throws Exception {
        String doormanToken = login(doormanUser.getEmail());

        // 1. Cadastra chave no acervo
        String keyResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/keys")
                        .header("Authorization", "Bearer " + doormanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "keyCode", "C-CASA-MAQ-01",
                                "description", "Chave da Casa de Máquinas dos Elevadores",
                                "keyCategory", "COMMON_AREA"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.currentStatus").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();

        Long keyId = objectMapper.readTree(keyResp).get("id").asLong();

        // 2. Empréstimo de chave para técnico de elevadores
        mockMvc.perform(post("/keys/" + keyId + "/checkout")
                        .header("Authorization", "Bearer " + doormanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "borrowerName", "Otis Elevadores - Roberto",
                                "borrowerDocument", "MG-12.345.678",
                                "borrowerPhone", "(21) 97777-6666"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("BORROWED"));

        // 3. Devolução da chave
        mockMvc.perform(post("/keys/" + keyId + "/checkin")
                        .header("Authorization", "Bearer " + doormanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "notes", "Chave devolvida intacta"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStatus").value("AVAILABLE"))
                .andExpect(jsonPath("$.returnedAt").isNotEmpty());
    }

    // =========================================================================
    // 4. BOTÃO DE PÂNICO E ALERTA DE EMERGÊNCIA CONDOMINIAL
    // =========================================================================
    @Test
    @DisplayName("Feature 4: Disparo de alerta de emergência e resolução")
    void testEmergencyAlertTriggerAndResolve() throws Exception {
        String residentToken = login(residentUser.getEmail());

        // Morador aciona botão de pânico
        String response = mockMvc.perform(post("/condominiums/" + condo.getId() + "/emergency-alerts")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "emergencyType", "MEDICAL",
                                "details", "Apto 101, morador idoso necessitando de primeiros socorros",
                                "contactPhone", "(11) 99999-1111"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.emergencyType").value("MEDICAL"))
                .andReturn().getResponse().getContentAsString();

        Long alertId = objectMapper.readTree(response).get("id").asLong();

        // Porteiro/Síndico resolve a ocorrência
        String doormanToken = login(doormanUser.getEmail());
        mockMvc.perform(patch("/emergency-alerts/" + alertId + "/resolve")
                        .header("Authorization", "Bearer " + doormanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "resolutionNotes", "Ambulância SAMU chegou em 8 minutos e atendeu o morador."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
    }

    // =========================================================================
    // 5. PREVISÃO ORÇAMENTÁRIA VS REALIZADO
    // =========================================================================
    @Test
    @DisplayName("Feature 5: Configuração de meta orçamentária e cálculo comparativo com despesas reais")
    void testBudgetForecastAndComparison() throws Exception {
        String managerToken = login(managerUser.getEmail());

        // Síndico define orçamento previsto para o mês
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/financial/budgets")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "year", 2026,
                                "month", 11,
                                "category", "MANUTENCAO",
                                "budgetedAmount", 8000.00
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.budgetedAmount").value(8000.00));

        // Morador ou Síndico visualiza o comparativo Orçado vs Realizado
        String residentToken = login(residentUser.getEmail());
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/financial/budget-comparison")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("year", "2026")
                        .param("month", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(11));
    }

    // =========================================================================
    // 6. MEDIÇÃO INDIVIDUALIZADA DE ÁGUA/GÁS COM DETECÇÃO DE VAZAMENTO
    // =========================================================================
    @Test
    @DisplayName("Feature 6: Lançamento de leituras de hidrômetro/gás e detecção de vazamento")
    void testUtilityReadingAndLeakDetection() throws Exception {
        String doormanToken = login(doormanUser.getEmail());

        // 1ª leitura mensal: 100 m³
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/utility-readings")
                        .header("Authorization", "Bearer " + doormanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "unitId", unit101.getId(),
                                "utilityType", "WATER",
                                "year", 2026,
                                "month", 10,
                                "currentReading", 100.00,
                                "readingDate", LocalDate.now().minusMonths(1).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumption").value(100.00));

        // 2ª leitura mensal: 145 m³ (consumo = 45 m³ > 30 m³ limite residencial -> flag vazamento)
        mockMvc.perform(post("/condominiums/" + condo.getId() + "/utility-readings")
                        .header("Authorization", "Bearer " + doormanToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "unitId", unit101.getId(),
                                "utilityType", "WATER",
                                "year", 2026,
                                "month", 11,
                                "currentReading", 145.00,
                                "readingDate", LocalDate.now().toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumption").value(45.00))
                .andExpect(jsonPath("$.leakSuspicion").value(true));
    }

    // =========================================================================
    // 7. INSPEÇÃO PREDIAL PERIÓDICA E CHECKLIST DE MANUTENÇÃO PREVENTIVA
    // =========================================================================
    @Test
    @DisplayName("Feature 7: Criação e execução de checklist de inspeção predial periódica")
    void testMaintenanceChecklistWorkflow() throws Exception {
        String managerToken = login(managerUser.getEmail());

        // Síndico programa checklist preventivo
        String response = mockMvc.perform(post("/condominiums/" + condo.getId() + "/maintenance/checklists")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Inspeção Semanal de Bombas e Para-raios",
                                "frequency", "WEEKLY",
                                "items", List.of(
                                        Map.of("itemDescription", "Verificar nível de óleo na bomba de recalque", "status", "CONFORMING"),
                                        Map.of("itemDescription", "Testar disjuntor do sistema SPDA", "status", "CONFORMING")
                                )
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Long checklistId = objectMapper.readTree(response).get("id").asLong();

        // Conclusão da inspeção
        mockMvc.perform(patch("/maintenance/checklists/" + checklistId + "/complete")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "generalNotes", "Todos os itens vistoriados e conformes."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    // =========================================================================
    // 8. GESTÃO DE CONTRATOS DE PRESTAÇÃO DE SERVIÇOS E GARANTIAS
    // =========================================================================
    @Test
    @DisplayName("Feature 8: Gestão do ciclo de vida de contratos com prestadores terceirizados")
    void testServiceContractManagement() throws Exception {
        String managerToken = login(managerUser.getEmail());

        // Cadastro de contrato de manutenção de elevadores
        String response = mockMvc.perform(post("/condominiums/" + condo.getId() + "/contracts")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Manutenção Mensal de Elevadores",
                                "category", "ELEVATOR_MAINTENANCE",
                                "monthlyValue", 1450.00,
                                "startDate", LocalDate.now().minusMonths(6).toString(),
                                "endDate", LocalDate.now().plusMonths(6).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        Long contractId = objectMapper.readTree(response).get("id").asLong();

        // Rescisão / Encerramento do contrato
        mockMvc.perform(patch("/contracts/" + contractId + "/terminate")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    // =========================================================================
    // 9. CONVITE DIGITAL WHATSAPP COM QR CODE PARA VISITANTES
    // =========================================================================
    @Test
    @DisplayName("Feature 9: Geração de passe digital de visitante com link e mensagem WhatsApp")
    void testVisitorDigitalPass() throws Exception {
        // Morador registra pré-autorização de visitante
        VisitorAccess visitor = new VisitorAccess();
        visitor.setCondominium(condo);
        visitor.setResident(residentUser);
        visitor.setVisitorName("Carlos Mendes");
        visitor.setDocumentNumber("12.345.678-9");
        visitor.setUnit(unit101);
        visitor.setAccessCode("VIS-7788");
        visitor.setValidFrom(LocalDateTime.now().minusHours(1));
        visitor.setValidUntil(LocalDateTime.now().plusHours(12));
        visitor = visitorAccessRepository.save(visitor);

        String residentToken = login(residentUser.getEmail());

        // Morador gera o passe digital com mensagem personalizada de WhatsApp
        mockMvc.perform(get("/visitors/" + visitor.getId() + "/digital-pass")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorName").value("Carlos Mendes"))
                .andExpect(jsonPath("$.accessCode").value("VIS-7788"))
                .andExpect(jsonPath("$.whatsappMessageText").value(org.hamcrest.Matchers.containsString("VIS-7788")));
    }

    // =========================================================================
    // 10. SMART LOCKERS AUTO-RETIRADA (CONDOMINIUM COMMUNITY API)
    // =========================================================================
    @Test
    @DisplayName("Feature 10: Rota e integração do armário inteligente / Smart Locker")
    void testSmartLockerIntegration() throws Exception {
        String residentToken = login(residentUser.getEmail());

        // Consulta de lockers instalados no condomínio
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/lockers")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // AUXILIARES
    // =========================================================================
    private Role getOrCreateRole(String roleName) {
        Role role = roleRepository.findByAuthority(roleName);
        if (role == null || role.getId() == null || !roleRepository.existsById(role.getId())) {
            role = new Role();
            role.setAuthority(roleName);
            role = roleRepository.save(role);
        }
        return role;
    }

    private User createUser(String firstName, String lastName, String email, Role role) {
        User u = new User();
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        u.addRole(role);
        return userRepository.save(u);
    }

    private void createMembership(User user, Role role, Unit unit) {
        Membership mem = new Membership();
        mem.setUser(user);
        mem.setCondominium(condo);
        mem.setRole(role);
        mem.setStatus(MembershipStatus.ACTIVE);
        if (unit != null) {
            mem.setRequestedUnit(unit);
        }
        membershipRepository.save(mem);
    }

    private String login(String email) throws Exception {
        String credentials = "test-client:test-secret";
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        String response = mockMvc.perform(post("/oauth2/token")
                        .header("Authorization", basicAuth)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("grant_type", "password")
                        .param("username", email)
                        .param("password", DEFAULT_PASSWORD)
                        .param("scope", "read write"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("access_token").asText();
    }
}
