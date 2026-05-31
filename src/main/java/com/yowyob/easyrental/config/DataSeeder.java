package com.yowyob.easyrental.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence.AgencyRepository;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence.UserRepository;
import com.yowyob.easyrental.modules.driver.domain.DriverEntity;
import com.yowyob.easyrental.modules.driver.infrastructure.adapter.out.persistence.DriverRepository;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.poste.domain.PosteEntity;
import com.yowyob.easyrental.modules.poste.infrastructure.adapter.out.persistence.PosteRepository;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.pricing.infrastructure.adapter.out.persistence.PricingRepository;
import com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence.StaffRepository;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionPlanRepository;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionRepository;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.CategoryRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.VehicleRepository;
import com.yowyob.easyrental.shared.enums.ResourceType;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "docker"}) // S'exécute uniquement avec le profil 'dev' ou 'docker'
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AgencyRepository agencyRepository;
    private final PosteRepository posteRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CategoryRepository categoryRepository;
    private final StaffRepository staffRepository;
    private final PricingRepository pricingRepository;

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    @Override
    public void run(String... args) {
        log.info("🌱 Démarrage du Seeder de données (Mode Robuste)…");

        // suppression des données précédentes (tables concernées),
        // poste/permission/plan restent en place
        cleanDatabase()
            .doOnTerminate(() -> log.info("🧹 Nettoyage initial terminé"))
            .then(seedOrganizations())
            .doOnError(error -> log.error("❌ Erreur critique lors du seeding : ", error))
            .doOnTerminate(() -> log.info("🏁 Processus de seeding terminé."))
            .subscribe();
    }

    /**
     * Vide les tables susceptibles d'avoir été créées par un précédent lancement.
     * Ne touche pas aux tables postes, permissions, plans : elles sont réutilisées
     * par le seeder.
     */
    private Mono<Void> cleanDatabase() {
        log.info("🧹 Purge des données existantes (excepté postes / permissions système) …");
        return userRepository.clearUserReferences()
            .then(pricingRepository.deleteAll())
            .then(driverRepository.deleteAll())
            .then(vehicleRepository.deleteAll())
            .then(subscriptionRepository.deleteAll())
            .then(agencyRepository.deleteAll())
            .then(posteRepository.deleteOrganizationPostes())
            .then(organizationRepository.deleteAll())
            .then(userRepository.deleteAll());
    }

    private Mono<Void> seedOrganizations() {
        // Création de 2 organisations réalistes
        return createFullOrganization("contact@prestige-auto.cm", "Prestige Auto Cameroun", "ENTERPRISE_YEARLY")
            .then(createFullOrganization("info@logistics-express.cm", "Logistics Express", "PRO"));
    }

    private Mono<Void> createFullOrganization(String email, String orgName, String planName) {
        // 1. Vérifier si le propriétaire existe déjà
        return userRepository.findByEmail(email)
            .flatMap(existing -> {
                log.info("⚠️ L'utilisateur {} existe déjà. Vérification de l'organisation...", email);
                // Si l'user existe, on cherche son organisation pour continuer le seeding (véhicules, etc.)
                return organizationRepository.findByOwnerId(existing.getId())
                        .flatMap(org -> seedPostes(org)
                                .flatMap(postes -> seedAgenciesAndResources(org, null, postes)))
                        .then();
            })
            .switchIfEmpty(Mono.defer(() ->
                planRepository.findByName(planName)
                    .switchIfEmpty(Mono.error(new RuntimeException("Plan " + planName + " introuvable.")))
                    .flatMap(plan -> {
                        log.info("🏗️ Création de l'organisation : {} (Plan: {})", orgName, planName);

                        // Création du Propriétaire
                        UserEntity owner = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .firstname("PDG")
                            .lastname(orgName.split(" ")[0])
                            .fullname("PDG " + orgName)
                            .email(email)
                            .password(passwordEncoder.encode("password123"))
                            .role("ORGANIZATION")
                            .status("ACTIVE")
                            .isNewRecord(true)
                            .build();

                        return userRepository.save(owner).flatMap(savedOwner -> {
                            savedOwner.setNewRecord(false);
                            LocalDateTime expiresAt = (plan.getDurationDays() > 0) ? LocalDateTime.now().plusDays(plan.getDurationDays()) : null;

                            // Création de l'Organisation
                            OrganizationEntity org = OrganizationEntity.builder()
                                .id(UUID.randomUUID())
                                .name(orgName)
                                .description("Leader de la location de véhicules au Cameroun. Service premium.")
                                .ownerId(savedOwner.getId())
                                .email(email)
                                .phone("+237 699 00 00 00")
                                .address("Boulevard de la Liberté, Akwa")
                                .city("Douala")
                                .country("CM")
                                .region("Littoral")
                                .postalCode("BP 1234")
                                .website("https://www." + orgName.toLowerCase().replaceAll(" ", "") + ".cm")
                                .timezone("Africa/Douala")
                                .subscriptionPlanId(plan.getId())
                                .subscriptionExpiresAt(expiresAt)
                                .isVerified(true)
                                .verificationDate(LocalDateTime.now())
                                .logoUrl("https://ui-avatars.com/api/?name=" + orgName.replaceAll(" ", "+") + "&background=0D8ABC&color=fff&size=200")
                                .registrationNumber("RC/DLA/2024/B/" + System.currentTimeMillis())
                                .taxNumber("M0123456789" + System.currentTimeMillis())
                                .isDriverBookingRequired(false)
                                .isNewRecord(true)
                                .build();

                            return organizationRepository.save(org).flatMap(savedOrg -> {
                                savedOwner.setOrganizationId(savedOrg.getId());

                                return subscriptionRepository.save(SubscriptionEntity.builder()
                                        .id(UUID.randomUUID())
                                        .organizationId(savedOrg.getId())
                                        .planType(planName)
                                        .status("ACTIVE")
                                        .startDate(LocalDateTime.now())
                                        .endDate(expiresAt)
                                        .isNewRecord(true).build())
                                    .then(userRepository.save(savedOwner))
                                    .then(seedPostes(savedOrg))
                                    .flatMap(postes ->
                                        seedAgenciesAndResources(savedOrg, plan, postes)
                                        .thenReturn(savedOwner)
                                    );
                            });
                        });
                    })
                    .then() // <-- conversion en Mono<Void> pour satisfaire switchIfEmpty
            ))
            .then();
    }

    private Mono<Map<String, PosteEntity>> seedPostes(OrganizationEntity org) {
        // On récupère les postes existants ou on les crée
        return posteRepository.findAllByOrganizationIdOrSystem(org.getId())
            .collectMap(PosteEntity::getName)
            .flatMap(existingPostes -> {
                List<Mono<PosteEntity>> toCreate = new ArrayList<>();

                if (!existingPostes.containsKey("Manager Agence")) {
                    toCreate.add(posteRepository.save(PosteEntity.builder().id(UUID.randomUUID()).organizationId(org.getId()).name("Manager Agence").description("Responsable opérationnel").isNewRecord(true).build()));
                }
                if (!existingPostes.containsKey("Agent Commercial")) {
                    toCreate.add(posteRepository.save(PosteEntity.builder().id(UUID.randomUUID()).organizationId(org.getId()).name("Agent Commercial").description("Gestion clientèle").isNewRecord(true).build()));
                }
                if (!existingPostes.containsKey("Chef de Parc")) {
                    toCreate.add(posteRepository.save(PosteEntity.builder().id(UUID.randomUUID()).organizationId(org.getId()).name("Chef de Parc").description("Maintenance flotte").isNewRecord(true).build()));
                }

                return Flux.concat(toCreate)
                    .then(posteRepository.findAllByOrganizationIdOrSystem(org.getId()).collectMap(PosteEntity::getName));
            });
    }

    private Mono<Void> seedAgenciesAndResources(OrganizationEntity org, SubscriptionPlanEntity plan, Map<String, PosteEntity> postes) {
        return Flux.just("Douala - Bonanjo", "Yaoundé - Bastos")
            .flatMap(agencyName -> {
                String city = agencyName.split(" - ")[0];
                String email = "agence." + city.toLowerCase() + "@" + org.getName().toLowerCase().replaceAll(" ", "") + ".cm";

                // Vérification existence Agence par Email (pour éviter doublons)
                return agencyRepository.findAllByOrganizationId(org.getId())
                    .filter(a -> a.getEmail().equals(email))
                    .next()
                    .switchIfEmpty(Mono.defer(() -> {
                        // Création Agence si n'existe pas
                        AgencyEntity agency = AgencyEntity.builder()
                            .id(UUID.randomUUID())
                            .organizationId(org.getId())
                            .name(org.getName() + " - " + agencyName)
                            .description("Agence principale de " + city)
                            .address(city.equals("Douala") ? "Rue Joss, Bonanjo" : "Avenue des Banques, Bastos")
                            .city(city)
                            .country("CM")
                            .region(city.equals("Douala") ? "Littoral" : "Centre")
                            .email(email)
                            .phone("+237 677 55 00 " + (city.equals("Douala") ? "01" : "02"))
                            .is24Hours(true)
                            .latitude(city.equals("Douala") ? 4.0511 : 3.8480)
                            .longitude(city.equals("Douala") ? 9.7679 : 11.5021)
                            .geofenceRadius(1000.0)
                            .logoUrl("https://ui-avatars.com/api/?name=" + city + "&background=random")
                            .primaryColor("#1e40af")
                            .secondaryColor("#fbbf24")
                            .workingHours("Lundi-Dimanche: 08h-20h")
                            .allowOnlineBooking(true)
                            .depositPercentage(10.0)
                            .isNewRecord(true)
                            .build();
                        return agencyRepository.save(agency);
                    }))
                    .flatMap(savedAgency -> {
                        savedAgency.setNewRecord(false);

                        // Création du Staff (Idempotent)
                        return seedStaff(org, savedAgency, 3, postes)
                            .flatMap(managerId -> {
                                savedAgency.setManagerId(managerId);
                                savedAgency.setTotalPersonnel(3);
                                return agencyRepository.save(savedAgency);
                            })
                            // Création des Véhicules (Idempotent)
                            .then(seedRealVehicles(org, savedAgency))
                            .flatMap(vehicleCount -> {
                                savedAgency.setTotalVehicles(vehicleCount);
                                savedAgency.setActiveVehicles(vehicleCount);
                                return agencyRepository.save(savedAgency);
                            })
                            // Création des Chauffeurs (Idempotent)
                            .then(seedDrivers(org, savedAgency, 4))
                            .flatMap(driverCount -> {
                                savedAgency.setTotalDrivers(driverCount);
                                savedAgency.setActiveDrivers(driverCount);
                                return agencyRepository.save(savedAgency);
                            });
                    });
            })
            .then(updateOrganizationCounters(org.getId()));
    }

    private Mono<UUID> seedStaff(OrganizationEntity org, AgencyEntity agency, int count, Map<String, PosteEntity> postes) {
        return Flux.range(1, count)
            .flatMap(i -> {
                boolean isManager = (i == 1);
                String roleName = isManager ? "Manager Agence" : "Agent Commercial";
                PosteEntity poste = postes.getOrDefault(roleName, postes.values().iterator().next());

                String email = "staff." + i + "." + agency.getCity().toLowerCase() + "@demo.com";

                // VÉRIFICATION CRUCIALE : On vérifie si l'email existe avant de créer
                return userRepository.findByEmail(email)
                    .map(UserEntity::getId) // Si existe, on retourne son ID
                    .switchIfEmpty(Mono.defer(() -> {
                        // Si n'existe pas, on crée
                        UserEntity staff = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .organizationId(org.getId())
                            .agencyId(agency.getId())
                            .posteId(poste.getId())
                            .firstname(isManager ? "Jean" : "Paul")
                            .lastname(isManager ? "Manager" : "Employé " + i)
                            .fullname((isManager ? "Jean Manager" : "Paul Employé " + i))
                            .email(email)
                            .password(passwordEncoder.encode("password123"))
                            .role("STAFF")
                            .status("ACTIVE")
                            .hiredAt(LocalDateTime.now().minusMonths(i))
                            .isNewRecord(true)
                            .build();
                        return userRepository.save(staff).map(UserEntity::getId);
                    }));
            })
            .collectList()
            .map(list -> list.isEmpty() ? null : list.get(0)); // Retourne l'ID du premier (Manager)
    }

    private Mono<Integer> seedRealVehicles(OrganizationEntity org, AgencyEntity agency) {
        return categoryRepository.findAll()
            .collectMap(VehicleCategoryEntity::getName)
            .flatMap(categories -> {
                List<Mono<VehicleEntity>> vehiclesToCreate = new ArrayList<>();

                // 1. Toyota Corolla
                vehiclesToCreate.add(createVehicleSafe(org, agency, categories.get("Berline (Sedan)"),
                    "Toyota", "Corolla LE", "LT-123-AB", 2021, 5, 45000.0, "AUTOMATIC", "Blanc", "Essence",
                    new String[]{"https://images.unsplash.com/photo-1621007947382-bb3c3968e3bb?w=800", "https://images.unsplash.com/photo-1590362835106-1f209f957687?w=800"},
                    35000.0));

                // 2. Toyota Prado
                vehiclesToCreate.add(createVehicleSafe(org, agency, categories.get("SUV (4x4)"),
                    "Toyota", "Land Cruiser Prado", "LT-888-CA", 2022, 7, 25000.0, "AUTOMATIC", "Noir", "Diesel",
                    new String[]{"https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?w=800", "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800"},
                    75000.0));

                // 3. Mercedes C-Class
                vehiclesToCreate.add(createVehicleSafe(org, agency, categories.get("Luxe (Luxury)"),
                    "Mercedes-Benz", "Classe C 300", "LT-001-ZZ", 2023, 5, 10000.0, "AUTOMATIC", "Gris Argent", "Essence",
                    new String[]{"https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=800", "https://images.unsplash.com/photo-1563720223185-11003d516935?w=800"},
                    120000.0));

                return Flux.concat(vehiclesToCreate).count().map(Long::intValue);
            });
    }

    private Mono<VehicleEntity> createVehicleSafe(
            OrganizationEntity org, AgencyEntity agency, VehicleCategoryEntity category,
            String brand, String model, String plateBase, int year, int places, double km,
            String transmission, String color, String fuel, String[] images, double dailyPrice) {

        if (category == null) return Mono.empty();

        // On utilise la plaque de base pour vérifier l'unicité (pour éviter de recréer la même voiture)
        // Note: Dans un vrai projet, il faudrait une méthode findByLicencePlate dans le repo.
        // Ici, on utilise onErrorResume pour attraper l'erreur de duplication si la plaque existe.

        String uniquePlate = plateBase + "-" + agency.getCity().substring(0, 3).toUpperCase();

        VehicleEntity vehicle = VehicleEntity.builder()
            .id(UUID.randomUUID())
            .organizationId(org.getId())
            .agencyId(agency.getId())
            .categoryId(category.getId())
            .licencePlate(uniquePlate)
            .vinNumber(UUID.randomUUID().toString().replace("-", "").toUpperCase().substring(0, 17))
            .brand(brand)
            .model(model)
            .yearProduction(LocalDateTime.of(year, 1, 1, 0, 0))
            .places(places)
            .kilometrage(km)
            .statut("AVAILABLE")
            .transmission(transmission)
            .color(color)
            .imagesList(createJson(images))
            .descriptionList(createJson(new String[]{"Climatisation", "Bluetooth", "GPS", "Sièges Cuir"}))
            .functionalities(createJson(Map.of("gps", true, "ac", true, "bluetooth", true)))
            .engineDetails(createJson(Map.of("type", fuel, "horsepower", 150)))
            .fuelEfficiency(createJson(Map.of("city", "10L/100km", "highway", "7L/100km")))
            .insuranceDetails(createJson(Map.of("provider", "AXA", "expiry", "2026-12-31")))
            .rating(4.5)
            .createdAt(LocalDateTime.now())
            .isNewRecord(true)
            .build();

        return vehicleRepository.save(vehicle)
            .flatMap(savedVehicle -> {
                // Création du prix
                BigDecimal pricePerDay = BigDecimal.valueOf(dailyPrice);
                BigDecimal pricePerHour = pricePerDay.divide(BigDecimal.valueOf(24), 2, java.math.RoundingMode.HALF_UP);

                PricingEntity pricing = PricingEntity.builder()
                    .id(UUID.randomUUID())
                    .organizationId(org.getId())
                    .resourceType(ResourceType.VEHICLE)
                    .resourceId(savedVehicle.getId())
                    .pricePerDay(pricePerDay)
                    .pricePerHour(pricePerHour)
                    .currency("XAF")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isNewRecord(true)
                    .build();

                return pricingRepository.save(pricing).thenReturn(savedVehicle);
            })
            // C'EST ICI QUE LA MAGIE OPÈRE : Si erreur de duplication, on ignore silencieusement
            .onErrorResume(DuplicateKeyException.class, e -> {
                log.info("🚗 Véhicule {} existe déjà. Ignoré.", uniquePlate);
                return Mono.empty();
            });
    }

    private Mono<Integer> seedDrivers(OrganizationEntity org, AgencyEntity agency, int count) {
        String[] firstnames = {"Jean", "Pierre", "Michel", "Alain", "Thierry", "Franck"};
        String[] lastnames = {"Mbarga", "Kamga", "Nguema", "Abessolo", "Tchakounte", "Eto'o"};

        return Flux.range(1, count)
            .flatMap(i -> {
                String fname = firstnames[i % firstnames.length];
                String lname = lastnames[i % lastnames.length];
                String phone = "+237 6" + (90000000 + i) + agency.getCity().length(); // Rendre unique par agence

                // Vérification basique (on pourrait vérifier par tel, mais ici on try/catch)
                DriverEntity driver = DriverEntity.builder()
                    .id(UUID.randomUUID())
                    .organizationId(org.getId())
                    .agencyId(agency.getId())
                    .firstname(fname)
                    .lastname(lname)
                    .tel(phone)
                    .age(28 + i)
                    .gender(0)
                    .profilUrl("https://ui-avatars.com/api/?name=" + fname + "+" + lname + "&background=random&size=200")
                    .cniUrl("https://placehold.co/600x400?text=CNI+" + lname)
                    .drivingLicenseUrl("https://placehold.co/600x400?text=Permis+" + lname)
                    .status("ACTIVE")
                    .rating(4.8)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isNewRecord(true)
                    .build();

                return driverRepository.save(driver)
                    .flatMap(savedDriver -> {
                        BigDecimal pricePerDay = BigDecimal.valueOf(10000);
                        BigDecimal pricePerHour = BigDecimal.valueOf(1000);

                        PricingEntity pricing = PricingEntity.builder()
                            .id(UUID.randomUUID())
                            .organizationId(org.getId())
                            .resourceType(ResourceType.DRIVER)
                            .resourceId(savedDriver.getId())
                            .pricePerDay(pricePerDay)
                            .pricePerHour(pricePerHour)
                            .currency("XAF")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .isNewRecord(true)
                            .build();

                        return pricingRepository.save(pricing).thenReturn(savedDriver);
                    })
                    .onErrorResume(DuplicateKeyException.class, e -> Mono.empty());
            })
            .count()
            .map(Long::intValue);
    }

    private Mono<Void> updateOrganizationCounters(UUID orgId) {
        return organizationRepository.findById(orgId)
            .flatMap(org -> {
                Mono<Long> totalVehicles = vehicleRepository.findAllByOrganizationId(orgId).count();
                Mono<Long> totalDrivers = driverRepository.findAllByOrganizationId(orgId).count();
                Mono<Long> totalStaff = staffRepository.findAllStaffByOrganizationId(orgId).count();
                Mono<Long> totalAgencies = agencyRepository.findAllByOrganizationId(orgId).count();

                return Mono.zip(totalVehicles, totalDrivers, totalStaff, totalAgencies)
                    .flatMap(tuple -> {
                        org.setCurrentVehicles(tuple.getT1().intValue());
                        org.setCurrentDrivers(tuple.getT2().intValue());
                        org.setCurrentUsers(tuple.getT3().intValue());
                        org.setCurrentAgencies(tuple.getT4().intValue());
                        return organizationRepository.save(org);
                    });
            })
            .then();
    }

    private Json createJson(Object object) {
        try {
            return Json.of(objectMapper.writeValueAsString(object));
        } catch (JsonProcessingException e) {
            log.error("Erreur JSON", e);
            return Json.of("{}");
        }
    }
}
