package com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
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

    Mono<Long> countByStatus(String status);

    /**
     * Nombre d'organisations avec au moins une souscription ACTIVE.
     * Compte DISTINCT car des données historiques peuvent avoir plusieurs
     * subs ACTIVE pour une même org (upgrades sans nettoyage).
     */
    @Query("SELECT COUNT(DISTINCT organization_id) FROM subscriptions WHERE status = 'ACTIVE'")
    Mono<Long> countActiveOrganizations();

    /**
     * MRR plateforme : somme des prix des plans dont le nom correspond au planType
     * des souscriptions actives (statut = ACTIVE). Inclut les doublons pour être
     * fidèle à ce qui est vraiment facturable.
     */
    @Query("""
            SELECT COALESCE(SUM(sp.price), 0) FROM subscriptions s
            JOIN subscription_plans sp ON sp.name = s.plan_type
            WHERE s.status = 'ACTIVE'
            """)
    Mono<BigDecimal> sumActivePlanPrices();
}