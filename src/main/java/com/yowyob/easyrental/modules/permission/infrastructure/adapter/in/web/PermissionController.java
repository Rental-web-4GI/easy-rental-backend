package com.yowyob.easyrental.modules.permission.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.permission.domain.port.in.PermissionUseCase;
import com.yowyob.easyrental.modules.permission.dto.PermissionResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Tag(name = "Permissions Metadata", description = "Liste des permissions système disponibles")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final PermissionUseCase permissionUseCase;

    @Operation(summary = "Lister toutes les permissions disponibles")
    @GetMapping
    public Flux<PermissionResponseDTO> getAllPermissions() {
        return permissionUseCase.getAllPermissions();
    }
}
