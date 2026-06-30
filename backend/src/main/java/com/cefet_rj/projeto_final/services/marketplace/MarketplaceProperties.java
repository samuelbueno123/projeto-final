package com.cefet_rj.projeto_final.services.marketplace;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "marketplaces")
public class MarketplaceProperties {

    private final Map<String, Store> stores = new HashMap<>();

    public Map<String, Store> getStores() {
        return stores;
    }

    public Store getStore(String code) {
        return stores.get(code);
    }

    public static class Store {
        private String apiBaseUrl;
        private String accessToken;
        private String webhookSecret;
        private String orderPath = "/orders/{orderId}";
        private Long defaultRecipientId;
        private Long defaultUnitId;

        public String getApiBaseUrl() { return apiBaseUrl; }
        public void setApiBaseUrl(String apiBaseUrl) { this.apiBaseUrl = apiBaseUrl; }
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getWebhookSecret() { return webhookSecret; }
        public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
        public String getOrderPath() { return orderPath; }
        public void setOrderPath(String orderPath) { this.orderPath = orderPath; }
        public Long getDefaultRecipientId() { return defaultRecipientId; }
        public void setDefaultRecipientId(Long defaultRecipientId) { this.defaultRecipientId = defaultRecipientId; }
        public Long getDefaultUnitId() { return defaultUnitId; }
        public void setDefaultUnitId(Long defaultUnitId) { this.defaultUnitId = defaultUnitId; }
    }
}
