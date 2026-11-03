package com.cefet_rj.projeto_final.realtime;

import com.cefet_rj.projeto_final.notifications.dtos.SystemAnnouncementRequestDTO;
import com.cefet_rj.projeto_final.notifications.entities.Notification;
import com.cefet_rj.projeto_final.notifications.enums.NotificationType;
import com.cefet_rj.projeto_final.notifications.repositories.NotificationRepository;
import com.cefet_rj.projeto_final.notifications.services.NotificationService;
import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import com.cefet_rj.projeto_final.realtime.services.RealtimeEmitterService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class RealtimeAndNotificationEnhancementsTests {

    private static final String DEFAULT_PASSWORD = "StrongPassword123!";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private RealtimeEmitterService realtimeEmitterService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User testUser;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        Role foundRole = roleRepository.findByAuthority("ROLE_RESIDENT");
        if (foundRole == null) {
            Role r = new Role();
            r.setAuthority("ROLE_RESIDENT");
            foundRole = roleRepository.save(r);
        }
        final Role residentRole = foundRole;

        testUser = userRepository.findByEmail("realtime_user@test.com").orElseGet(() -> {
            User u = new User();
            u.setFirstName("Realtime");
            u.setLastName("Tester");
            u.setEmail("realtime_user@test.com");
            u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            u.addRole(residentRole);
            return userRepository.save(u);
        });

        token = login(testUser.getEmail());
    }

    @Test
    void testRealtimeEmitterRegistrationAndBroadcast() {
        SseEmitter emitter = realtimeEmitterService.registerEmitter(testUser.getId(), List.of(1L, 2L));
        assertNotNull(emitter);

        assertDoesNotThrow(() -> {
            realtimeEmitterService.sendToUser(testUser.getId(),
                    RealtimeEventDTO.of("NOTIFICATION", 1L, Map.of("msg", "Teste SSE")));
            realtimeEmitterService.sendToCondominium(1L,
                    RealtimeEventDTO.of("ANNOUNCEMENT", 1L, Map.of("title", "Aviso")));
            realtimeEmitterService.sendToAll(
                    RealtimeEventDTO.of("SYSTEM_ANNOUNCEMENT", null, Map.of("title", "Geral")));
            realtimeEmitterService.sendHeartbeat();
        });
    }

    @Test
    void testUnreadCountAndMarkAllAsReadEndpoints() throws Exception {
        // Cria uma notificação não lida para o usuário
        Notification notice = new Notification();
        notice.setRecipient(testUser);
        notice.setType(NotificationType.ANNOUNCEMENT);
        notice.setTitle("Aviso Teste");
        notice.setMessage("Mensagem do aviso");
        notice.setRead(false);
        notice.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notice);

        // Verifica contagem de não lidos
        mockMvc.perform(get("/notifications/my/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").isNumber());

        // Marca todas como lidas
        mockMvc.perform(patch("/notifications/read-all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verifica que o número de não lidos agora é 0
        mockMvc.perform(get("/notifications/my/unread-count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void testRealtimeEventsSseEndpoint() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/realtime/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn();

        assertNotNull(mvcResult);
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
