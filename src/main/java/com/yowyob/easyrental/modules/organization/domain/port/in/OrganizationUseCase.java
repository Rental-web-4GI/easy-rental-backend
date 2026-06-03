package com.yowyob.easyrental.modules.organization.domain.port.in;

import com.yowyob.easyrental.modules.organization.dto.OrgResponseDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUpdateDTO;
import com.yowyob.easyrental.modules.organization.dto.OrgUserResponseDTO;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import java.util.UUID;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for organization use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface OrganizationUseCase {
    Mono<OrgResponseDTO> getOrganization(UUID id);
    Flux<OrgResponseDTO> getAllOrganizations();
    Mono<OrgResponseDTO> updateOrganization(UUID id, OrgUpdateDTO request);
    Mono<OrgResponseDTO> updateOrganizationWithMedia(
            UUID id,
            OrgUpdateDTO request,
            FilePart logoFile,
            FilePart licenseFile);
    Mono<Boolean> validateQuota(UUID orgId, String resourceType);
    Mono<OrgUserResponseDTO> getCurrentOrgAndUser();
    Flux<OrgResponseDTO> getOrganizationsByPlan(UUID planId);
    Mono<SubscriptionResponseDTO> toggleAutoRenewWithResponse(UUID orgId, boolean enabled);
    Mono<SubscriptionResponseDTO> getOrgSubscriptionStatus(UUID orgId);
    Mono<SubscriptionResponseDTO> upgradePlanWithResponse(UUID orgId, String planName);
    Mono<Void> updateAgencyCounter(UUID orgId, int increment);
    Mono<Void> updateStaffCounter(UUID orgId, int increment);
    Mono<Void> updateVehicleCounter(UUID orgId, int increment);
    Mono<Void> updateDriverCounter(UUID orgId, int increment);
}
