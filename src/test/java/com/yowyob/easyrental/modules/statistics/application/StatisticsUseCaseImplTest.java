package com.yowyob.easyrental.modules.statistics.application;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.statistics.dto.AgencyStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.GlobalStatsDTO;
import com.yowyob.easyrental.modules.statistics.dto.OrgStatsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatisticsUseCaseImplTest {

    @Mock private DatabaseClient databaseClient;
    @Mock private AgencyRepositoryPort agencyRepository;
    @InjectMocks private StatisticsUseCaseImpl statisticsUseCase;

    private DatabaseClient.GenericExecuteSpec sqlSpec;
    private DatabaseClient.GenericExecuteSpec boundSpec;

    @BeforeEach
    void setUp() {
        sqlSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        boundSpec = mock(DatabaseClient.GenericExecuteSpec.class);
        when(databaseClient.sql(anyString())).thenReturn(sqlSpec);
        when(sqlSpec.bind(anyString(), any())).thenReturn(boundSpec);
        when(boundSpec.bind(anyString(), any())).thenReturn(boundSpec);
        when(boundSpec.bind(anyString(), anyInt())).thenReturn(boundSpec);
    }

    @SuppressWarnings("unchecked")
    private void stubMapOne(Object value) {
        RowsFetchSpec<Object> rowsFetchSpec = mock(RowsFetchSpec.class);
        when(boundSpec.map(any(Function.class))).thenReturn(rowsFetchSpec);
        when(boundSpec.map(any(BiFunction.class))).thenReturn(rowsFetchSpec);
        when(rowsFetchSpec.one()).thenReturn(Mono.just(value));
    }

    private void stubOneQuery(GlobalStatsDTO stats) {
        stubMapOne(stats);
    }

    @SuppressWarnings("unchecked")
    private void stubFetchAll(List<Map<String, Object>> rows) {
        FetchSpec<Map<String, Object>> fetchSpec = mock(FetchSpec.class);
        when(boundSpec.fetch()).thenReturn(fetchSpec);
        when(fetchSpec.all()).thenReturn(Flux.fromIterable(rows));
    }

    private void stubScalarOne(Object value) {
        stubMapOne(value);
    }

    @SuppressWarnings("unchecked")
    private void stubMapOneSequence(Object... values) {
        Queue<Object> queue = new LinkedList<>(Arrays.asList(values));
        RowsFetchSpec<Object> rowsFetchSpec = mock(RowsFetchSpec.class);
        when(boundSpec.map(any(Function.class))).thenReturn(rowsFetchSpec);
        when(boundSpec.map(any(BiFunction.class))).thenReturn(rowsFetchSpec);
        when(rowsFetchSpec.one()).thenAnswer(invocation -> Mono.just(queue.poll()));
    }

    @Test
    void shouldReturnEmptyOrganizationDashboardWhenNoAgencies() {
        UUID orgId = UUID.randomUUID();
        when(agencyRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.empty());

        StepVerifier.create(statisticsUseCase.getOrganizationDashboard(orgId, 2025))
                .expectNextMatches(dashboard -> dashboard.summary().totalRentals() == 0L)
                .verifyComplete();
    }

    @Test
    void shouldGetOrganizationDashboardWithAgencies() {
        UUID orgId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).name("Agency A").build();
        when(agencyRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.just(agency));

        GlobalStatsDTO stats = new GlobalStatsDTO(
                1L, 2L, 1L, 1L, 5L, 1L, 2L, BigDecimal.valueOf(1000), BigDecimal.valueOf(200));
        stubMapOneSequence(stats, 2L, 3L, BigDecimal.valueOf(500));
        stubFetchAll(List.of());

        StepVerifier.create(statisticsUseCase.getOrganizationDashboard(orgId, 2025))
                .expectNextMatches(dashboard -> dashboard.summary().totalRentals() == 5L)
                .verifyComplete();
    }

    @Test
    void shouldGetAgencyDashboard() {
        UUID agencyId = UUID.randomUUID();
        GlobalStatsDTO stats = new GlobalStatsDTO(
                1L, 2L, 1L, 1L, 5L, 1L, 2L, BigDecimal.valueOf(1000), BigDecimal.valueOf(200));

        stubOneQuery(stats);
        stubFetchAll(List.of());

        StepVerifier.create(statisticsUseCase.getAgencyDashboard(agencyId, 2025))
                .expectNextMatches(d -> d.summary().totalRentals() == 5L)
                .verifyComplete();
    }

    @Test
    void shouldGetAgencyStats() {
        UUID agencyId = UUID.randomUUID();
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).name("Agency A").build();

        stubScalarOne(BigDecimal.valueOf(500));
        Map<String, Object> statusRow = new HashMap<>();
        statusRow.put("status", "COMPLETED");
        statusRow.put("count", 3L);
        stubFetchAll(List.of(statusRow));
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));

        StepVerifier.create(statisticsUseCase.getAgencyStats(agencyId, 2025, null))
                .expectNextMatches(AgencyStatsDTO.class::isInstance)
                .verifyComplete();
    }

    @Test
    void shouldGetOrganizationStats() {
        UUID orgId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        AgencyEntity agency = AgencyEntity.builder().id(agencyId).name("Agency A").build();
        when(agencyRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.just(agency));

        stubScalarOne(BigDecimal.ZERO);
        stubFetchAll(List.of());
        when(agencyRepository.findById(agencyId)).thenReturn(Mono.just(agency));

        StepVerifier.create(statisticsUseCase.getOrganizationStats(orgId, 2025))
                .expectNextMatches(OrgStatsDTO.class::isInstance)
                .verifyComplete();
    }
}
