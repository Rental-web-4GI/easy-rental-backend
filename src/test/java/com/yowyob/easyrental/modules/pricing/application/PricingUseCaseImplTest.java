package com.yowyob.easyrental.modules.pricing.application;

import com.yowyob.easyrental.modules.pricing.domain.port.out.PricingRepositoryPort;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingUseCaseImplTest {

    @Mock
    private PricingRepositoryPort pricingRepository;

    @InjectMocks
    private PricingUseCaseImpl pricingUseCase;

    @Test
    void shouldSetPricingForNewResource() {
        UUID orgId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        when(pricingRepository.findByResourceTypeAndResourceId(ResourceType.VEHICLE, resourceId))
                .thenReturn(Mono.empty());
        when(pricingRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(pricingUseCase.setPricing(
                        orgId, ResourceType.VEHICLE, resourceId,
                        BigDecimal.valueOf(5000), BigDecimal.valueOf(50000)))
                .expectNextMatches(p -> p.getPricePerHour().equals(BigDecimal.valueOf(5000)))
                .verifyComplete();
    }

    @Test
    void shouldGetPricing() {
        UUID resourceId = UUID.randomUUID();
        PricingEntity pricing = PricingEntity.builder()
                .id(UUID.randomUUID()).resourceId(resourceId).pricePerDay(BigDecimal.TEN).build();
        when(pricingRepository.findByResourceTypeAndResourceId(ResourceType.DRIVER, resourceId))
                .thenReturn(Mono.just(pricing));

        StepVerifier.create(pricingUseCase.getPricing(ResourceType.DRIVER, resourceId))
                .expectNext(pricing)
                .verifyComplete();
    }
}
