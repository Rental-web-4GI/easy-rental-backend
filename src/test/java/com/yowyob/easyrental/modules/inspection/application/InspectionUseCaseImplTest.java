package com.yowyob.easyrental.modules.inspection.application;

import com.yowyob.easyrental.modules.inspection.domain.InspectionChecklistDefaults;
import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import com.yowyob.easyrental.modules.inspection.domain.ItemStatus;
import com.yowyob.easyrental.modules.inspection.domain.port.out.InspectionItemRepositoryPort;
import com.yowyob.easyrental.modules.inspection.domain.port.out.InspectionRepositoryPort;
import com.yowyob.easyrental.modules.inspection.dto.InspectionCreateRequest;
import com.yowyob.easyrental.modules.inspection.dto.InspectionItemDTO;
import com.yowyob.easyrental.modules.inspection.dto.InspectionResponseDTO;
import com.yowyob.easyrental.modules.inspection.mapper.InspectionMapper;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InspectionUseCaseImplTest {

    @Mock
    private InspectionRepositoryPort inspectionRepository;
    @Mock
    private InspectionItemRepositoryPort inspectionItemRepository;
    @Mock
    private InspectionMapper inspectionMapper;
    @Mock
    private InspectionComparisonService comparisonService;

    @InjectMocks
    private InspectionUseCaseImpl useCase;

    private static final List<String> FOUR_PHOTOS = List.of("p1", "p2", "p3", "p4");

    @Test
    void createInspection_lessThan4Photos_throwsValidation() {
        InspectionCreateRequest request = new InspectionCreateRequest(
                "CHECK_IN", 1000, (short) 80, "notes", List.of("p1", "p2", "p3"), null);

        StepVerifier.create(useCase.createInspection(UUID.randomUUID(), request))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof ValidationException);
                    assertTrue(err.getMessage().contains("MIN_4_PHOTOS"));
                })
                .verify();
    }

    @Test
    void createInspection_nullPhotos_throwsValidation() {
        InspectionCreateRequest request = new InspectionCreateRequest(
                "CHECK_IN", 1000, (short) 80, "notes", null, null);

        StepVerifier.create(useCase.createInspection(UUID.randomUUID(), request))
                .expectErrorSatisfies(err -> {
                    assertTrue(err instanceof ValidationException);
                    assertTrue(err.getMessage().contains("MIN_4_PHOTOS"));
                })
                .verify();
    }

    @Test
    void createInspection_seedsDefaultChecklist_whenItemsAbsent() {
        UUID rentalId = UUID.randomUUID();
        InspectionCreateRequest request = new InspectionCreateRequest(
                "CHECK_IN", 1000, (short) 80, "notes", FOUR_PHOTOS, null);

        when(inspectionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(inspectionItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inspectionMapper.toResponseDto(any(), any())).thenReturn(
                new InspectionResponseDTO(null, null, null, null, null, null, null, null, null, null));

        StepVerifier.create(useCase.createInspection(rentalId, request))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Flux<InspectionItemEntity>> captor = ArgumentCaptor.forClass(Flux.class);
        verify(inspectionItemRepository).saveAll(captor.capture());
        List<InspectionItemEntity> savedItems = captor.getValue().collectList().block();

        assertEquals(12, savedItems.size());
        List<String> codes = savedItems.stream().map(InspectionItemEntity::getItemCode).toList();
        assertEquals(InspectionChecklistDefaults.ITEM_CODES, codes);
        assertTrue(savedItems.stream().allMatch(item -> item.getStatus() == ItemStatus.OK));
    }

    @Test
    void createInspection_customItems_override() {
        UUID rentalId = UUID.randomUUID();
        List<InspectionItemDTO> customItems = List.of(new InspectionItemDTO("TIRES", "DAMAGED", "bosse"));
        InspectionCreateRequest request = new InspectionCreateRequest(
                "CHECK_OUT", 2000, (short) 50, "notes", FOUR_PHOTOS, customItems);

        when(inspectionRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(inspectionItemRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inspectionMapper.toResponseDto(any(), any())).thenReturn(
                new InspectionResponseDTO(null, null, null, null, null, null, null, null, null, null));

        StepVerifier.create(useCase.createInspection(rentalId, request))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Flux<InspectionItemEntity>> captor = ArgumentCaptor.forClass(Flux.class);
        verify(inspectionItemRepository).saveAll(captor.capture());
        List<InspectionItemEntity> savedItems = captor.getValue().collectList().block();

        assertEquals(1, savedItems.size());
        assertEquals("TIRES", savedItems.get(0).getItemCode());
        assertEquals(ItemStatus.DAMAGED, savedItems.get(0).getStatus());
        assertEquals("bosse", savedItems.get(0).getNote());
    }
}
