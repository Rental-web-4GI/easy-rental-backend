package com.yowyob.easyrental.modules.rental.infrastructure;

import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.persistence.RentalRepository;
import com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.persistence.RentalRepositoryAdapter;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalRepositoryAdapterTest {

    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private RentalRepositoryAdapter adapter;

    @Test
    void shouldFindRentalById() {
        UUID id = UUID.randomUUID();
        RentalEntity entity = RentalEntity.builder().id(id).status(RentalStatus.PENDING).build();
        when(rentalRepository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(adapter.findById(id))
                .expectNext(entity)
                .verifyComplete();
    }
}
