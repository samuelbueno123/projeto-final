package com.cefet_rj.projeto_final.assemblies.controllers;

import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.AssemblyTopicDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CastVoteDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CreateAssemblyDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.CreateTopicDTO;
import com.cefet_rj.projeto_final.assemblies.dtos.AssemblyDTO.UpdateAssemblyStatusDTO;
import com.cefet_rj.projeto_final.assemblies.services.AssemblyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AssemblyController {

    private final AssemblyService assemblyService;

    @GetMapping("/condominiums/{condominiumId}/assemblies")
    public ResponseEntity<List<AssemblyDTO>> listAssemblies(@PathVariable Long condominiumId) {
        return ResponseEntity.ok(assemblyService.listAssemblies(condominiumId));
    }

    @GetMapping("/assemblies/{id}")
    public ResponseEntity<AssemblyDTO> getAssembly(@PathVariable Long id) {
        return ResponseEntity.ok(assemblyService.getAssembly(id));
    }

    @PostMapping("/condominiums/{condominiumId}/assemblies")
    public ResponseEntity<AssemblyDTO> createAssembly(
            @PathVariable Long condominiumId,
            @Valid @RequestBody CreateAssemblyDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assemblyService.createAssembly(condominiumId, dto));
    }

    @PostMapping("/assemblies/{id}/topics")
    public ResponseEntity<AssemblyTopicDTO> addTopic(
            @PathVariable Long id,
            @Valid @RequestBody CreateTopicDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assemblyService.addTopic(id, dto));
    }

    @PatchMapping("/assembly-topics/{topicId}/voting")
    public ResponseEntity<AssemblyTopicDTO> toggleVoting(
            @PathVariable Long topicId,
            @RequestParam boolean open) {
        return ResponseEntity.ok(assemblyService.toggleTopicVoting(topicId, open));
    }

    @PostMapping("/assembly-topics/{topicId}/votes")
    public ResponseEntity<AssemblyTopicDTO> castVote(
            @PathVariable Long topicId,
            @Valid @RequestBody CastVoteDTO dto) {
        return ResponseEntity.ok(assemblyService.castVote(topicId, dto));
    }

    @PatchMapping("/assemblies/{id}/status")
    public ResponseEntity<AssemblyDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssemblyStatusDTO dto) {
        return ResponseEntity.ok(assemblyService.updateAssemblyStatus(id, dto));
    }
}
