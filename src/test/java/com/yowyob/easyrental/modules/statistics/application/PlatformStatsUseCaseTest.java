package com.yowyob.easyrental.modules.statistics.application;

import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import com.yowyob.easyrental.shared.enums.RentalStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;

/**
 * Vérifie l'agrégation des compteurs plateforme (users, orgs, agences, véhicules,
 * locations, revenu MRR abonnements).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlatformStatsUseCaseTest {

    @Mock private DatabaseClient databaseClient;
    @Mock private AgencyRepositoryPort agencyRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private VehicleRepositoryPort vehicleRepository;
    @Mock private RentalRepositoryPort rentalRepository;
    @Mock private SubscriptionRepositoryPort subscriptionRepository;

    @InjectMocks private StatisticsUseCaseImpl statisticsUseCase;

    @Test
    void shouldAggregatePlatformStats() {
        // Users
        when(userRepository.count()).thenReturn(Mono.just(100L));
        when(userRepository.countByRole("CLIENT")).thenReturn(Mono.just(70L));
        when(userRepository.countByRole("ORGANIZATION")).thenReturn(Mono.just(20L));
        when(userRepository.countByAccountType("FREELANCE")).thenReturn(Mono.just(5L));
        when(userRepository.countByRole("STAFF")).thenReturn(Mono.just(10L));

        // Organizations
        when(organizationRepository.count()).thenReturn(Mono.just(25L));
        when(organizationRepository.countByAccountType("COMPANY")).thenReturn(Mono.just(20L));
        when(organizationRepository.countByAccountType("FREELANCE")).thenReturn(Mono.just(5L));
        when(organizationRepository.countByGovernanceStatus("REJECTED")).thenReturn(Mono.just(2L));

        // Agencies
        when(agencyRepository.count()).thenReturn(Mono.just(40L));

        // Vehicles
        when(vehicleRepository.count()).thenReturn(Mono.just(200L));
        when(vehicleRepository.countByStatut("AVAILABLE")).thenReturn(Mono.just(150L));

        // Rentals
        when(rentalRepository.count()).thenReturn(Mono.just(500L));
        when(rentalRepository.countByStatus(RentalStatus.ONGOING)).thenReturn(Mono.just(30L));
        when(rentalRepository.countByStatus(RentalStatus.COMPLETED)).thenReturn(Mono.just(400L));
        when(rentalRepository.countCompletedThisMonth()).thenReturn(Mono.just(15L));

        // Subscriptions : une seule souscription active, plan à 5000
        when(subscriptionRepository.sumActivePlanPrices()).thenReturn(Mono.just(BigDecimal.valueOf(5000)));
        when(subscriptionRepository.countActiveOrganizations()).thenReturn(Mono.just(1L));

        StepVerifier.create(statisticsUseCase.getPlatformStats())
                .assertNext(stats -> {
                    // Users
                    org.assertj.core.api.Assertions.assertThat(stats.users().total()).isEqualTo(100L);
                    org.assertj.core.api.Assertions.assertThat(stats.users().clients()).isEqualTo(70L);
                    org.assertj.core.api.Assertions.assertThat(stats.users().orgOwners()).isEqualTo(20L);
                    org.assertj.core.api.Assertions.assertThat(stats.users().freelances()).isEqualTo(5L);
                    org.assertj.core.api.Assertions.assertThat(stats.users().staff()).isEqualTo(10L);

                    // Organizations
                    org.assertj.core.api.Assertions.assertThat(stats.organizations().total()).isEqualTo(25L);
                    org.assertj.core.api.Assertions.assertThat(stats.organizations().companies()).isEqualTo(20L);
                    org.assertj.core.api.Assertions.assertThat(stats.organizations().freelances()).isEqualTo(5L);
                    org.assertj.core.api.Assertions.assertThat(stats.organizations().suspended()).isEqualTo(2L);

                    // Agencies : 40 agences / 20 companies = 2.0 en moyenne
                    org.assertj.core.api.Assertions.assertThat(stats.agencies().total()).isEqualTo(40L);
                    org.assertj.core.api.Assertions.assertThat(stats.agencies().averagePerCompany()).isEqualTo(2.0);

                    // Vehicles
                    org.assertj.core.api.Assertions.assertThat(stats.vehicles().total()).isEqualTo(200L);
                    org.assertj.core.api.Assertions.assertThat(stats.vehicles().published()).isEqualTo(150L);

                    // Rentals
                    org.assertj.core.api.Assertions.assertThat(stats.rentals().total()).isEqualTo(500L);
                    org.assertj.core.api.Assertions.assertThat(stats.rentals().ongoing()).isEqualTo(30L);
                    org.assertj.core.api.Assertions.assertThat(stats.rentals().completed()).isEqualTo(400L);
                    org.assertj.core.api.Assertions.assertThat(stats.rentals().monthlyCompleted()).isEqualTo(15L);

                    // Revenue : MRR = 5000, 1 abonnement actif
                    org.assertj.core.api.Assertions.assertThat(stats.revenue().monthlyRecurringRevenue())
                            .isEqualByComparingTo(BigDecimal.valueOf(5000));
                    org.assertj.core.api.Assertions.assertThat(stats.revenue().subscriptionsActiveCount())
                            .isEqualTo(1L);
                })
                .verifyComplete();
    }

    @Test
    void shouldDefaultMrrToZeroWhenNoActiveSubscriptions() {
        when(userRepository.count()).thenReturn(Mono.just(0L));
        when(userRepository.countByRole("CLIENT")).thenReturn(Mono.just(0L));
        when(userRepository.countByRole("ORGANIZATION")).thenReturn(Mono.just(0L));
        when(userRepository.countByAccountType("FREELANCE")).thenReturn(Mono.just(0L));
        when(userRepository.countByRole("STAFF")).thenReturn(Mono.just(0L));

        when(organizationRepository.count()).thenReturn(Mono.just(0L));
        when(organizationRepository.countByAccountType("COMPANY")).thenReturn(Mono.just(0L));
        when(organizationRepository.countByAccountType("FREELANCE")).thenReturn(Mono.just(0L));
        when(organizationRepository.countByGovernanceStatus("REJECTED")).thenReturn(Mono.just(0L));

        when(agencyRepository.count()).thenReturn(Mono.just(0L));

        when(vehicleRepository.count()).thenReturn(Mono.just(0L));
        when(vehicleRepository.countByStatut("AVAILABLE")).thenReturn(Mono.just(0L));

        when(rentalRepository.count()).thenReturn(Mono.just(0L));
        when(rentalRepository.countByStatus(RentalStatus.ONGOING)).thenReturn(Mono.just(0L));
        when(rentalRepository.countByStatus(RentalStatus.COMPLETED)).thenReturn(Mono.just(0L));
        when(rentalRepository.countCompletedThisMonth()).thenReturn(Mono.just(0L));

        when(subscriptionRepository.sumActivePlanPrices()).thenReturn(Mono.just(BigDecimal.ZERO));
        when(subscriptionRepository.countActiveOrganizations()).thenReturn(Mono.just(0L));

        StepVerifier.create(statisticsUseCase.getPlatformStats())
                .assertNext(stats -> {
                    org.assertj.core.api.Assertions.assertThat(stats.agencies().averagePerCompany()).isEqualTo(0.0);
                    org.assertj.core.api.Assertions.assertThat(stats.revenue().monthlyRecurringRevenue())
                            .isEqualByComparingTo(BigDecimal.ZERO);
                })
                .verifyComplete();
    }
}
