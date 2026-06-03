package com.yowyob.easyrental.modules.agency.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.agency.dto.AgencyRequestDTO;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import com.yowyob.easyrental.modules.agency.domain.port.in.AgencyUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/agencies")
@RequiredArgsConstructor
@Tag(name = "Agency Management", description = "CRUD pour les agences des organisations")
@SecurityRequirement(name = "bearerAuth")
public class AgencyController {

    private final AgencyUseCase agencyUseCase;

    @Operation(summary = "Lister toutes les agences de la plateforme")
    @GetMapping("/all")
    public Flux<AgencyResponseDTO> getAll() {
        return agencyUseCase.getAllAgencies();
    }

    // NOUVEAU : Route publique pour rechercher des agences
    @Operation(summary = "Rechercher des agences (Client)")
    @GetMapping("/search")
    public Flux<AgencyResponseDTO> searchAgencies(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String city) {
        return agencyUseCase.searchAgencies(keyword, city);
    }

    // NOUVEAU : Route publique pour voir les détails d'une agence
    @Operation(summary = "Détails d'une agence (Client)")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<AgencyResponseDTO>> getAgencyDetailsClient(@PathVariable UUID id) {
        return agencyUseCase.getAgency(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les agences d'une organisation")
    @GetMapping("/org/{orgId}")
    public Flux<AgencyResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return agencyUseCase.getAgenciesByOrg(orgId);
    }

    @Operation(summary = "Obtenir les détails d'une agence (Admin/Org)")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AgencyResponseDTO>> getById(@PathVariable UUID id) {
        return agencyUseCase.getAgency(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Créer une nouvelle agence pour une organisation")
    @PostMapping("/org/{orgId}")
    @NotNull
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<AgencyResponseDTO>> create(
            @PathVariable UUID orgId,
            @RequestBody AgencyRequestDTO request) {
        return agencyUseCase.createAgency(orgId, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Modifier une agence")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#id, 'agency:update')")
    public Mono<ResponseEntity<AgencyResponseDTO>> update(
            @PathVariable UUID id,
            @RequestBody AgencyRequestDTO request) {
        return agencyUseCase.updateAgency(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer une agence")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.canAccessAgency(#id, 'agency:delete')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return agencyUseCase.deleteAgency(id).then(Mono.just(ResponseEntity.noContent().build()));
    }
}
