package com.cefet_rj.projeto_final.realtime.services;

import com.cefet_rj.projeto_final.realtime.dtos.RealtimeEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RealtimeEmitterService {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutos
    private static final int MAX_EMITTERS_PER_USER = 5;

    // userId -> lista de SseEmitter ativos protegida por operações atômicas no ConcurrentHashMap
    private final ConcurrentHashMap<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // condominiumId -> conjunto thread-safe de userIds conectados
    private final ConcurrentHashMap<Long, Set<Long>> condoUsers = new ConcurrentHashMap<>();

    /**
     * Registra uma nova conexão SSE para o usuário autenticado e seus condomínios.
     */
    public SseEmitter registerEmitter(Long userId, Collection<Long> condominiumIds) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        List<SseEmitter> evicted = new ArrayList<>();

        userEmitters.compute(userId, (k, existing) -> {
            List<SseEmitter> list = existing != null ? existing : new ArrayList<>();
            while (list.size() >= MAX_EMITTERS_PER_USER) {
                evicted.add(list.remove(0));
            }
            list.add(emitter);
            return list;
        });

        for (SseEmitter old : evicted) {
            try {
                old.complete();
            } catch (Exception ignored) {
            }
        }

        if (condominiumIds != null) {
            for (Long condoId : condominiumIds) {
                if (condoId != null) {
                    condoUsers.computeIfAbsent(condoId, k -> ConcurrentHashMap.newKeySet()).add(userId);
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
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name("CONNECTED")
                        .data(RealtimeEventDTO.of("CONNECTED", Map.of("userId", userId, "status", "ONLINE"))));
            }
            log.debug("Nova conexão SSE estabelecida para userId {}.", userId);
        } catch (IOException | IllegalStateException e) {
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    /**
     * Assina um usuário que já possui conexões SSE abertas em um novo condomínio
     * (ex.: após aprovação de solicitação ou aceite de convite).
     */
    public void subscribeUserToCondominium(Long userId, Long condominiumId) {
        if (userId == null || condominiumId == null) {
            return;
        }
        if (userEmitters.containsKey(userId)) {
            condoUsers.computeIfAbsent(condominiumId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        }
    }

    public void evictUserFromCondominium(Long userId, Long condominiumId) {
        if (userId == null || condominiumId == null) {
            return;
        }
        Set<Long> users = condoUsers.get(condominiumId);
        if (users != null) {
            users.remove(userId);
        }
    }

    public void sendToUsers(Collection<Long> userIds, RealtimeEventDTO event) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueIds = new java.util.LinkedHashSet<>(userIds);
        for (Long userId : uniqueIds) {
            sendToUser(userId, event);
        }
    }

    /**
     * Envia evento para um usuário específico em todas as suas conexões ativas.
     */
    public void sendToUser(Long userId, RealtimeEventDTO event) {
        if (userId == null) return;
        List<SseEmitter> snapshot = snapshotEmitters(userId);
        if (snapshot.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : snapshot) {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event()
                            .name(event.type())
                            .data(event));
                }
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

        for (Long userId : List.copyOf(users)) {
            sendToUser(userId, event);
        }
    }

    /**
     * Envia evento global para todos os usuários conectados no sistema.
     */
    public void sendToAll(RealtimeEventDTO event) {
        for (Long userId : List.copyOf(userEmitters.keySet())) {
            sendToUser(userId, event);
        }
    }

    /**
     * Heartbeat executado a cada 25 segundos para manter canais abertos através de firewalls e proxies reversos.
     */
    @Scheduled(fixedRate = 25000, scheduler = "sseHeartbeatScheduler")
    public void sendHeartbeat() {
        for (Long userId : List.copyOf(userEmitters.keySet())) {
            List<SseEmitter> snapshot = snapshotEmitters(userId);
            if (snapshot.isEmpty()) continue;

            List<SseEmitter> deadEmitters = new ArrayList<>();
            for (SseEmitter emitter : snapshot) {
                try {
                    synchronized (emitter) {
                        emitter.send(SseEmitter.event().comment("ping"));
                    }
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                }
            }

            for (SseEmitter dead : deadEmitters) {
                removeEmitter(userId, dead);
            }
        }
    }

    private List<SseEmitter> snapshotEmitters(Long userId) {
        final List<SseEmitter>[] copy = new List[1];
        userEmitters.computeIfPresent(userId, (k, list) -> {
            copy[0] = list.isEmpty() ? List.of() : new ArrayList<>(list);
            return list;
        });
        return copy[0] != null ? copy[0] : List.of();
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        if (userId == null || emitter == null) return;
        final boolean[] allRemoved = {false};
        userEmitters.computeIfPresent(userId, (k, emitters) -> {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                allRemoved[0] = true;
                return null;
            }
            return emitters;
        });

        if (allRemoved[0]) {
            condoUsers.entrySet().removeIf(entry -> {
                entry.getValue().remove(userId);
                return entry.getValue().isEmpty();
            });
        }
    }

    public int getActiveConnectionsCount() {
        int total = 0;
        for (Long userId : userEmitters.keySet()) {
            total += snapshotEmitters(userId).size();
        }
        return total;
    }

    public int getConnectedUsersCount() {
        return userEmitters.size();
    }

    public int getConnectedCondominiumsCount() {
        return condoUsers.size();
    }
}
