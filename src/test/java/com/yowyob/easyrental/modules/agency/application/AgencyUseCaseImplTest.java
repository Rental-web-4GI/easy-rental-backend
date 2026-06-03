package com.yowyob.easyrental.modules.agency.application;

import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.dto.AgencyRequestDTO;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import com.yowyob.easyrental.modules.agency.mapper.AgencyMapper;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgencyUseCaseImplTest {

    @Mock
    private AgencyRepositoryPort agencyRepository;
    @Mock
    private OrganizationRepositoryPort organizationRepository;
    @Mock
    private SubscriptionPlanRepositoryPort planRepository;
    @Mock
    private AgencyMapper agencyMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AgencyUseCaseImpl agencyUseCase;

    @Test
    void shouldReturnErrorWhenAgencyNotFound() {
        UUID id = UUID.randomUUID();
        when(agencyRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(agencyUseCase.getAgency(id))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void shouldGetAgencyWhenFound() {
        UUID id = UUID.randomUUID();
        AgencyEntity entity = AgencyEntity.builder().id(id).name("Agency Douala").build();
        AgencyResponseDTO dto = mock(AgencyResponseDTO.class);
        when(dto.name()).thenReturn("Agency Douala");
        when(agencyRepository.findById(id)).thenReturn(Mono.just(entity));
        when(agencyMapper.toDto(entity)).thenReturn(dto);

        StepVerifier.create(agencyUseCase.getAgency(id))
                .expectNextMatches(r -> r.name().equals("Agency Douala"))
                .verifyComplete();
    }

    @Test
    void shouldListAgenciesByOrg() {
        UUID orgId = UUID.randomUUID();
        AgencyEntity entity = AgencyEntity.builder().id(UUID.randomUUID()).organizationId(orgId).name("A1").build();
        when(agencyRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.just(entity));
        when(agencyMapper.toDto(entity)).thenReturn(mock(AgencyResponseDTO.class));

        StepVerifier.create(agencyUseCase.getAgenciesByOrg(orgId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldCanAddVehicleResource() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity org = OrganizationEntity.builder()
                .id(orgId).subscriptionPlanId(planId).currentVehicles(1).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder()
                .id(planId).maxVehicles(5).build();
        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(org));
        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));

        StepVerifier.create(agencyUseCase.canAddResource(orgId, "VEHICLE"))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldCreateAgency() {
        UUID orgId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        OrganizationEntity organization = OrganizationEntity.builder()
                .id(orgId).subscriptionPlanId(planId).currentAgencies(0).build();
        SubscriptionPlanEntity plan = SubscriptionPlanEntity.builder()
                .id(planId).name("FREE").maxAgencies(10).build();
        AgencyRequestDTO request = mock(AgencyRequestDTO.class);
        when(request.name()).thenReturn("New Agency");
        AgencyEntity saved = AgencyEntity.builder().id(UUID.randomUUID()).organizationId(orgId).name("New Agency").build();
        AgencyResponseDTO dto = mock(AgencyResponseDTO.class);
        when(dto.name()).thenReturn("New Agency");

        when(organizationRepository.findById(orgId)).thenReturn(Mono.just(organization));
        when(planRepository.findById(planId)).thenReturn(Mono.just(plan));
        when(agencyRepository.save(any())).thenReturn(Mono.just(saved));
        when(organizationRepository.save(any())).thenReturn(Mono.just(organization));
        when(agencyMapper.toDto(saved)).thenReturn(dto);

        StepVerifier.create(agencyUseCase.createAgency(orgId, request))
                .expectNextMatches(r -> r.name().equals("New Agency"))
                .verifyComplete();
    }

    @Test
    void shouldDeleteAgency() {
        UUID id = UUID.randomUUID();
        when(agencyRepository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(agencyUseCase.deleteAgency(id))
                .verifyComplete();
    }

    @Test
    void shouldListAllAgencies() {
        AgencyEntity entity = AgencyEntity.builder().id(UUID.randomUUID()).name("All").build();
        when(agencyRepository.findAll()).thenReturn(Flux.just(entity));
        when(agencyMapper.toDto(entity)).thenReturn(mock(AgencyResponseDTO.class));

        StepVerifier.create(agencyUseCase.getAllAgencies())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldSearchAgencies() {
        AgencyEntity entity = AgencyEntity.builder().id(UUID.randomUUID()).name("Search").build();
        when(agencyRepository.searchAgencies("douala", "Douala")).thenReturn(Flux.just(entity));
        when(agencyMapper.toDto(entity)).thenReturn(mock(AgencyResponseDTO.class));

        StepVerifier.create(agencyUseCase.searchAgencies("douala", "Douala"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpdateAgency() {
        UUID id = UUID.randomUUID();
        AgencyEntity existing = AgencyEntity.builder().id(id).name("Old").build();
        AgencyRequestDTO request = mock(AgencyRequestDTO.class);
        when(request.name()).thenReturn("Updated");
        AgencyResponseDTO dto = mock(AgencyResponseDTO.class);
        when(agencyRepository.findById(id)).thenReturn(Mono.just(existing));
        when(agencyRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(agencyMapper.toDto(any())).thenReturn(dto);

        StepVerifier.create(agencyUseCase.updateAgency(id, request))
                .expectNext(dto)
                .verifyComplete();
    }
}
