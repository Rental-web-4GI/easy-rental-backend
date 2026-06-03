package com.yowyob.easyrental.modules.review.domain.port.out;

import com.yowyob.easyrental.modules.review.domain.ReviewEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for review persistence.
 */
public interface ReviewRepositoryPort {

    Mono<ReviewEntity> save(ReviewEntity review);

    Flux<ReviewEntity> findAllByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId);

    Mono<Double> getAverageRating(ResourceType resourceType, UUID resourceId);
}
