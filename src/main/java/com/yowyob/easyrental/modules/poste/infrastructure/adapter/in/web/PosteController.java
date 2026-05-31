package com.yowyob.easyrental.modules.poste.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.poste.dto.PosteRequestDTO;
import com.yowyob.easyrental.modules.poste.dto.PosteResponseDTO;
import com.yowyob.easyrental.modules.poste.application.PosteUseCaseImpl;
import com.yowyob.easyrental.modules.poste.infrastructure.adapter.out.persistence.PosteRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/postes")
@RequiredArgsConstructor
@Tag(name = "Poste & Roles", description = "Gestion des postes et permissions employés")
@SecurityRequirement(name = "bearerAuth")
public class PosteController {

    private final PosteUseCaseImpl posteUseCaseImpl;
    private final PosteRepository PosteRepository;

    @Operation(summary = "Lister les postes d'une organisation")
    @GetMapping("/org/{orgId}/postes")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<PosteResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return posteUseCaseImpl.getAvailablePostes(orgId);
    }

    @Operation(summary = "Créer un nouveau poste avec permissions")
    @PostMapping("/org/{orgId}/poste")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<PosteResponseDTO>> create(@PathVariable UUID orgId, @RequestBody PosteRequestDTO request) {
        return posteUseCaseImpl.createPoste(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtenir les détails et permissions d'un poste")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<PosteResponseDTO>> getById(@PathVariable UUID id) {
        return posteUseCaseImpl.getPosteById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour un poste")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<PosteResponseDTO>> update(@PathVariable UUID id, @RequestBody PosteRequestDTO request) {
        return PosteRepository.findById(Objects.requireNonNull(id))
                .flatMap(poste -> {
                    // Si l'organizationId est NULL, c'est un poste système : INTERDIT de modifier
                    if (poste.getOrganizationId() == null) {
                        return Mono.error(new RuntimeException("Impossible de modifier un poste par défaut du système"));
                    }
                    return posteUseCaseImpl.updatePoste(id, request);
                })
                .map(ResponseEntity::ok);
    }
}
