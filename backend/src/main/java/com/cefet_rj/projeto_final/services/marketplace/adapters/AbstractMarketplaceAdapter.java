package com.cefet_rj.projeto_final.services.marketplace.adapters;

import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceOrderDTO;
import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceWebhookDTO;
import com.cefet_rj.projeto_final.services.marketplace.MarketplaceAdapter;
import com.cefet_rj.projeto_final.services.marketplace.MarketplaceOrderClient;
import com.cefet_rj.projeto_final.services.marketplace.MarketplaceProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public abstract class AbstractMarketplaceAdapter implements MarketplaceAdapter {

    private final MarketplaceOrderClient orderClient;
    private final MarketplaceProperties properties;

    protected AbstractMarketplaceAdapter(MarketplaceOrderClient orderClient, MarketplaceProperties properties) {
        this.orderClient = orderClient;
        this.properties = properties;
    }

    @Override
    public boolean isValidWebhook(MarketplaceWebhookDTO payload, Map<String, String> headers) {
        MarketplaceProperties.Store store = store();
        String secret = store.getWebhookSecret();
        if (secret == null || secret.isBlank()) return true;

        String signature = headers.getOrDefault("x-hub-signature-256", headers.get("x-signature"));
        if (signature == null) return false;
        String expected = hmacSha256(payload.payload().toString(), secret);
        String received = signature.replace("sha256=", "");
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isOrderCreatedEvent(MarketplaceWebhookDTO payload) {
        String event = Optional.ofNullable(payload.getString("topic"))
                .orElseGet(() -> Optional.ofNullable(payload.getString("event")).orElse(payload.getString("type")));
        return event != null && orderCreatedEvents().stream().anyMatch(value -> value.equalsIgnoreCase(event));
    }

    @Override
    public String extractOrderId(MarketplaceWebhookDTO payload) {
        Long numericId = Optional.ofNullable(payload.getLong("order_id"))
                .orElseGet(() -> Optional.ofNullable(payload.getLong("orderId")).orElse(payload.getLong("id")));
        if (numericId != null) return numericId.toString();

        String resource = payload.getString("resource");
        if (resource != null && resource.contains("/orders/")) {
            return resource.substring(resource.lastIndexOf('/') + 1);
        }
        return payload.getString("orderCode");
    }

    @Override
    public MarketplaceOrderDTO fetchOrder(String orderId) {
        Map<String, Object> order = orderClient.findOrder(orderId, store());
        if (order == null) order = Map.of();
        return new MarketplaceOrderDTO(
                marketplaceCode(),
                Objects.toString(value(order, "id", "order_id", "orderId", "orderCode"), orderId),
                buildDescription(orderId, order),
                Objects.toString(value(order, "buyer.email", "customer.email", "email"), null),
                asLong(value(order, "recipientId", "recipient_id")),
                asLong(value(order, "unitId", "unit_id"))
        );
    }

    protected MarketplaceProperties.Store store() {
        MarketplaceProperties.Store store = properties.getStore(marketplaceCode());
        if (store == null) {
            throw new IllegalStateException("Marketplace não configurado: " + marketplaceCode());
        }
        return store;
    }

    protected abstract List<String> orderCreatedEvents();

    private String buildDescription(String orderId, Map<String, Object> order) {
        Object item = value(order, "items", "order_items", "products");
        return displayName() + " - Pedido " + orderId + (item == null ? "" : " - " + item);
    }

    private Object value(Map<String, Object> source, String... paths) {
        for (String path : paths) {
            Object current = source;
            for (String part : path.split("\\.")) {
                if (!(current instanceof Map<?, ?> map)) {
                    current = null;
                    break;
                }
                current = map.get(part);
            }
            if (current != null) return current;
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao validar assinatura do marketplace " + marketplaceCode(), e);
        }
    }

    protected abstract String displayName();
}
