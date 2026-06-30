package com.cefet_rj.projeto_final.dtos.marketplace;

public record MarketplaceOrderDTO(
        String marketplaceCode,
        String externalOrderId,
        String description,
        String buyerEmail,
        Long recipientId,
        Long unitId
) {
}
