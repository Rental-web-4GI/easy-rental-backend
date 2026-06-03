package com.yowyob.easyrental.modules.poste.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.poste.domain.port.in.PosteUseCase;
import com.yowyob.easyrental.modules.poste.dto.PosteRequestDTO;
import com.yowyob.easyrental.modules.poste.dto.PosteResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/postes")
@RequiredArgsConstructor
@Tag(name = "Poste & Roles", description = "Gestion des postes et permissions employés")
@SecurityRequirement(name = "bearerAuth")
public class PosteController {

    private final PosteUseCase posteUseCase;

    @Operation(summary = "Lister les postes d'une organisation")
    @GetMapping("/org/{orgId}/postes")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<PosteResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return posteUseCase.getAvailablePostes(orgId);
    }

    @Operation(summary = "Créer un nouveau poste avec permissions")
    @PostMapping("/org/{orgId}/poste")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<PosteResponseDTO>> create(
            @PathVariable UUID orgId,
            @RequestBody PosteRequestDTO request) {
        return posteUseCase.createPoste(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtenir les détails et permissions d'un poste")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<PosteResponseDTO>> getById(@PathVariable UUID id) {
        return posteUseCase.getPosteById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour un poste")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<PosteResponseDTO>> update(@PathVariable UUID id, @RequestBody PosteRequestDTO request) {
        return posteUseCase.updatePoste(id, request).map(ResponseEntity::ok);
    }
}
