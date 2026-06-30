package com.cefet_rj.projeto_final.services.marketplace;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MarketplaceOrderClient {

    private final RestClient.Builder restClientBuilder;

    @SuppressWarnings("unchecked")
    public Map<String, Object> findOrder(String orderId, MarketplaceProperties.Store store) {
        RestClient.Builder builder = restClientBuilder.baseUrl(store.getApiBaseUrl());
        if (store.getAccessToken() != null && !store.getAccessToken().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + store.getAccessToken());
        }
        return builder.build()
                .get()
                .uri(store.getOrderPath(), orderId)
                .retrieve()
                .body(Map.class);
    }
}
