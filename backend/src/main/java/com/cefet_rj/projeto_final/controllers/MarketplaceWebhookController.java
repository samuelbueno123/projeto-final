package com.cefet_rj.projeto_final.controllers;

import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceWebhookDTO;
import com.cefet_rj.projeto_final.services.marketplace.MarketplaceWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/webhooks/marketplaces")
@RequiredArgsConstructor
public class MarketplaceWebhookController {

    private final MarketplaceWebhookService webhookService;

    @PostMapping("/{marketplaceCode}")
    public ResponseEntity<Void> receive(
            @PathVariable String marketplaceCode,
            @RequestHeader Map<String, String> headers,
            @RequestBody MarketplaceWebhookDTO payload
    ) {
        Map<String, String> normalizedHeaders = headers.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(Locale.ROOT),
                        Map.Entry::getValue,
                        (first, second) -> first
                ));
        webhookService.process(marketplaceCode, payload, normalizedHeaders);
        return ResponseEntity.ok().build();
    }
}
