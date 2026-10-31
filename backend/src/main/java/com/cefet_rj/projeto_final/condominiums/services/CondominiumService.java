package com.cefet_rj.projeto_final.condominiums.services;

import com.cefet_rj.projeto_final.auth.security.AuthenticatedUserService;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.condominiums.dtos.CondominiumDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.insert.CondominiumInsertDTO;
import com.cefet_rj.projeto_final.condominiums.dtos.update.CondominiumUpdateDTO;
import com.cefet_rj.projeto_final.condominiums.entities.Condominium;
import com.cefet_rj.projeto_final.condominiums.repositories.CondominiumRepository;
import com.cefet_rj.projeto_final.condominiums.security.CondominiumAccessService;
import com.cefet_rj.projeto_final.shared.exceptions.DatabaseException;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import com.cefet_rj.projeto_final.users.entities.User;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CondominiumService {

    private final CondominiumRepository condominiumRepository;
    private final BillingPlanRepository billingPlanRepository;
    private final FeatureRepository featureRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final CondominiumAccessService condominiumAccessService;

    @Transactional(readOnly = true)
    public Page<CondominiumDTO> findAllPaged(Pageable pageable) {
        Page<Condominium> list = condominiumRepository.findAll(pageable);
        return list.map(CondominiumDTO::new);
    }

    @Transactional(readOnly = true)
    public CondominiumDTO findById(Long id) {
        Optional<Condominium> obj = condominiumRepository.findById(id);
        Condominium entity = obj.orElseThrow(() -> new ResourceNotFoundException("Entity not found"));
        User user = authenticatedUserService.getAuthenticatedUser();
        if (!user.hasRole("ROLE_ADMIN")) {
            condominiumAccessService.validateUserBelongsToCondominium(user.getId(), id);
        }
        return new CondominiumDTO(entity);
    }

    @Transactional
    public CondominiumDTO insert(CondominiumInsertDTO dto) {
        Condominium entity = new Condominium();
        linkAssociations(dto.name(), dto.address(), dto.billingPlan(), dto.isActive(), entity);
        entity = condominiumRepository.save(entity);
        return new CondominiumDTO(entity);
    }

    @Transactional
    public CondominiumDTO update(Long id, CondominiumUpdateDTO dto) {
        try {
            Condominium entity = condominiumRepository.getReferenceById(id);
            linkAssociations(dto.name(), dto.address(), dto.billingPlan(), dto.isActive(), entity);
            entity = condominiumRepository.save(entity);
            return new CondominiumDTO(entity);
        } catch (EntityNotFoundException e) {
            throw new ResourceNotFoundException("Id " + id + " not found");
        }
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public void delete(Long id) {
        if (!condominiumRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource not found");
        }
        try {
            condominiumRepository.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new DatabaseException("Integrity violation");
        }
    }

    private BillingPlan getBillingPlanOrThrow(String billingPlanName) {
        String normalized = billingPlanName == null ? "Básico" : billingPlanName.trim();
        if (normalized.isEmpty()) {
            normalized = "Básico";
        }
        final String targetName = normalized;
        return billingPlanRepository
                .findByNameIgnoreCase(targetName)
                .orElseGet(() -> {
                    BillingPlan created = new BillingPlan();
                    created.setName(targetName);
                    created.setActive(true);
                    created.setMonthlyFee(defaultFeeForPlan(targetName));
                    created.setFeatures(new HashSet<>(featureRepository.findAll()));
                    return billingPlanRepository.save(created);
                });
    }

    private BigDecimal defaultFeeForPlan(String planName) {
        String lower = planName.toLowerCase(Locale.ROOT);
        if (lower.contains("premium") || lower.contains("enterprise")) {
            return new BigDecimal("499.90");
        }
        if (lower.contains("profissional") || lower.contains("pro") || lower.contains("standard")) {
            return new BigDecimal("349.90");
        }
        return new BigDecimal("199.90");
    }

    private void linkAssociations(
            String name,
            String address,
            String billingPlan,
            Boolean isActive,
            Condominium entity
    ) {
        entity.setName(name.trim());
        entity.setAddress(address.trim());

        BillingPlan plan = getBillingPlanOrThrow(billingPlan);

        entity.setBillingPlan(plan);

        entity.setIsActive(isActive != null ? isActive : Boolean.TRUE);
    }
}
