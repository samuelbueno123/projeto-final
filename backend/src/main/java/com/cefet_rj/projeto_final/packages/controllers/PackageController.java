package com.cefet_rj.projeto_final.packages.controllers;

import com.cefet_rj.projeto_final.packages.dtos.PackageDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageInsertDTO;
import com.cefet_rj.projeto_final.packages.dtos.PackageUpdateDTO;
import com.cefet_rj.projeto_final.packages.services.PackageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
@Tag(
        name = "Packages",
        description = "Gerenciamento e ciclo de vida de encomendas"
)
public class PackageController {

    private final PackageService packageService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @GetMapping
    public ResponseEntity<Page<PackageDTO>> findAll(
            @RequestParam Long condominiumId,
            @RequestParam(value = "search", defaultValue = "") String search,
            @RequestParam(value = "status", defaultValue = "") String status,
            Pageable pageable
    ) {
        Page<PackageDTO> list = packageService.findAllPagedByCondominium(
                condominiumId,
                search,
                status,
                pageable
        );

        return ResponseEntity.ok().body(list);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<PackageDTO> findById(@PathVariable Long id){
        PackageDTO dto = packageService.findById(id);
        return ResponseEntity.ok().body(dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my")
    public ResponseEntity<Page<PackageDTO>> findMyPackages(Pageable pageable) {
        Page<PackageDTO> list = packageService.findMyPackages(pageable);
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @PostMapping
    public ResponseEntity<PackageDTO> insert(@Valid @RequestBody PackageInsertDTO dto){
        PackageDTO newDto = packageService.registerReceived(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(newDto.id()).toUri();

        return ResponseEntity.created(uri).body(newDto);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/expected")
    public ResponseEntity<PackageDTO> insertExpected(@Valid @RequestBody PackageInsertDTO dto) {
        PackageDTO newDto = packageService.registerExpected(dto);

        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/packages/{id}")
                .buildAndExpand(newDto.id())
                .toUri();

        return ResponseEntity.created(uri).body(newDto);
    }

	@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
	@PutMapping ("/{id}")
	public ResponseEntity<PackageDTO> update(@PathVariable Long id, @Valid @RequestBody PackageUpdateDTO dto){
		PackageDTO newDto = packageService.update(id, dto);
		return ResponseEntity.ok().body(newDto);
	}

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        packageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @PatchMapping("/{id}/receive")
    @Operation(
            summary = "Marcar encomenda como recebida",
            description = "Transiciona uma encomenda para o status RECEIVED."
    )
    public ResponseEntity<PackageDTO> receive(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.receive(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @PatchMapping("/{id}/notify")
    @Operation(
            summary = "Marcar morador como notificado",
            description = "Transiciona a encomenda para NOTIFIED e pode gerar notificação conforme o plano do condomínio."
    )
    public ResponseEntity<PackageDTO> notify(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.notify(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @PatchMapping("/{id}/pickup")
    @Operation(
            summary = "Registrar retirada",
            description = "Marca a encomenda como retirada pelo destinatário."
    )
    public ResponseEntity<PackageDTO> pickUp(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.pickUp(id));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{id}/request-pickup")
    @Operation(
            summary = "Solicitar separação para retirada na portaria",
            description = "Morador avisa a portaria que irá retirar a encomenda ou autoriza terceiro."
    )
    public ResponseEntity<PackageDTO> requestPickup(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(packageService.requestPickup(id, note));
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<PackageDTO> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.cancel(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'DOORMAN')")
    @PatchMapping("/{id}/return")
    public ResponseEntity<PackageDTO> returnPkg(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.returnPkg(id));
    }
}
