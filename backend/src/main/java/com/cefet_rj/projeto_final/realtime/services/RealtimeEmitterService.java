package com.cefet_rj.projeto_final.realtime.services;

import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
public class RealtimeEmitterService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutos

    // userId -> lista de SseEmitter ativos (caso usuário abra em múltiplos dispositivos/abas)
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // condominiumId -> conjunto de userIds conectados
    private final Map<Long, CopyOnWriteArraySet<Long>> condoUsers = new ConcurrentHashMap<>();

    /**
     * Registra uma nova conexão SSE para o usuário autenticado e seus condomínios.
     */
    public SseEmitter registerEmitter(Long userId, Collection<Long> condominiumIds) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        if (condominiumIds != null) {
            for (Long condoId : condominiumIds) {
                if (condoId != null) {
                    condoUsers.computeIfAbsent(condoId, k -> new CopyOnWriteArraySet<>()).add(userId);
                }
            }
        }

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> {
            log.debug("SSE timeout para userId {}", userId);
            emitter.complete();
            removeEmitter(userId, emitter);
        });
        emitter.onError(e -> {
            log.debug("SSE erro para userId {}: {}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        // Envia evento de confirmação de conexão inicial
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(RealtimeEventDTO.of("CONNECTED", Map.of("userId", userId, "status", "ONLINE"))));
        } catch (IOException e) {
            removeEmitter(userId, emitter);
        }

        log.debug("Nova conexão SSE estabelecida para userId {}. Total ativos: {}", userId, userEmitters.get(userId).size());
        return emitter;
    }

    /**
     * Envia evento para um usuário específico em todas as suas conexões ativas.
     */
    public void sendToUser(Long userId, RealtimeEventDTO event) {
        if (userId == null) return;
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.type())
                        .data(event));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        for (SseEmitter dead : deadEmitters) {
            removeEmitter(userId, dead);
        }
    }

    /**
     * Envia evento em tempo real para todos os usuários com sessão ativa no condomínio informado.
     */
    public void sendToCondominium(Long condominiumId, RealtimeEventDTO event) {
        if (condominiumId == null) return;
        Set<Long> users = condoUsers.get(condominiumId);
        if (users == null || users.isEmpty()) {
            return;
        }

        for (Long userId : users) {
            sendToUser(userId, event);
        }
    }

    /**
     * Envia evento global para todos os usuários conectados no sistema.
     */
    public void sendToAll(RealtimeEventDTO event) {
        for (Long userId : userEmitters.keySet()) {
            sendToUser(userId, event);
        }
    }

    /**
     * Heartbeat executado a cada 25 segundos para manter canais abertos através de firewalls e proxies reversos.
     */
    @Scheduled(fixedRate = 25000)
    public void sendHeartbeat() {
        for (Map.Entry<Long, CopyOnWriteArrayList<SseEmitter>> entry : userEmitters.entrySet()) {
            Long userId = entry.getKey();
            List<SseEmitter> emitters = entry.getValue();
            List<SseEmitter> deadEmitters = new ArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }

            for (SseEmitter dead : deadEmitters) {
                removeEmitter(userId, dead);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
                // Remove de condoUsers se não tiver mais nenhuma conexão
                for (Set<Long> set : condoUsers.values()) {
                    set.remove(userId);
                }
            }
        }
    }
}
