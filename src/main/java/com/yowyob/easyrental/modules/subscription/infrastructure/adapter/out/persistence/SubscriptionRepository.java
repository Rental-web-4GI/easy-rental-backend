package com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface SubscriptionRepository extends R2dbcRepository<SubscriptionEntity, UUID> {

    /**
     * Récupère l'historique complet des souscriptions d'une organisation, 
     * de la plus récente à la plus ancienne.
     */
    Flux<SubscriptionEntity> findAllByOrganizationIdOrderByStartDateDesc(UUID organizationId);

    /**
     * Récupère la souscription actuellement active pour une organisation.
     */
    Mono<SubscriptionEntity> findFirstByOrganizationIdAndStatus(UUID organizationId, String status);

    /**
     * Récupère toutes les souscriptions d'un certain type (ex: pour des statistiques).
     */
    Flux<SubscriptionEntity> findAllByPlanType(String planType);
}