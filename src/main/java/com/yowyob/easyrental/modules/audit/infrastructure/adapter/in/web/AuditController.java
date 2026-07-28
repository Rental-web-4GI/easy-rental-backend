package com.yowyob.easyrental.modules.audit.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.audit.domain.AuditEventEntity;
import com.yowyob.easyrental.modules.audit.domain.port.in.AuditUseCase;
import com.yowyob.easyrental.modules.audit.dto.AuditEventResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Admin-only endpoint to browse the audit journal.
 *
 * @author Easy Rental Team
 * @since 2026-07-24
 */
@RestController
@RequestMapping("/api/admin/audit-events")
@RequiredArgsConstructor
@Tag(name = "Admin - Audit", description = "Journal d'audit (auth events, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditUseCase auditUseCase;

    @Operation(summary = "Rechercher les événements d'audit")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<AuditEventResponseDTO> search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return auditUseCase.search(userId, action, from, to, page, size)
                .map(this::toDto);
    }

    private AuditEventResponseDTO toDto(AuditEventEntity entity) {
        return new AuditEventResponseDTO(
                entity.getId(),
                entity.getUserId(),
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getIp(),
                entity.getUserAgent(),
                entity.getMetadata(),
                entity.getCreatedAt());
    }
}
