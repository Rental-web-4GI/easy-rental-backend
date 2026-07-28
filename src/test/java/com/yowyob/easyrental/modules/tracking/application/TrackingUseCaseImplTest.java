package com.yowyob.easyrental.modules.tracking.application;

import com.yowyob.easyrental.modules.tracking.domain.PositionSource;
import com.yowyob.easyrental.modules.tracking.domain.RentalPositionEntity;
import com.yowyob.easyrental.modules.tracking.domain.port.out.TrackingRepositoryPort;
import com.yowyob.easyrental.modules.tracking.dto.PositionRequest;
import com.yowyob.easyrental.modules.tracking.dto.PositionResponseDTO;
import com.yowyob.easyrental.modules.tracking.mapper.TrackingMapper;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackingUseCaseImplTest {

    @Mock private TrackingRepositoryPort repo;
    @Mock private TrackingMapper mapper;

    @InjectMocks private TrackingUseCaseImpl useCase;

    private final UUID rentalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(mapper.toDto(any(RentalPositionEntity.class)))
                .thenAnswer(inv -> {
                    RentalPositionEntity e = inv.getArgument(0);
                    return new PositionResponseDTO(
                            e.getId(), e.getRentalId(), e.getVehicleId(),
                            e.getLatitude(), e.getLongitude(),
                            e.getRecordedAt(),
                            e.getSource() == null ? null : e.getSource().name()
                    );
                });
    }

    private RentalPositionEntity pos(double lat, double lng) {
        return RentalPositionEntity.builder()
                .id(UUID.randomUUID())
                .rentalId(rentalId)
                .latitude(lat)
                .longitude(lng)
                .recordedAt(Instant.now())
                .source(PositionSource.CLIENT_PWA)
                .build();
    }

    @Test
    void computeTrackedKm_3points_sumsHaversineSegments() {
        // Douala approx: 4.05,9.71 → 4.06,9.72 → 4.07,9.73
        // Each 0.01 deg lat ≈ 1.11 km, 0.01 deg lng at 4°N ≈ 1.11 km
        // Diagonal segment ≈ sqrt(1.11² + 1.11²) ≈ 1.57 km ; total 2 segments ≈ 3.14 km
        List<RentalPositionEntity> list = List.of(pos(4.05, 9.71), pos(4.06, 9.72), pos(4.07, 9.73));
        when(repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId)).thenReturn(Flux.fromIterable(list));

        StepVerifier.create(useCase.computeTrackedKm(rentalId))
                .assertNext(km -> {
                    // Between 2.5 and 3.7 km (large tolerance due to earth curvature approx)
                    if (km < 2.5 || km > 3.7) {
                        throw new AssertionError("Expected ~3.14 km, got " + km);
                    }
                })
                .verifyComplete();
    }

    @Test
    void computeTrackedKm_singlePoint_returnsZero() {
        when(repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId))
                .thenReturn(Flux.just(pos(4.05, 9.71)));

        StepVerifier.create(useCase.computeTrackedKm(rentalId))
                .expectNext(0.0)
                .verifyComplete();
    }

    @Test
    void computeTrackedKm_noPoints_returnsZero() {
        when(repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.computeTrackedKm(rentalId))
                .expectNext(0.0)
                .verifyComplete();
    }

    @Test
    void recordPosition_nullLat_throws() {
        PositionRequest req = new PositionRequest(null, 9.71, null);

        StepVerifier.create(useCase.recordPosition(rentalId, req))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    void getSummary_lessThan2Positions_returnsOdometerSource() {
        when(repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId))
                .thenReturn(Flux.just(pos(4.05, 9.71)));

        StepVerifier.create(useCase.getSummary(rentalId))
                .assertNext(summary -> {
                    if (!"ODOMETER".equals(summary.source())) {
                        throw new AssertionError("Expected source ODOMETER, got " + summary.source());
                    }
                })
                .verifyComplete();
    }

    @Test
    void getSummary_2PlusPositions_returnsGpsSource() {
        when(repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId))
                .thenReturn(Flux.just(pos(4.05, 9.71), pos(4.06, 9.72), pos(4.07, 9.73)));

        StepVerifier.create(useCase.getSummary(rentalId))
                .assertNext(summary -> {
                    if (!"GPS".equals(summary.source())) {
                        throw new AssertionError("Expected source GPS, got " + summary.source());
                    }
                    if (summary.positions().size() != 3) {
                        throw new AssertionError("Expected 3 positions, got " + summary.positions().size());
                    }
                })
                .verifyComplete();
    }

    @Test
    void recordPosition_validRequest_savesAndReturnsDto() {
        PositionRequest req = new PositionRequest(4.05, 9.71, "CLIENT_PWA");
        when(repo.save(any(RentalPositionEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.recordPosition(rentalId, req))
                .assertNext(dto -> {
                    if (dto.latitude() != 4.05 || dto.longitude() != 9.71) {
                        throw new AssertionError("Coords not preserved: " + dto);
                    }
                    if (!rentalId.equals(dto.rentalId())) {
                        throw new AssertionError("rentalId not preserved");
                    }
                })
                .verifyComplete();
    }
}
