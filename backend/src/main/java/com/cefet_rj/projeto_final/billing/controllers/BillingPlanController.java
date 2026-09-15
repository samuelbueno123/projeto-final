package com.cefet_rj.projeto_final.billing.controllers;

import com.cefet_rj.projeto_final.billing.dtos.BillingPlanDTO;
import com.cefet_rj.projeto_final.billing.dtos.BillingPlanRequestDTO;
import com.cefet_rj.projeto_final.billing.services.BillingPlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/billing/plans")
@RequiredArgsConstructor
@Tag(
		name = "Billing Plans",
		description = "Planos comerciais e funcionalidades contratadas"
)
public class BillingPlanController {

	private final BillingPlanService billingPlanService;

	@GetMapping
	public ResponseEntity<List<BillingPlanDTO>> findAll() {
		return ResponseEntity.ok(
				billingPlanService.findAll()
		);
	}

	@GetMapping("/{id}")
	public ResponseEntity<BillingPlanDTO> findById(
			@PathVariable Long id
	) {
		return ResponseEntity.ok(
				billingPlanService.findDTOById(id)
		);
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BillingPlanDTO> create(
			@Valid
			@RequestBody BillingPlanRequestDTO dto
	) {

		BillingPlanDTO created =
				billingPlanService.create(dto);

		return ResponseEntity
				.status(HttpStatus.CREATED)
				.body(created);
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<BillingPlanDTO> update(
			@PathVariable Long id,
			@Valid
			@RequestBody BillingPlanRequestDTO dto
	) {

		return ResponseEntity.ok(
				billingPlanService.update(id, dto)
		);
	}

	@PatchMapping("/{id}/activate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> activate(
			@PathVariable Long id
	) {

		billingPlanService.activate(id);

		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{id}/deactivate")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> deactivate(
			@PathVariable Long id
	) {

		billingPlanService.deactivate(id);

		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{plan}/monthly-fee")
	public ResponseEntity<BigDecimal> monthlyFee(
			@PathVariable String plan
	) {

		return ResponseEntity.ok(
				billingPlanService
						.calculateMonthlyFee(plan)
		);
	}
}