package com.cefet_rj.projeto_final.dtos.marketplace;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class MarketplaceWebhookDTO {

    private final Map<String, Object> payload = new HashMap<>();

    @JsonAnySetter
    public void put(String key, Object value) {
        payload.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> payload() {
        return payload;
    }

    public String getString(String key) {
        Object value = payload.get(key);
        return value == null ? null : value.toString();
    }

    public Long getLong(String key) {
        Object value = payload.get(key);
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
