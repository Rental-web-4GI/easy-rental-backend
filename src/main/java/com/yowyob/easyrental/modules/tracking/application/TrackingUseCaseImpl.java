package com.yowyob.easyrental.modules.tracking.application;

import com.yowyob.easyrental.modules.tracking.domain.PositionSource;
import com.yowyob.easyrental.modules.tracking.domain.RentalPositionEntity;
import com.yowyob.easyrental.modules.tracking.domain.port.in.TrackingUseCase;
import com.yowyob.easyrental.modules.tracking.domain.port.out.TrackingRepositoryPort;
import com.yowyob.easyrental.modules.tracking.dto.PositionRequest;
import com.yowyob.easyrental.modules.tracking.dto.PositionResponseDTO;
import com.yowyob.easyrental.modules.tracking.dto.TrackingSummaryDTO;
import com.yowyob.easyrental.modules.tracking.mapper.TrackingMapper;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link TrackingUseCase}: records GPS positions and
 * computes cumulative distance via the Haversine formula, falling back to
 * an "ODOMETER" source label when fewer than two positions are available
 * (the actual odometer delta is computed on the frontend).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Service
@RequiredArgsConstructor
public class TrackingUseCaseImpl implements TrackingUseCase {

    private final TrackingRepositoryPort repo;
    private final TrackingMapper mapper;

    @Override
    public Mono<PositionResponseDTO> recordPosition(UUID rentalId, PositionRequest req) {
        if (req.latitude() == null || req.longitude() == null) {
            return Mono.error(new ValidationException("LAT_LNG_REQUIRED"));
        }
        PositionSource src;
        try {
            src = req.source() == null ? PositionSource.CLIENT_PWA : PositionSource.valueOf(req.source());
        } catch (IllegalArgumentException e) {
            src = PositionSource.CLIENT_PWA;
        }
        RentalPositionEntity entity = RentalPositionEntity.builder()
                .id(UUID.randomUUID())
                .rentalId(rentalId)
                .latitude(req.latitude())
                .longitude(req.longitude())
                .recordedAt(Instant.now())
                .source(src)
                .isNewRecord(true)
                .build();
        return repo.save(entity).map(mapper::toDto);
    }

    @Override
    public Flux<PositionResponseDTO> getPositions(UUID rentalId) {
        return repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId).map(mapper::toDto);
    }

    @Override
    public Mono<Double> computeTrackedKm(UUID rentalId) {
        return repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId)
                .collectList()
                .map(this::sumHaversineKm);
    }

    @Override
    public Mono<TrackingSummaryDTO> getSummary(UUID rentalId) {
        return repo.findAllByRentalIdOrderByRecordedAtAsc(rentalId)
                .collectList()
                .map(list -> {
                    List<PositionResponseDTO> dtos = list.stream().map(mapper::toDto).toList();
                    double km = sumHaversineKm(list);
                    String src = list.size() >= 2 ? "GPS" : "ODOMETER";
                    return new TrackingSummaryDTO(dtos, km, src);
                });
    }

    private double sumHaversineKm(List<RentalPositionEntity> positions) {
        if (positions == null || positions.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        for (int i = 1; i < positions.size(); i++) {
            RentalPositionEntity a = positions.get(i - 1);
            RentalPositionEntity b = positions.get(i);
            total += haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
        }
        return total;
    }

    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }
}
