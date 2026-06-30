package com.cefet_rj.projeto_final.services.marketplace.adapters;

import com.cefet_rj.projeto_final.services.marketplace.MarketplaceOrderClient;
import com.cefet_rj.projeto_final.services.marketplace.MarketplaceProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AmericanasAdapter extends AbstractMarketplaceAdapter {
    public AmericanasAdapter(MarketplaceOrderClient orderClient, MarketplaceProperties properties) { super(orderClient, properties); }
    @Override public String marketplaceCode() { return "americanas"; }
    @Override protected List<String> orderCreatedEvents() { return List.of("order.created", "orders", "created"); }
    @Override protected String displayName() { return "Americanas"; }
}
