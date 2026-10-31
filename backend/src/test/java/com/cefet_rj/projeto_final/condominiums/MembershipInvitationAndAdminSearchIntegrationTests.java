package com.cefet_rj.projeto_final.condominiums;

import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.entities.Membership;
import com.cefet_rj.projeto_final.condominiums.enums.MembershipStatus;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.repositories.MembershipRepository;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
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
import java.util.Base64;
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
class MembershipInvitationAndAdminSearchIntegrationTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

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
    private BillingPlanRepository billingPlanRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role adminRole;
    private Role managerRole;
    private Role doormanRole;
    private Role residentRole;

    private Condominium condoAlpha;
    private Condominium condoBeta;

    private User adminUser;
    private User managerAlpha;
    private User invitedResident;
    private User invitedDoorman;
    private User otherUser;

    @BeforeEach
    void setUp() {
        adminRole = getOrCreateRole("ROLE_ADMIN");
        managerRole = getOrCreateRole("ROLE_MANAGER");
        doormanRole = getOrCreateRole("ROLE_DOORMAN");
        residentRole = getOrCreateRole("ROLE_RESIDENT");

        BillingPlan plan = billingPlanRepository.findByNameIgnoreCase("TEST_PLAN")
                .orElseGet(() -> {
                    BillingPlan bp = new BillingPlan();
                    bp.setName("TEST_PLAN");
                    bp.setMonthlyFee(new BigDecimal("199.00"));
                    bp.setActive(true);
                    bp.setFeatures(Set.of());
                    return billingPlanRepository.save(bp);
                });

        condoAlpha = createCondominium("Condomínio Alpha", "Rua Alpha, 100", plan);
        condoBeta = createCondominium("Condomínio Beta", "Rua Beta, 200", plan);

        adminUser = createUser("Admin", "System", "admin.flow@test.com", adminRole);
        managerAlpha = createUser("Carlos", "Síndico", "manager.alpha@test.com", managerRole);
        invitedResident = createUser("Marina", "Moradora", "marina.resident@test.com", residentRole);
        invitedDoorman = createUser("Paulo", "Porteiro", "paulo.doorman@test.com", residentRole);
        otherUser = createUser("Outro", "Usuário", "outro.user@test.com", residentRole);

        Membership managerMembership = new Membership();
        managerMembership.setUser(managerAlpha);
        managerMembership.setCondominium(condoAlpha);
        managerMembership.setRole(managerRole);
        managerMembership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(managerMembership);
    }

    @Test
    void managerInvitationFlowRequiresRecipientAcceptanceAndGrantsAccessOnlyAfterAccept() throws Exception {
        String managerToken = login(managerAlpha.getEmail());
        String residentToken = login(invitedResident.getEmail());
        String otherToken = login(otherUser.getEmail());

        // Before invitation: resident has no memberships and cannot access condominium details
        mockMvc.perform(get("/memberships/my")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/condominiums/" + condoAlpha.getId())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        // Manager cannot invite to a condominium they do not manage
        mockMvc.perform(post("/manager/condominiums/" + condoBeta.getId() + "/membership-invitations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", invitedResident.getEmail(), "authority", "ROLE_RESIDENT")
                        )))
                .andExpect(status().isForbidden());

        // Manager invites resident to condoAlpha
        String inviteResponse = mockMvc.perform(post("/manager/condominiums/" + condoAlpha.getId() + "/membership-invitations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", invitedResident.getEmail(), "authority", "ROLE_RESIDENT")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.condominiumId").value(condoAlpha.getId()))
                .andExpect(jsonPath("$.condominiumName").value("Condomínio Alpha"))
                .andExpect(jsonPath("$.authority").value("ROLE_RESIDENT"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.notificationId").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long invitationId = objectMapper.readTree(inviteResponse).get("id").asLong();

        // Pending invitation does NOT grant access yet
        mockMvc.perform(get("/memberships/my")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(get("/condominiums/" + condoAlpha.getId())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden());

        // Notification arrives on resident's account with invitation metadata
        mockMvc.perform(get("/notifications/my")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("MEMBERSHIP_INVITATION"))
                .andExpect(jsonPath("$[0].invitationId").value(invitationId))
                .andExpect(jsonPath("$[0].condominiumId").value(condoAlpha.getId()))
                .andExpect(jsonPath("$[0].condominiumName").value("Condomínio Alpha"))
                .andExpect(jsonPath("$[0].authority").value("ROLE_RESIDENT"))
                .andExpect(jsonPath("$[0].invitationStatus").value("PENDING"));

        // Non-recipient cannot accept or decline the invitation
        mockMvc.perform(post("/membership-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/membership-invitations/" + invitationId + "/decline")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        // Recipient accepts the invitation
        mockMvc.perform(post("/membership-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(invitationId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.condominiumId").value(condoAlpha.getId()));

        // Accepting again returns 409 Conflict
        mockMvc.perform(post("/membership-invitations/" + invitationId + "/accept")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isConflict());

        // Now resident has active membership and access to the condominium
        mockMvc.perform(get("/memberships/my")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].condominiumId").value(condoAlpha.getId()))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));

        mockMvc.perform(get("/condominiums/" + condoAlpha.getId())
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Condomínio Alpha"));

        // Notification reflects ACTIVE status and read=true
        mockMvc.perform(get("/notifications/my")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].invitationStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[0].read").value(true));
    }

    @Test
    void doormanInviteAcceptDeclineAndRemovalFlow() throws Exception {
        String managerToken = login(managerAlpha.getEmail());
        String doormanToken = login(invitedDoorman.getEmail());

        // Invite user as ROLE_DOORMAN
        String inviteResponse = mockMvc.perform(post("/manager/condominiums/" + condoAlpha.getId() + "/membership-invitations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", invitedDoorman.getEmail(), "authority", "ROLE_DOORMAN")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authority").value("ROLE_DOORMAN"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long invitationId = objectMapper.readTree(inviteResponse).get("id").asLong();

        // Decline first
        mockMvc.perform(post("/membership-invitations/" + invitationId + "/decline")
                        .header("Authorization", "Bearer " + doormanToken))
                .andExpect(status().isNoContent());

        // Declined invitation does not grant access or ROLE_DOORMAN
        mockMvc.perform(get("/memberships/my")
                        .header("Authorization", "Bearer " + doormanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        User afterDecline = userRepository.findById(invitedDoorman.getId()).orElseThrow();
        assertFalse(afterDecline.hasRole("ROLE_DOORMAN"));

        // Manager re-invites the same user as ROLE_DOORMAN
        String reInviteResponse = mockMvc.perform(post("/manager/condominiums/" + condoAlpha.getId() + "/membership-invitations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", invitedDoorman.getEmail(), "authority", "ROLE_DOORMAN")
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long secondInvitationId = objectMapper.readTree(reInviteResponse).get("id").asLong();

        // User accepts the doorman invitation
        mockMvc.perform(post("/membership-invitations/" + secondInvitationId + "/accept")
                        .header("Authorization", "Bearer " + doormanToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.authority").value("ROLE_DOORMAN"));

        User afterAccept = userRepository.findById(invitedDoorman.getId()).orElseThrow();
        assertTrue(afterAccept.hasRole("ROLE_DOORMAN"));

        // Manager removes the doorman
        mockMvc.perform(delete("/manager/condominiums/" + condoAlpha.getId() + "/memberships/" + secondInvitationId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNoContent());

        User afterRemoval = userRepository.findById(invitedDoorman.getId()).orElseThrow();
        assertFalse(afterRemoval.hasRole("ROLE_DOORMAN"));
    }

    @Test
    void adminCanListAddAndRemoveCondominiumManagersAndSearchAdminData() throws Exception {
        String adminToken = login(adminUser.getEmail());
        String managerToken = login(managerAlpha.getEmail());

        // Non-admin cannot access admin manager endpoints
        mockMvc.perform(get("/admin/condominiums/" + condoBeta.getId() + "/managers")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());

        // Admin adds otherUser as manager of condoBeta
        String addManagerResponse = mockMvc.perform(post("/admin/condominiums/" + condoBeta.getId() + "/managers")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", otherUser.getEmail())
                        )))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.condominiumId").value(condoBeta.getId()))
                .andExpect(jsonPath("$.authority").value("ROLE_MANAGER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long managerMembershipId = objectMapper.readTree(addManagerResponse).get("id").asLong();

        User promotedUser = userRepository.findById(otherUser.getId()).orElseThrow();
        assertTrue(promotedUser.hasRole("ROLE_MANAGER"));

        // Admin lists managers of condoBeta
        mockMvc.perform(get("/admin/condominiums/" + condoBeta.getId() + "/managers")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value(otherUser.getEmail()));

        // Admin removes manager from condoBeta
        mockMvc.perform(delete("/admin/condominiums/" + condoBeta.getId() + "/managers/" + managerMembershipId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        User demotedUser = userRepository.findById(otherUser.getId()).orElseThrow();
        assertFalse(demotedUser.hasRole("ROLE_MANAGER"));

        // Admin data endpoint filters by query before paginating
        mockMvc.perform(get("/admin/data/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("query", "marina.resident@test.com")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("marina.resident@test.com"));

        mockMvc.perform(get("/admin/data/condominiums")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("query", "Beta")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Condomínio Beta"));
    }

    @Test
    void systemAdminCanCreateCondominiumAndSingleNameUserCanUpdateProfile() throws Exception {
        String adminToken = login(adminUser.getEmail());
        String residentToken = login(invitedResident.getEmail());

        mockMvc.perform(post("/condominiums")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Condomínio Proibido",
                                  "address": "Rua 1, 10",
                                  "billingPlan": "Básico",
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/condominiums")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Condomínio Horizonte",
                                  "address": "Av. Atlântica, 500",
                                  "billingPlan": "Profissional",
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Condomínio Horizonte"))
                .andExpect(jsonPath("$.address").value("Av. Atlântica, 500"))
                .andExpect(jsonPath("$.billingPlanName").value("Profissional"))
                .andExpect(jsonPath("$.isActive").value(true));

        mockMvc.perform(put("/users/me/profile")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Marina",
                                  "lastName": "",
                                  "email": "marina.resident@test.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Marina"))
                .andExpect(jsonPath("$.lastName").value(""));
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

    private Condominium createCondominium(String name, String address, BillingPlan plan) {
        Condominium c = new Condominium();
        c.setName(name);
        c.setAddress(address);
        c.setBillingPlan(plan);
        c.setIsActive(true);
        return condominiumRepository.save(c);
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
