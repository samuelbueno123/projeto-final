package com.cefet_rj.projeto_final.billing.services;

import com.cefet_rj.projeto_final.billing.dtos.BillingPlanDTO;
import com.cefet_rj.projeto_final.billing.dtos.BillingPlanRequestDTO;
import com.cefet_rj.projeto_final.billing.entities.BillingPlan;
import com.cefet_rj.projeto_final.billing.entities.Feature;
import com.cefet_rj.projeto_final.billing.repositories.BillingPlanRepository;
import com.cefet_rj.projeto_final.billing.repositories.FeatureRepository;
import com.cefet_rj.projeto_final.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cefet_rj.projeto_final.billing.enums.FeatureCode;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BillingPlanService {

	private final BillingPlanRepository billingPlanRepository;
	private final FeatureRepository featureRepository;

	@Transactional(readOnly = true)
	public BillingPlan findByName(String name) {
		return billingPlanRepository
				.findByNameIgnoreCase(name)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								"Billing plan not found: " + name
						)
				);
	}

	@Transactional(readOnly = true)
	public BillingPlan findById(Long id) {
		return billingPlanRepository
				.findById(id)
				.orElseThrow(() ->
						new ResourceNotFoundException(
								"Billing plan not found: " + id
						)
				);
	}

	@Transactional(readOnly = true)
	public List<BillingPlanDTO> findAll() {
		return billingPlanRepository
				.findAll()
				.stream()
				.map(BillingPlanDTO::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public BillingPlanDTO findDTOById(Long id) {
		return new BillingPlanDTO(
				findById(id)
		);
	}

	@Transactional
	public BillingPlanDTO create(
			BillingPlanRequestDTO dto
	) {

		if (billingPlanRepository
				.findByNameIgnoreCase(dto.name())
				.isPresent()) {

			throw new IllegalArgumentException(
					"Billing plan already exists: "
							+ dto.name()
			);
		}

		BillingPlan plan = new BillingPlan();

		plan.setName(
				dto.name().trim().toUpperCase()
		);

		plan.setMonthlyFee(
				dto.monthlyFee()
		);

		plan.setActive(true);

		plan.setFeatures(
				resolveFeatures(dto.features())
		);

		plan = billingPlanRepository.save(plan);

		return new BillingPlanDTO(plan);
	}

	@Transactional
	public BillingPlanDTO update(
			Long id,
			BillingPlanRequestDTO dto
	) {

		BillingPlan plan = findById(id);

		billingPlanRepository
				.findByNameIgnoreCase(dto.name())
				.filter(existing ->
						!existing.getId().equals(id)
				)
				.ifPresent(existing -> {
					throw new IllegalArgumentException(
							"Billing plan already exists: "
									+ dto.name()
					);
				});

		plan.setName(
				dto.name().trim().toUpperCase()
		);

		plan.setMonthlyFee(
				dto.monthlyFee()
		);

		plan.setFeatures(
				resolveFeatures(dto.features())
		);

		plan = billingPlanRepository.save(plan);

		return new BillingPlanDTO(plan);
	}

	@Transactional
	public void deactivate(Long id) {

		BillingPlan plan = findById(id);

		plan.setActive(false);

		billingPlanRepository.save(plan);
	}

	@Transactional
	public void activate(Long id) {

		BillingPlan plan = findById(id);

		plan.setActive(true);

		billingPlanRepository.save(plan);
	}

	@Transactional(readOnly = true)
	public BigDecimal calculateMonthlyFee(
			String planName
	) {
		return findByName(planName)
				.getMonthlyFee();
	}

	private Set<Feature> resolveFeatures(
			Set<FeatureCode> featureCodes
	) {

		Set<Feature> features = new HashSet<>();

		for (FeatureCode featureCode : featureCodes) {

			Feature feature = featureRepository
					.findByCode(featureCode.name())
					.orElseThrow(() ->
							new ResourceNotFoundException(
									"Feature not found: "
											+ featureCode.name()
							)
					);

			features.add(feature);
		}

		return features;
	}
}