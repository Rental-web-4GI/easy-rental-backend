package com.yowyob.easyrental.modules.staff.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.staff.dto.StaffRequestDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffUpdateDTO;
import com.yowyob.easyrental.modules.staff.domain.port.in.StaffUseCase;
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
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Tag(name = "Staff Management", description = "Gestion du personnel")
@SecurityRequirement(name = "bearerAuth")
public class StaffController {

    private final StaffUseCase staffUseCase;

    @Operation(summary = "Ajouter un membre au staff d'une organisation")
    @PostMapping("/org/{orgId}")
    @NotNull
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'staff:create')")
    // @PreAuthorize("@rbac.hasPermission(#orgId, 'staff:create')")
    public Mono<ResponseEntity<StaffResponseDTO>> create(@PathVariable UUID orgId,
            @RequestBody StaffRequestDTO request) {
        return staffUseCase.addStaffToOrganization(orgId, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Lister tout le staff d'une organisation")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'staff:list') or hasRole('ADMIN')")
    public Flux<StaffResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return staffUseCase.getStaffByOrganization(orgId);
    }

    @Operation(summary = "Lister le staff d'une agence")
    @GetMapping("/agency/{agencyId}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'staff:list') or hasRole('STAFF')")
    public Flux<StaffResponseDTO> getByAgency(@PathVariable UUID agencyId) {
        return staffUseCase.getStaffByAgency(agencyId);
    }

    @Operation(summary = "Obtenir les détails d'un membre du staff")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'staff:update')")
    public Mono<ResponseEntity<StaffResponseDTO>> getById(@PathVariable UUID id) {
        return staffUseCase.getStaffById(id).map(ResponseEntity::ok);
    }

    // Dans StaffController.java

    @Operation(summary = "Modifier les informations d'un membre du personnel")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.canAccessStaffMember(#id, 'staff:update')")
    public Mono<ResponseEntity<StaffResponseDTO>> update(
            @PathVariable UUID id,
            @RequestBody StaffUpdateDTO request) {
        return staffUseCase.updateStaff(id, request)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer (désactiver) un membre du staff")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION') or @rbac.hasPermission(#orgId, 'staff:delete')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return staffUseCase.deleteStaff(id).then(Mono.just(ResponseEntity.noContent().build()));
    }
}