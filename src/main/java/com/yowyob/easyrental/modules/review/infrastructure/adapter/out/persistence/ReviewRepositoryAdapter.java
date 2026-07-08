package com.yowyob.easyrental.modules.review.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.review.domain.ReviewEntity;
import com.yowyob.easyrental.modules.review.domain.port.out.ReviewRepositoryPort;
import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {

    private final ReviewRepository reviewRepository;

    @Override
    public Mono<ReviewEntity> save(ReviewEntity review) {
        return reviewRepository.save(review);
    }

    @Override
    public Mono<ReviewEntity> findById(UUID id) {
        return reviewRepository.findById(id);
    }

    @Override
    public Flux<ReviewEntity> findAllByResourceTypeAndResourceId(ResourceType resourceType, UUID resourceId) {
        return reviewRepository.findAllByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public Mono<Double> getAverageRating(ResourceType resourceType, UUID resourceId) {
        return reviewRepository.getAverageRating(resourceType, resourceId);
    }

    @Override
    public Flux<ReviewEntity> findPublishedLatest(int limit) {
        return reviewRepository.findPublishedLatest(limit);
    }

    @Override
    public Flux<ReviewEntity> findAllOrderedByCreatedAtDesc() {
        return reviewRepository.findAllOrderedByCreatedAtDesc();
    }

    @Override
    public Mono<Double> getPublishedAverageRating() {
        return reviewRepository.getPublishedAverageRating();
    }

    @Override
    public Mono<Long> countPublished() {
        return reviewRepository.countPublished();
    }

    @Override
    public Mono<Long> countUnpublished() {
        return reviewRepository.countUnpublished();
    }
}
