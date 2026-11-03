package com.cefet_rj.projeto_final.commercial;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.entities.Resident;
import com.cefet_rj.projeto_final.condominiums.entities.Sector;
import com.cefet_rj.projeto_final.condominiums.entities.Unit;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.enums.ResidentType;
import com.cefet_rj.projeto_final.condominiums.enums.SectorStructuralType;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.ResidentRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.SectorRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.UnitRepository;
import com.cefet_rj.projeto_final.users.entities.Role;
import com.cefet_rj.projeto_final.users.entities.User;
import com.cefet_rj.projeto_final.users.repositories.RoleRepository;
import com.cefet_rj.projeto_final.users.repositories.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;
import java.util.Set;

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
public class CommercialModulesIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CondominiumRepository condominiumRepository;
    @Autowired private BillingPlanRepository billingPlanRepository;
    @Autowired private FeatureRepository featureRepository;
    @Autowired private SectorRepository sectorRepository;
    @Autowired private UnitRepository unitRepository;
    @Autowired private MembershipRepository membershipRepository;
    @Autowired private ResidentRepository residentRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Condominium condo;
    private User managerUser;
    private User residentUser;
    private String managerToken;
    private String residentToken;

    @BeforeEach
    void setUp() throws Exception {
        Role managerRole = getOrCreateRole("ROLE_MANAGER");
        Role residentRole = getOrCreateRole("ROLE_RESIDENT");

        Feature reservationsFeature = getOrCreateFeature(FeatureCode.RESERVATIONS);
        Feature packagesFeature = getOrCreateFeature(FeatureCode.PACKAGES);

        BillingPlan plan = new BillingPlan();
        plan.setName("COMMERCIAL_SUITE_TEST_PLAN");
        plan.setMonthlyFee(new BigDecimal("199.00"));
        plan.setActive(true);
        plan.setFeatures(Set.of(reservationsFeature, packagesFeature));
        plan = billingPlanRepository.save(plan);

        condo = new Condominium();
        condo.setName("Residencial Grand Ville");
        condo.setAddress("Avenida Principal, 500");
        condo.setBillingPlan(plan);
        condo.setIsActive(true);
        condo = condominiumRepository.save(condo);

        Sector sector = new Sector();
        sector.setName("Bloco B");
        sector.setStructuralType(SectorStructuralType.BLOCK);
        sector.setCondominium(condo);
        sector = sectorRepository.save(sector);

        Unit unit = new Unit();
        unit.setIdentifier("201");
        unit.setSector(sector);
        unit = unitRepository.save(unit);

        managerUser = createUser("Carlos", "Sindico", "carlos.manager@grandville.com", managerRole);
        residentUser = createUser("Mariana", "Moradora", "mariana.resident@grandville.com", residentRole);

        Membership manMem = new Membership();
        manMem.setUser(managerUser);
        manMem.setCondominium(condo);
        manMem.setRole(managerRole);
        manMem.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(manMem);

        Membership resMem = new Membership();
        resMem.setUser(residentUser);
        resMem.setCondominium(condo);
        resMem.setRole(residentRole);
        resMem.setStatus(MembershipStatus.ACTIVE);
        resMem.setRequestedUnit(unit);
        membershipRepository.save(resMem);

        Resident resident = new Resident();
        resident.setUser(residentUser);
        resident.setUnit(unit);
        resident.setType(ResidentType.OWNER);
        residentRepository.save(resident);

        managerToken = login(managerUser.getEmail());
        residentToken = login(residentUser.getEmail());
    }

    @Test
    void testFinancialExpensesAndSummaryWorkflow() throws Exception {
        // 1. Manager creates an expense
        String createExpenseJson = objectMapper.writeValueAsString(Map.of(
                "category", "MANUTENCAO",
                "description", "Limpeza e higienização das caixas d'água",
                "amount", new BigDecimal("1250.00"),
                "dueDate", LocalDate.now().plusDays(10).toString(),
                "supplier", "AcquaClean Serviços",
                "documentNumber", "NF-998822"
        ));

        String postExpenseResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/financial/expenses")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExpenseJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.category").value("MANUTENCAO"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        JsonNode createdExpense = objectMapper.readTree(postExpenseResp);
        long expenseId = createdExpense.get("id").asLong();

        // 2. Resident can view financial summary (transparency)
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/financial/summary")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentExpenses").isArray());

        // 3. Manager marks expense as paid
        mockMvc.perform(patch("/financial/expenses/" + expenseId + "/pay")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").isNotEmpty());
    }

    @Test
    void testMaintenanceOrdersWorkflow() throws Exception {
        // 1. Manager creates a maintenance order
        String createOrderJson = objectMapper.writeValueAsString(Map.of(
                "title", "Manutenção preventiva dos elevadores",
                "description", "Troca preventiva de cabos e lubrificação técnica",
                "category", "ELEVADORES",
                "type", "PREVENTIVE",
                "priority", "HIGH",
                "contractorName", "Atlas Schindler",
                "contractorContact", "(11) 98888-7777",
                "scheduledDate", LocalDate.now().plusDays(3).toString(),
                "estimatedCost", new BigDecimal("2500.00")
        ));

        String postResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/maintenance/orders")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();

        long orderId = objectMapper.readTree(postResp).get("id").asLong();

        // 2. Resident lists maintenance orders
        mockMvc.perform(get("/condominiums/" + condo.getId() + "/maintenance/orders")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Manutenção preventiva dos elevadores"));

        // 3. Manager updates order status to COMPLETED
        String updateStatusJson = objectMapper.writeValueAsString(Map.of(
                "status", "COMPLETED",
                "actualCost", new BigDecimal("2450.00"),
                "notes", "Serviço concluído dentro do prazo e com emissão de laudo técnico."
        ));

        mockMvc.perform(patch("/maintenance/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateStatusJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    @Test
    void testVirtualAssembliesAndVotingWorkflow() throws Exception {
        // 1. Manager creates an assembly
        String createAssemblyJson = objectMapper.writeValueAsString(Map.of(
                "title", "Assembleia Geral Ordinária 2026",
                "description", "Deliberação orçamentária e eleição de comissão consultiva",
                "type", "ORDINARY",
                "meetingDate", LocalDateTime.now().plusDays(7).toString(),
                "locationOrLink", "https://meet.google.com/condo-ago-2026"
        ));

        String assemblyResp = mockMvc.perform(post("/condominiums/" + condo.getId() + "/assemblies")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createAssemblyJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn().getResponse().getContentAsString();

        long assemblyId = objectMapper.readTree(assemblyResp).get("id").asLong();

        // 2. Manager adds a topic to the assembly
        String createTopicJson = objectMapper.writeValueAsString(Map.of(
                "title", "Aprovação da previsão orçamentária para 2026",
                "description", "Reajuste de 4.5% no condomínio para cobrir inflação de serviços",
                "displayOrder", 1
        ));

        String topicResp = mockMvc.perform(post("/assemblies/" + assemblyId + "/topics")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createTopicJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.votingOpen").value(false))
                .andReturn().getResponse().getContentAsString();

        long topicId = objectMapper.readTree(topicResp).get("id").asLong();

        // 3. Manager opens voting for this topic
        mockMvc.perform(patch("/assembly-topics/" + topicId + "/voting?open=true")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.votingOpen").value(true));

        // 4. Resident casts vote IN_FAVOR
        String castVoteJson = objectMapper.writeValueAsString(Map.of(
                "voteChoice", "IN_FAVOR"
        ));

        mockMvc.perform(post("/assembly-topics/" + topicId + "/votes")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(castVoteJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inFavorCount").value(1))
                .andExpect(jsonPath("$.userVote").value("IN_FAVOR"));

        // 5. Trying to vote again throws Forbidden
        mockMvc.perform(post("/assembly-topics/" + topicId + "/votes")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(castVoteJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCondoAssistantIntelligence() throws Exception {
        String assistantQueryJson = objectMapper.writeValueAsString(Map.of(
                "question", "Qual é o horário de silêncio e as regras para som alto no condomínio?"
        ));

        mockMvc.perform(post("/condominiums/" + condo.getId() + "/assistant/query")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assistantQueryJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("22h00")))
                .andExpect(jsonPath("$.legalBasis").value(org.hamcrest.Matchers.containsString("Art. 1.336")))
                .andExpect(jsonPath("$.suggestedFollowUps").isArray());
    }

    private Feature getOrCreateFeature(FeatureCode code) {
        return featureRepository.findByCode(code.name()).orElseGet(() -> {
            Feature f = new Feature();
            f.setCode(code.name());
            f.setName(code.name());
            return featureRepository.save(f);
        });
    }

    private Role getOrCreateRole(String authority) {
        Role role = roleRepository.findByAuthority(authority);
        if (role == null) {
            role = new Role();
            role.setAuthority(authority);
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
