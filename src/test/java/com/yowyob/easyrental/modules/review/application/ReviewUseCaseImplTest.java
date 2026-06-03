package com.yowyob.easyrental.modules.review.application;

import com.yowyob.easyrental.modules.driver.domain.port.out.DriverRepositoryPort;
import com.yowyob.easyrental.modules.review.domain.port.out.ReviewRepositoryPort;
import com.yowyob.easyrental.modules.review.domain.ReviewEntity;
import com.yowyob.easyrental.modules.review.dto.ReviewRequestDTO;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewUseCaseImplTest {

    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private VehicleRepositoryPort vehicleRepository;
    @Mock
    private DriverRepositoryPort driverRepository;

    @InjectMocks
    private ReviewUseCaseImpl reviewUseCase;

    @Test
    void shouldAddReviewForVehicle() {
        UUID resourceId = UUID.randomUUID();
        ReviewRequestDTO request = new ReviewRequestDTO(resourceId, ResourceType.VEHICLE, 5, "Great", "John");
        ReviewEntity saved = ReviewEntity.builder().id(UUID.randomUUID()).resourceId(resourceId)
                .resourceType(ResourceType.VEHICLE).rating(5).comment("Great").authorName("John").build();
        VehicleEntity vehicle = VehicleEntity.builder().id(resourceId).rating(0.0).build();

        when(reviewRepository.save(any())).thenReturn(Mono.just(saved));
        when(reviewRepository.getAverageRating(ResourceType.VEHICLE, resourceId)).thenReturn(Mono.just(5.0));
        when(vehicleRepository.findById(resourceId)).thenReturn(Mono.just(vehicle));
        when(vehicleRepository.save(any())).thenReturn(Mono.just(vehicle));

        StepVerifier.create(reviewUseCase.addReview(request))
                .expectNextMatches(dto -> dto.rating() == 5)
                .verifyComplete();
    }

    @Test
    void shouldGetReviews() {
        UUID resourceId = UUID.randomUUID();
        ReviewEntity review = ReviewEntity.builder().id(UUID.randomUUID()).resourceId(resourceId)
                .resourceType(ResourceType.DRIVER).rating(4).build();
        when(reviewRepository.findAllByResourceTypeAndResourceId(ResourceType.DRIVER, resourceId))
                .thenReturn(Flux.just(review));

        StepVerifier.create(reviewUseCase.getReviews(ResourceType.DRIVER, resourceId))
                .expectNextCount(1)
                .verifyComplete();
    }
}
