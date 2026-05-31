package com.yowyob.easyrental.modules.review.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.review.domain.ReviewEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ReviewRepository extends R2dbcRepository<ReviewEntity, UUID> {

    Flux<ReviewEntity> findAllByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    // Calcul de la moyenne directement en base
    @Query("SELECT AVG(rating) FROM reviews WHERE resource_type = :resourceType AND resource_id = :resourceId")
    Mono<Double> getAverageRating(ResourceType resourceType, UUID resourceId);
}
