package com.cefet_rj.projeto_final.services.marketplace;

import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceOrderDTO;
import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceWebhookDTO;

import java.util.Map;

public interface MarketplaceAdapter {

    String marketplaceCode();

    boolean isValidWebhook(MarketplaceWebhookDTO payload, Map<String, String> headers);

    boolean isOrderCreatedEvent(MarketplaceWebhookDTO payload);

    String extractOrderId(MarketplaceWebhookDTO payload);

    MarketplaceOrderDTO fetchOrder(String orderId);
}
