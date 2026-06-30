package com.cefet_rj.projeto_final.services.marketplace.adapters;

import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceWebhookDTO;
import com.cefet_rj.projeto_final.services.marketplace.MarketplaceOrderClient;
import com.cefet_rj.projeto_final.services.marketplace.MarketplaceProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@Component
public class MercadoLivreAdapter extends AbstractMarketplaceAdapter {

    public MercadoLivreAdapter(MarketplaceOrderClient orderClient, MarketplaceProperties properties) {
        super(orderClient, properties);
    }

    @Override
    public String marketplaceCode() {
        return "mercado-livre";
    }

    @Override
    public boolean isValidWebhook(MarketplaceWebhookDTO payload, Map<String, String> headers) {
        String secret = store().getWebhookSecret();
        if (secret == null || secret.isBlank()) return true;

        String signature = headers.get("x-signature");
        String requestId = headers.get("x-request-id");
        String resource = payload.getString("resource");
        if (signature == null || requestId == null || resource == null) return false;

        String timestamp = signaturePart(signature, "ts");
        String received = signaturePart(signature, "v1");
        String manifest = "id:" + resource + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        String expected = hmacSha256(manifest, secret);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected List<String> orderCreatedEvents() {
        return List.of("orders_v2", "orders");
    }

    @Override
    protected String displayName() {
        return "Mercado Livre";
    }

    private String signaturePart(String signature, String key) {
        for (String part : signature.split(",")) {
            String[] keyValue = part.split("=", 2);
            if (keyValue.length == 2 && key.equals(keyValue[0].trim())) return keyValue[1].trim();
        }
        return "";
    }
}
