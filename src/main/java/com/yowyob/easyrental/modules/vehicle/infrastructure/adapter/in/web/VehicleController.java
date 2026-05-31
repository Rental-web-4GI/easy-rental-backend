package com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.driver.application.DriverUseCaseImpl;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleRequestDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleResponseDTO;
import com.yowyob.easyrental.modules.vehicle.application.VehicleUseCaseImpl;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleDetailResponseDTO;
import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle Management")
@SecurityRequirement(name = "bearerAuth")
public class VehicleController {

    private final VehicleUseCaseImpl vehicleUseCaseImpl;
    private final DriverUseCaseImpl driverUseCaseImpl;

    @Operation(summary = "Ajouter un véhicule à la flotte (Vérifie les quotas)")
    @NotNull
    @PostMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'vehicle:create')")
    public Mono<ResponseEntity<VehicleResponseDTO>> create(@PathVariable UUID orgId, @RequestBody VehicleRequestDTO request) {
        return vehicleUseCaseImpl.createVehicle(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister tous les véhicules d'une organisation")
    @GetMapping("/org/{orgId}")
    public Flux<VehicleResponseDTO> getAllByOrg(@PathVariable UUID orgId) {
        return vehicleUseCaseImpl.getVehiclesByOrg(orgId);
    }

    @Operation(summary = "Lister les véhicules d'une agence")
    @GetMapping("/agency/{agencyId}")
    public Flux<VehicleResponseDTO> getAllByAgency(@PathVariable UUID agencyId) {
        return vehicleUseCaseImpl.getVehiclesByAgency(agencyId);
    }

    @Operation(summary = "Lister tous les véhicules disponibles sur la plateforme")
    @GetMapping("/available")
    public Flux<VehicleResponseDTO> getAvailableVehicles() {
        return vehicleUseCaseImpl.getAvailableVehicles();
    }

    // Route publique pour rechercher des véhicules
    @Operation(summary = "Rechercher des véhicules disponibles (Client)")
    @GetMapping("/search")
    public Flux<VehicleResponseDTO> searchVehicles(
            @RequestParam(required = false) UUID agencyId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword) {
        return vehicleUseCaseImpl.searchAvailableVehicles(agencyId, categoryId, keyword);
    }

    // Route publique pour lister les véhicules disponibles d'une agence
    @Operation(summary = "Lister les véhicules disponibles d'une agence (Client)")
    @GetMapping("/agency/{agencyId}/available")
    public Flux<VehicleResponseDTO> getAvailableVehiclesByAgency(@PathVariable UUID agencyId) {
        return vehicleUseCaseImpl.getAvailableVehiclesByAgency(agencyId);
    }

    @Operation(summary = "Trouver les chauffeurs disponibles pour une agence sur une période")
    @GetMapping("/drivers/available")
    public Flux<DriverResponseDTO> getAvailableDriversForBooking(
            @Parameter(description = "ID de l'agence") @RequestParam UUID agencyId,
            @Parameter(description = "Date de début (ISO-8601)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin (ISO-8601)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        return driverUseCaseImpl.getAvailableDrivers(agencyId, startDate, endDate);
    }

    @Operation(summary = "Obtenir les détails complets (Planning + Prix + evaluation) d'un véhicule")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<VehicleDetailResponseDTO>> getVehicleDetails(@PathVariable UUID id) {
        return vehicleUseCaseImpl.getVehicleDetails(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour le prix de location du véhicule")
    @PutMapping("/{id}/pricing")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<VehicleDetailResponseDTO>> updatePricing(
            @PathVariable UUID id,
            @RequestBody PricingUpdateDTO request) {
        return vehicleUseCaseImpl.updateVehiclePricing(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Ajouter des indisponibilités (Planning) au véhicule")
    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<VehicleDetailResponseDTO>> updateSchedule(
            @PathVariable UUID id,
            @RequestBody ScheduleUpdateDTO request) {
        return vehicleUseCaseImpl.updateVehicleSchedules(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtenir les détails d'un véhicule par son ID")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<VehicleResponseDTO>> getById(@PathVariable UUID id) {
        return vehicleUseCaseImpl.getVehicleById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour les informations d'un véhicule")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF') or @rbac.hasPermission(#orgId, 'vehicle:update')")
    public Mono<ResponseEntity<VehicleResponseDTO>> update(@PathVariable UUID id, @RequestBody VehicleRequestDTO request) {
        return vehicleUseCaseImpl.updateVehicle(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Changer le statut du véhicule (MAINTENANCE, AVAILABLE, RENTED)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF') or @rbac.hasPermission(#orgId, 'vehicle:update')")
    public Mono<ResponseEntity<VehicleResponseDTO>> updateStatus(@PathVariable UUID id, @RequestParam String status) {
        return vehicleUseCaseImpl.updateVehicleStatus(id, status).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer un véhicule (Met à jour les compteurs)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'vehicle:delete')")
     public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return vehicleUseCaseImpl.deleteVehicle(id).then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Operation(summary = "Lister les véhicules d'une organisation filtrés par catégorie")
    @GetMapping("/org/{orgId}/category/{categoryId}")
    @PreAuthorize("@rbac.hasPermission(#orgId, 'vehicle:list')")
    public Flux<VehicleResponseDTO> getByOrgAndCategory(
            @PathVariable UUID orgId,
            @PathVariable UUID categoryId) {
        return vehicleUseCaseImpl.getVehiclesByOrgAndCategory(orgId, categoryId);
    }

    @Operation(summary = "Lister les véhicules d'une agence filtrés par catégorie")
    @GetMapping("/agency/{agencyId}/category/{categoryId}")
    @PreAuthorize("@rbac.canAccessAgency(#agencyId, 'vehicle:list')")
    public Flux<VehicleResponseDTO> getByAgencyAndCategory(
            @PathVariable UUID agencyId,
            @PathVariable UUID categoryId) {
        return vehicleUseCaseImpl.getVehiclesByAgencyAndCategory(agencyId, categoryId);
    }
}
