package com.yowyob.easyrental.modules.notification.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO;
import com.yowyob.easyrental.modules.notification.application.NotificationUseCaseImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestion des notifications Client, Agence et Organisation")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationUseCaseImpl notificationUseCaseImpl;

    @Operation(summary = "Lister les notifications d'un client")
    @GetMapping("/client/{clientId}")
    public Flux<NotificationResponseDTO> getClientNotifications(@PathVariable UUID clientId) {
        return notificationUseCaseImpl.getClientNotifications(clientId);
    }

    @Operation(summary = "Compter les notifications non lues d'un client")
    @GetMapping("/client/{clientId}/unread/count")
    public Mono<Long> countUnreadClient(@PathVariable UUID clientId) {
        return notificationUseCaseImpl.countUnreadClient(clientId);
    }

    @Operation(summary = "Lister les notifications d'une agence")
    @GetMapping("/agency/{agencyId}")
    public Flux<NotificationResponseDTO> getAgencyNotifications(@PathVariable UUID agencyId) {
        return notificationUseCaseImpl.getAgencyNotifications(agencyId);
    }

    @Operation(summary = "Compter les notifications non lues d'une agence")
    @GetMapping("/agency/{agencyId}/unread/count")
    public Mono<Long> countUnreadAgency(@PathVariable UUID agencyId) {
        return notificationUseCaseImpl.countUnreadAgency(agencyId);
    }

    @Operation(summary = "Lister les notifications d'une organisation (Toutes les agences)")
    @GetMapping("/org/{orgId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Flux<NotificationResponseDTO> getOrgNotifications(@PathVariable UUID orgId) {
        return notificationUseCaseImpl.getOrganizationNotifications(orgId);
    }

    @Operation(summary = "Compter les notifications non lues d'une organisation")
    @GetMapping("/org/{orgId}/unread/count")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<Long> countUnreadOrg(@PathVariable UUID orgId) {
        return notificationUseCaseImpl.countUnreadOrganization(orgId);
    }

    @Operation(summary = "Marquer une notification comme lue")
    @PutMapping("/{id}/read")
    public Mono<ResponseEntity<Void>> markAsRead(@PathVariable UUID id) {
        return notificationUseCaseImpl.markAsRead(id)
                .then(Mono.just(ResponseEntity.ok().build()));
    }
}
