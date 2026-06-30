package com.cefet_rj.projeto_final.services.marketplace;

import com.cefet_rj.projeto_final.dtos.insert.PackageInsertDTO;
import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceOrderDTO;
import com.cefet_rj.projeto_final.dtos.marketplace.MarketplaceWebhookDTO;
import com.cefet_rj.projeto_final.entities.enums.PackageStatus;
import com.cefet_rj.projeto_final.repositories.PackageRepository;
import com.cefet_rj.projeto_final.repositories.UnitRepository;
import com.cefet_rj.projeto_final.repositories.UserRepository;
import com.cefet_rj.projeto_final.services.PackageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MarketplaceWebhookService {

    private final Map<String, MarketplaceAdapter> adapters;
    private final PackageRepository packageRepository;
    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final PackageService packageService;
    private final MarketplaceProperties properties;

    public MarketplaceWebhookService(
            List<MarketplaceAdapter> adapters,
            PackageRepository packageRepository,
            UserRepository userRepository,
            UnitRepository unitRepository,
            PackageService packageService,
            MarketplaceProperties properties
    ) {
        this.adapters = adapters.stream().collect(Collectors.toMap(MarketplaceAdapter::marketplaceCode, Function.identity()));
        this.packageRepository = packageRepository;
        this.userRepository = userRepository;
        this.unitRepository = unitRepository;
        this.packageService = packageService;
        this.properties = properties;
    }

    @Transactional
    public void process(String marketplaceCode, MarketplaceWebhookDTO payload, Map<String, String> headers) {
        MarketplaceAdapter adapter = adapters.get(marketplaceCode);
        if (adapter == null) {
            throw new IllegalArgumentException("Marketplace não suportado: " + marketplaceCode);
        }
        if (!adapter.isValidWebhook(payload, headers)) {
            throw new IllegalArgumentException("Assinatura inválida para marketplace: " + marketplaceCode);
        }
        if (!adapter.isOrderCreatedEvent(payload)) {
            log.debug("Ignorando webhook {} que não é criação de pedido", marketplaceCode);
            return;
        }

        String orderId = adapter.extractOrderId(payload);
        if (orderId == null || orderId.isBlank()) {
            log.warn("Webhook {} sem id de pedido válido", marketplaceCode);
            return;
        }
        if (packageRepository.existsByMarketplaceCodeAndMarketplaceOrderId(marketplaceCode, orderId)) {
            log.info("Pedido {} do marketplace {} já possui encomenda cadastrada", orderId, marketplaceCode);
            return;
        }

        MarketplaceOrderDTO order = adapter.fetchOrder(orderId);
        packageService.insert(new PackageInsertDTO(
                order.description(),
                resolveRecipientId(marketplaceCode, order),
                resolveUnitId(marketplaceCode, order),
                PackageStatus.EXPECTED,
                null,
                null,
                marketplaceCode,
                order.externalOrderId()
        ));
        log.info("Encomenda criada para pedido {} do marketplace {}", order.externalOrderId(), marketplaceCode);
    }

    private Long resolveRecipientId(String marketplaceCode, MarketplaceOrderDTO order) {
        if (order.recipientId() != null && userRepository.existsById(order.recipientId())) return order.recipientId();
        if (order.buyerEmail() != null) {
            return userRepository.findByEmail(order.buyerEmail()).map(user -> user.getId()).orElseGet(() -> defaultRecipientId(marketplaceCode));
        }
        return defaultRecipientId(marketplaceCode);
    }

    private Long resolveUnitId(String marketplaceCode, MarketplaceOrderDTO order) {
        if (order.unitId() != null && unitRepository.existsById(order.unitId())) return order.unitId();
        Long unitId = store(marketplaceCode).getDefaultUnitId();
        if (unitId == null || !unitRepository.existsById(unitId)) {
            throw new IllegalStateException("Configure marketplaces.stores." + marketplaceCode + ".default-unit-id com uma unidade válida");
        }
        return unitId;
    }

    private Long defaultRecipientId(String marketplaceCode) {
        Long recipientId = store(marketplaceCode).getDefaultRecipientId();
        if (recipientId == null || !userRepository.existsById(recipientId)) {
            throw new IllegalStateException("Configure marketplaces.stores." + marketplaceCode + ".default-recipient-id com um usuário válido");
        }
        return recipientId;
    }

    private MarketplaceProperties.Store store(String marketplaceCode) {
        MarketplaceProperties.Store store = properties.getStore(marketplaceCode);
        if (store == null) throw new IllegalStateException("Marketplace não configurado: " + marketplaceCode);
        return store;
    }
}
