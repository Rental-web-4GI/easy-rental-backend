package com.yowyob.easyrental.modules.driver.application;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.driver.domain.DriverEntity;
import com.yowyob.easyrental.modules.driver.domain.port.out.DriverRepositoryPort;
import com.yowyob.easyrental.modules.driver.dto.DriverDetailResponseDTO;
import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.driver.mapper.DriverMapper;
import com.yowyob.easyrental.modules.media.domain.port.in.MediaUseCase;
import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import com.yowyob.easyrental.modules.organization.domain.port.in.OrganizationUseCase;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.pricing.domain.port.in.PricingUseCase;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.review.domain.port.in.ReviewUseCase;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import com.yowyob.easyrental.shared.enums.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverUseCaseImplTest {

    @Mock private DriverRepositoryPort driverRepository;
    @Mock private AgencyRepositoryPort agencyRepository;
    @Mock private OrganizationUseCase organizationService;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private MediaUseCase mediaService;
    @Mock private DriverMapper driverMapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private ScheduleUseCase scheduleService;
    @Mock private PricingUseCase pricingService;
    @Mock private ReviewUseCase reviewService;
    @InjectMocks private DriverUseCaseImpl driverUseCase;

    private DriverEntity sampleDriver;
    private DriverResponseDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleDriver = DriverEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .agencyId(UUID.randomUUID())
                .firstname("John")
                .lastname("Doe")
                .build();
        sampleDto = mock(DriverResponseDTO.class);
    }

    private void stubEnrichDriver() {
        when(pricingService.getPricing(eq(ResourceType.DRIVER), any())).thenReturn(Mono.just(new PricingEntity()));
        when(driverMapper.toDto(any(), any())).thenReturn(sampleDto);
    }

    @Test
    void shouldReturnErrorWhenDriverNotFound() {
        UUID id = UUID.randomUUID();
        when(driverRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(driverUseCase.getDriverById(id))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetDriversByOrg() {
        UUID orgId = UUID.randomUUID();
        when(driverRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.just(sampleDriver));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.getDriversByOrg(orgId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenDateRangeInvalid() {
        UUID agencyId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        StepVerifier.create(driverUseCase.getAvailableDrivers(agencyId, start, end))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void shouldGetAvailableDrivers() {
        UUID agencyId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        when(driverRepository.findAvailableDrivers(agencyId, start, end)).thenReturn(Flux.just(sampleDriver));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.getAvailableDrivers(agencyId, start, end))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetDriversByAgency() {
        UUID agencyId = UUID.randomUUID();
        when(driverRepository.findAllByAgencyId(agencyId)).thenReturn(Flux.just(sampleDriver));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.getDriversByAgency(agencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetDriverDetails() {
        UUID id = sampleDriver.getId();
        when(driverRepository.findById(id)).thenReturn(Mono.just(sampleDriver));
        when(scheduleService.getResourceSchedule(ResourceType.DRIVER, id)).thenReturn(Flux.empty());
        when(reviewService.getReviews(ResourceType.DRIVER, id)).thenReturn(Flux.empty());
        when(organizationRepository.findById(sampleDriver.getOrganizationId())).thenReturn(Mono.just(
                OrganizationEntity.builder().id(sampleDriver.getOrganizationId()).isDriverBookingRequired(false).build()));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.getDriverDetails(id))
                .expectNextMatches(DriverDetailResponseDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldCreateDriver() {
        UUID orgId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        MediaEntity media = MediaEntity.builder().fileUrl("http://file.url").build();
        FilePart profil = mock(FilePart.class);
        FilePart cni = mock(FilePart.class);
        FilePart license = mock(FilePart.class);

        when(organizationService.validateQuota(orgId, "DRIVER")).thenReturn(Mono.just(true));
        when(mediaService.uploadFile(any())).thenReturn(Mono.just(media));
        when(driverRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(organizationService.updateDriverCounter(orgId, 1)).thenReturn(Mono.empty());
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(
                AgencyEntity.builder().id(agencyId).totalDrivers(0).activeDrivers(0).build()));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.createDriver(orgId, agencyId, "John", "Doe", "690000000", 30, 1,
                        profil, cni, license))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldChangeAgency() {
        UUID driverId = sampleDriver.getId();
        UUID newAgencyId = UUID.randomUUID();
        UUID oldAgencyId = sampleDriver.getAgencyId();
        when(driverRepository.findById(driverId)).thenReturn(Mono.just(sampleDriver));
        when(agencyRepository.findById(oldAgencyId)).thenReturn(Mono.just(
                AgencyEntity.builder().id(oldAgencyId).totalDrivers(1).activeDrivers(1).build()));
        when(agencyRepository.findById(newAgencyId)).thenReturn(Mono.just(
                AgencyEntity.builder().id(newAgencyId).totalDrivers(0).activeDrivers(0).build()));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(driverRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.changeAgency(driverId, newAgencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpdateDriverPricing() {
        UUID id = sampleDriver.getId();
        when(driverRepository.findById(id)).thenReturn(Mono.just(sampleDriver));
        when(pricingService.setPricing(any(), eq(ResourceType.DRIVER), eq(id), any(), any()))
                .thenReturn(Mono.just(new PricingEntity()));
        when(scheduleService.getResourceSchedule(ResourceType.DRIVER, id)).thenReturn(Flux.empty());
        when(reviewService.getReviews(ResourceType.DRIVER, id)).thenReturn(Flux.empty());
        when(organizationRepository.findById(sampleDriver.getOrganizationId())).thenReturn(Mono.just(
                OrganizationEntity.builder().id(sampleDriver.getOrganizationId()).build()));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.updateDriverPricing(id,
                        new PricingUpdateDTO(BigDecimal.TEN, BigDecimal.valueOf(100))))
                .expectNextMatches(DriverDetailResponseDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldUpdateDriverSchedules() {
        UUID id = sampleDriver.getId();
        ScheduleRequestDTO schedule = new ScheduleRequestDTO(
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "UNAVAILABLE", "Off");
        when(driverRepository.findById(id)).thenReturn(Mono.just(sampleDriver));
        when(scheduleService.addUnavailability(any(), eq(ResourceType.DRIVER), eq(id), any()))
                .thenReturn(Mono.just(ScheduleEntity.builder().id(UUID.randomUUID()).build()));
        when(scheduleService.getResourceSchedule(ResourceType.DRIVER, id)).thenReturn(Flux.empty());
        when(reviewService.getReviews(ResourceType.DRIVER, id)).thenReturn(Flux.empty());
        when(organizationRepository.findById(sampleDriver.getOrganizationId())).thenReturn(Mono.just(
                OrganizationEntity.builder().id(sampleDriver.getOrganizationId()).build()));
        stubEnrichDriver();

        StepVerifier.create(driverUseCase.updateDriverSchedules(id, new ScheduleUpdateDTO(List.of(schedule))))
                .expectNextMatches(DriverDetailResponseDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldDeleteDriver() {
        UUID id = sampleDriver.getId();
        when(driverRepository.findById(id)).thenReturn(Mono.just(sampleDriver));
        when(driverRepository.delete(any())).thenReturn(Mono.empty());
        when(organizationService.updateDriverCounter(sampleDriver.getOrganizationId(), -1)).thenReturn(Mono.empty());
        when(agencyRepository.findById(sampleDriver.getAgencyId())).thenReturn(Mono.just(
                AgencyEntity.builder().id(sampleDriver.getAgencyId()).totalDrivers(1).activeDrivers(1).build()));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(driverUseCase.deleteDriver(id))
                .verifyComplete();
    }
}
