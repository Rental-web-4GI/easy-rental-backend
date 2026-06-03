package com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.vehicle.domain.port.in.CategoryUseCase;
import com.yowyob.easyrental.modules.vehicle.dto.CategoryRequestDTO;
import com.yowyob.easyrental.modules.vehicle.dto.CategoryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles/categories")
@RequiredArgsConstructor
@Tag(name = "Vehicle Category Management", description = "Gestion des catégories de véhicules (SUV, Berline, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryUseCase categoryUseCase;

    @Operation(summary = "Créer une catégorie de véhicule")
    @PostMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<CategoryResponseDTO>> create(
            @PathVariable UUID orgId,
            @RequestBody CategoryRequestDTO request) {
        return categoryUseCase.create(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister les catégories de véhicules d'une organisation (Org + Système)")
    @GetMapping("/org/{orgId}")
    public Flux<CategoryResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return categoryUseCase.getByOrg(orgId);
    }

    @Operation(summary = "Lister toutes les catégories de véhicules (plateforme)")
    @GetMapping("/all")
    public Flux<CategoryResponseDTO> getAllCategories() {
        return categoryUseCase.getAllCategories();
    }

    @Operation(summary = "Lister toutes les catégories utilisables par une agence (Org + Système)")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or ADMIN")
    public Flux<CategoryResponseDTO> getByAgency(@PathVariable UUID agencyId) {
        return categoryUseCase.getByAgency(agencyId);
    }

    @Operation(summary = "Obtenir les détails d'une catégorie")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CategoryResponseDTO>> getById(@PathVariable UUID id) {
        return categoryUseCase.getById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer une catégorie (Uniquement si elle appartient à l'organisation)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') ")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return categoryUseCase.delete(id).then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Operation(summary = "Mettre à jour une catégorie de véhicule")
    @PutMapping("/{id}")
    @PreAuthorize("@rbac.canAccessCategory(#id, 'vehiclecategory:update')")
    public Mono<ResponseEntity<CategoryResponseDTO>> update(
            @PathVariable UUID id,
            @RequestBody CategoryRequestDTO request) {
        return categoryUseCase.update(id, request).map(ResponseEntity::ok);
    }
}
