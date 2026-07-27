package com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

public interface OrganizationRepository extends R2dbcRepository<OrganizationEntity, UUID> {
    Flux<OrganizationEntity> findAllBySubscriptionPlanId(UUID subscriptionPlanId);
    Mono<OrganizationEntity> findByOwnerId(UUID ownerId);
    Mono<OrganizationEntity> findByKernelOrganizationId(UUID kernelOrganizationId);

    @Query("SELECT * FROM organizations WHERE subscription_expires_at IS NOT NULL "
            + "AND subscription_expires_at < :threshold")
    Flux<OrganizationEntity> findAllExpiredBefore(LocalDateTime threshold);

    Mono<Long> countByAccountType(String accountType);

    Mono<Long> countByGovernanceStatus(String governanceStatus);

    Mono<Long> countByStatus(String status);
}
