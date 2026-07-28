package com.yowyob.easyrental.modules.inspection.application;

import com.yowyob.easyrental.modules.inspection.domain.InspectionChecklistDefaults;
import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import com.yowyob.easyrental.modules.inspection.domain.InspectionType;
import com.yowyob.easyrental.modules.inspection.domain.ItemStatus;
import com.yowyob.easyrental.modules.inspection.domain.RentalInspectionEntity;
import com.yowyob.easyrental.modules.inspection.domain.port.in.InspectionUseCase;
import com.yowyob.easyrental.modules.inspection.domain.port.out.InspectionItemRepositoryPort;
import com.yowyob.easyrental.modules.inspection.domain.port.out.InspectionRepositoryPort;
import com.yowyob.easyrental.modules.inspection.dto.InspectionComparisonResult;
import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;
import com.yowyob.easyrental.modules.inspection.dto.InspectionItemDTO;
import com.yowyob.easyrental.modules.inspection.dto.InspectionResponseDTO;
import com.yowyob.easyrental.modules.inspection.mapper.InspectionMapper;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Implements rental inspection use cases: creation with a photo-count guard
 * and default/custom checklist seeding, lookup by id and by rental.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Service
@RequiredArgsConstructor
public class InspectionUseCaseImpl implements InspectionUseCase {

    private final InspectionRepositoryPort inspectionRepository;
    private final InspectionItemRepositoryPort inspectionItemRepository;
    private final InspectionMapper inspectionMapper;
    private final InspectionComparisonService comparisonService;

    @Override
    public Mono<InspectionResponseDTO> createInspection(UUID rentalId, InspectionCreateRequest request) {
        if (request.photoUrls() == null || request.photoUrls().size() < 4) {
            return Mono.error(new ValidationException("MIN_4_PHOTOS"));
        }

        RentalInspectionEntity toSave = RentalInspectionEntity.builder()
                .id(UUID.randomUUID())
                .rentalId(rentalId)
                .type(InspectionType.valueOf(request.type()))
                .odometer(request.odometer())
                .fuelLevel(request.fuelLevel())
                .notes(request.notes())
                .photoUrls(request.photoUrls())
                .performedAt(Instant.now())
                .isNewRecord(true)
                .build();

        return inspectionRepository.save(toSave)
                .flatMap(saved -> {
                    List<InspectionItemEntity> itemEntities = buildItemEntities(saved.getId(), request.items());
                    return inspectionItemRepository.saveAll(Flux.fromIterable(itemEntities))
                            .collectList()
                            .map(savedItems -> inspectionMapper.toResponseDto(saved, savedItems));
                });
    }

    @Override
    public Mono<InspectionResponseDTO> getInspection(UUID inspectionId) {
        return inspectionRepository.findById(inspectionId)
                .flatMap(inspection -> inspectionItemRepository.findAllByInspectionId(inspectionId)
                        .collectList()
                        .map(items -> inspectionMapper.toResponseDto(inspection, items)));
    }

    @Override
    public Flux<InspectionResponseDTO> listByRental(UUID rentalId) {
        return inspectionRepository.findAllByRentalId(rentalId)
                .flatMap(inspection -> inspectionItemRepository.findAllByInspectionId(inspection.getId())
                        .collectList()
                        .map(items -> inspectionMapper.toResponseDto(inspection, items)));
    }

    @Override
    public Mono<InspectionComparisonResult> compareCheckInOut(UUID rentalId) {
        return inspectionRepository.findAllByRentalId(rentalId)
                .collectList()
                .flatMap(all -> {
                    RentalInspectionEntity in = all.stream()
                            .filter(i -> i.getType() == InspectionType.CHECK_IN)
                            .findFirst().orElse(null);
                    RentalInspectionEntity out = all.stream()
                            .filter(i -> i.getType() == InspectionType.CHECK_OUT)
                            .findFirst().orElse(null);
                    if (in == null || out == null) {
                        return Mono.error(new ValidationException("MISSING_INSPECTION"));
                    }
                    return Mono.zip(
                            inspectionItemRepository.findAllByInspectionId(in.getId()).collectList(),
                            inspectionItemRepository.findAllByInspectionId(out.getId()).collectList()
                    ).map(tuple -> comparisonService.compare(in, tuple.getT1(), out, tuple.getT2()));
                });
    }

    private List<InspectionItemEntity> buildItemEntities(UUID inspectionId, List<InspectionItemDTO> items) {
        Instant now = Instant.now();
        if (items != null && !items.isEmpty()) {
            return items.stream()
                    .map(dto -> InspectionItemEntity.builder()
                            .id(UUID.randomUUID())
                            .inspectionId(inspectionId)
                            .itemCode(dto.itemCode())
                            .status(ItemStatus.valueOf(dto.status()))
                            .note(dto.note())
                            .createdAt(now)
                            .isNewRecord(true)
                            .build())
                    .toList();
        }
        return InspectionChecklistDefaults.ITEM_CODES.stream()
                .map(code -> InspectionItemEntity.builder()
                        .id(UUID.randomUUID())
                        .inspectionId(inspectionId)
                        .itemCode(code)
                        .status(ItemStatus.OK)
                        .note(null)
                        .createdAt(now)
                        .isNewRecord(true)
                        .build())
                .toList();
    }
}
