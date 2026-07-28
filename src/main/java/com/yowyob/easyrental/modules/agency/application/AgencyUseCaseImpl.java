package com.yowyob.easyrental.modules.agency.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.KernelResponseSupport;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.dto.AgencyRequestDTO;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import com.yowyob.easyrental.modules.agency.mapper.AgencyMapper;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.modules.agency.domain.port.in.AgencyUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgencyUseCaseImpl implements AgencyUseCase {

    private final AgencyRepositoryPort agencyRepository;
    private final OrganizationRepositoryPort organizationRepository;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final AgencyMapper agencyMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final KernelClientProperties kernelProperties;
    private final KernelOrganizationAdapter kernelOrganizationAdapter;
    private final com.yowyob.easyrental.modules.rating.domain.port.in.RatingUseCase ratingUseCase;
    private final com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort userRepository;

    @Transactional
    public Mono<AgencyResponseDTO> createAgency(UUID orgId, AgencyRequestDTO request) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
            .switchIfEmpty(Mono.error(new RuntimeException("Organisation non trouvée")))
            .flatMap(org -> {
                if (kernelProperties.isIntegrationEnabled() && org.getKernelOrganizationId() != null) {
                    return createAgencyViaKernel(org, request);
                }
                return createAgencyLocal(org, orgId, request);
            })
            .doOnSuccess(a -> eventPublisher.publishEvent(new AuditEvent("CREATE_AGENCY", "AGENCY",
                    "Agence créée : " + a.getName())))
            .map(agencyMapper::toDto);
    }

    private Mono<AgencyEntity> createAgencyViaKernel(
            com.yowyob.easyrental.modules.organization.domain.OrganizationEntity org,
            AgencyRequestDTO request) {
        return KernelContextHolder.current()
                .flatMap(ctx -> kernelOrganizationAdapter
                        .createAgency(org.getKernelOrganizationId(), buildKernelAgencyPayload(request), ctx))
                .flatMap(kernelNode -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                        .flatMap(plan -> {
                            if (org.getCurrentAgencies() >= plan.getMaxAgencies()) {
                                return Mono.error(new RuntimeException(
                                        "Quota d'agences atteint pour votre plan (" + plan.getName() + ")"));
                            }
                            String kernelAgencyIdText = KernelResponseSupport.textOrNull(kernelNode, "id");
                            if (kernelAgencyIdText == null) {
                                return Mono.error(new RuntimeException(
                                        "Kernel agency creation did not return an agency id"));
                            }
                            UUID kernelAgencyId = UUID.fromString(kernelAgencyIdText);
                            AgencyEntity agency = buildAgencyEntity(org.getId(), request);
                            agency.setKernelAgencyId(kernelAgencyId);
                            applyKernelAgencyFields(agency, kernelNode);
                            return agencyRepository.save(Objects.requireNonNull(agency))
                                    .flatMap(savedAgency -> {
                                        org.setCurrentAgencies(org.getCurrentAgencies() + 1);
                                        return organizationRepository.save(org).thenReturn(savedAgency);
                                    });
                        }));
    }

    private Map<String, Object> buildKernelAgencyPayload(AgencyRequestDTO request) {
        Map<String, Object> payload = new HashMap<>();
        String name = request.name();
        payload.put("code", "AG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payload.put("name", name);
        payload.put("shortName", name);
        payload.put("longName", name);
        payload.put("agencyType", "BRANCH");
        payload.put("isHeadquarter", false);
        payload.put("active", true);
        if (request.description() != null) {
            payload.put("description", request.description());
        }
        if (request.address() != null) {
            payload.put("address", request.address());
        }
        if (request.city() != null) {
            payload.put("city", request.city());
        }
        payload.put("country", request.country() != null ? request.country() : "CM");
        if (request.phone() != null) {
            payload.put("phone", request.phone());
        }
        if (request.email() != null) {
            payload.put("email", request.email());
        }
        return payload;
    }

    private void applyKernelAgencyFields(AgencyEntity agency, JsonNode kernelNode) {
        String kernelName = KernelResponseSupport.textOrNull(kernelNode, "name");
        if (kernelName != null) {
            agency.setName(kernelName);
        }
        String address = KernelResponseSupport.textOrNull(kernelNode, "address");
        if (address != null) {
            agency.setAddress(address);
        }
        String city = KernelResponseSupport.textOrNull(kernelNode, "city");
        if (city != null) {
            agency.setCity(city);
        }
        String country = KernelResponseSupport.textOrNull(kernelNode, "country");
        if (country != null) {
            agency.setCountry(country);
        }
        String phone = KernelResponseSupport.textOrNull(kernelNode, "phone");
        if (phone != null) {
            agency.setPhone(phone);
        }
        String email = KernelResponseSupport.textOrNull(kernelNode, "email");
        if (email != null) {
            agency.setEmail(email);
        }
    }

    private Mono<AgencyEntity> createAgencyLocal(
            com.yowyob.easyrental.modules.organization.domain.OrganizationEntity org,
            UUID orgId,
            AgencyRequestDTO request) {
        return planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                .flatMap(plan -> {
                    if (org.getCurrentAgencies() >= plan.getMaxAgencies()) {
                        return Mono.error(new RuntimeException(
                                "Quota d'agences atteint pour votre plan (" + plan.getName() + ")"));
                    }
                    AgencyEntity agency = buildAgencyEntity(orgId, request);
                    return agencyRepository.save(Objects.requireNonNull(agency))
                            .flatMap(savedAgency -> {
                                org.setCurrentAgencies(org.getCurrentAgencies() + 1);
                                return organizationRepository.save(org).thenReturn(savedAgency);
                            });
                });
    }

    private AgencyEntity buildAgencyEntity(UUID orgId, AgencyRequestDTO request) {
        return AgencyEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .name(request.name())
                .description(request.description())
                .address(request.address())
                .city(request.city())
                .country(request.country() != null ? request.country() : "CM")
                .postalCode(request.postalCode())
                .region(request.region())
                .phone(request.phone())
                .email(request.email())
                .managerId(request.managerId())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .geofenceRadius(request.geofenceRadius() != null ? request.geofenceRadius() : 500.0)
                .is24Hours(request.is24Hours() != null ? request.is24Hours() : false)
                .timezone(request.timezone() != null ? request.timezone() : "Africa/Douala")
                .workingHours(request.workingHours())
                .allowOnlineBooking(request.allowOnlineBooking() != null ? request.allowOnlineBooking() : true)
                .depositPercentage(request.depositPercentage() != null ? request.depositPercentage() : 0.0)
                .logoUrl(request.logoUrl())
                .primaryColor(request.primaryColor())
                .secondaryColor(request.secondaryColor())
                .isNewRecord(true)
                .build();
    }

    public Mono<Boolean> canAddResource(UUID orgId, String resourceType) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
            .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                .map(plan -> {
                    return switch (resourceType.toUpperCase()) {
                        case "VEHICLE" -> org.getCurrentVehicles() < plan.getMaxVehicles();
                        case "DRIVER" -> org.getCurrentDrivers() < plan.getMaxDrivers();
                        case "USER" -> org.getCurrentUsers() < plan.getMaxUsers();
                        default -> false;
                    };
                }));
    }

    public Flux<AgencyResponseDTO> getAgenciesByOrg(UUID orgId) {
        return organizationRepository.findById(Objects.requireNonNull(orgId))
                .flatMapMany(org -> {
                    if (kernelProperties.isIntegrationEnabled() && org.getKernelOrganizationId() != null) {
                        return syncAgenciesFromKernel(org)
                                .thenMany(agencyRepository.findAllByOrganizationId(orgId));
                    }
                    return agencyRepository.findAllByOrganizationId(orgId);
                })
                .map(agencyMapper::toDto);
    }

    private Mono<Void> syncAgenciesFromKernel(
            com.yowyob.easyrental.modules.organization.domain.OrganizationEntity org) {
        return KernelContextHolder.current()
                .flatMap(ctx -> kernelOrganizationAdapter.listAgencies(org.getKernelOrganizationId(), ctx)
                        .filter(this::isKernelAgencyActive)
                        .collectList()
                        .flatMap(kernelAgencies -> agencyRepository.findAllByOrganizationId(org.getId())
                                .collectList()
                                .flatMap(localAgencies -> planRepository
                                        .findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                                        .flatMap(plan -> {
                                            List<Mono<AgencyEntity>> upserts = kernelAgencies.stream()
                                                    .map(kernelNode -> upsertAgencyFromKernel(
                                                            org.getId(),
                                                            kernelNode,
                                                            localAgencies,
                                                            plan.getMaxAgencies()))
                                                    .toList();
                                            Mono<Void> upsertChain = upserts.isEmpty()
                                                    ? Mono.empty()
                                                    : Flux.mergeSequential(upserts).then();
                                            return upsertChain
                                                    .then(agencyRepository.countByOrganizationId(org.getId()))
                                                    .flatMap(count -> {
                                                        org.setCurrentAgencies(count.intValue());
                                                        return organizationRepository.save(org);
                                                    })
                                                    .then();
                                        }))))
                .onErrorResume(ex -> Mono.empty());
    }

    private boolean isKernelAgencyActive(JsonNode kernelNode) {
        if (!kernelNode.has("active")) {
            return true;
        }
        return kernelNode.path("active").asBoolean(true);
    }

    private Mono<AgencyEntity> upsertAgencyFromKernel(
            UUID orgId, JsonNode kernelNode, List<AgencyEntity> localAgencies, Integer maxAgencies) {
        String kernelAgencyIdText = KernelResponseSupport.textOrNull(kernelNode, "id");
        if (kernelAgencyIdText == null) {
            return Mono.empty();
        }
        UUID kernelAgencyId = UUID.fromString(kernelAgencyIdText);
        AgencyEntity existing = localAgencies.stream()
                .filter(agency -> kernelAgencyId.equals(agency.getKernelAgencyId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            applyKernelAgencyFields(existing, kernelNode);
            return agencyRepository.save(existing);
        }
        return agencyRepository.countByOrganizationId(orgId)
                .flatMap(currentCount -> {
                    if (maxAgencies != null && currentCount >= maxAgencies) {
                        return Mono.empty();
                    }
                    AgencyEntity created = AgencyEntity.builder()
                            .id(UUID.randomUUID())
                            .organizationId(orgId)
                            .kernelAgencyId(kernelAgencyId)
                            .isNewRecord(true)
                            .build();
                    applyKernelAgencyFields(created, kernelNode);
                    String name = KernelResponseSupport.textOrNull(kernelNode, "name");
                    if (name != null) {
                        created.setName(name);
                    }
                    return agencyRepository.save(created);
                });
    }

    public Flux<AgencyResponseDTO> getAllAgencies() {
        return agencyRepository.findCatalogAgencies().flatMap(this::toCatalogDto, 16);
    }

    /**
     * Comme {@link #toDtoWithAccountType} mais EXCLUT du catalogue les agences dont
     * l'organisation est suspendue (cascade R3) — elles disparaissent de la marketplace.
     */
    private Mono<AgencyResponseDTO> toCatalogDto(AgencyEntity agency) {
        if (agency.getOrganizationId() == null) {
            return toDtoWithAccountType(agency);
        }
        return organizationRepository.findById(agency.getOrganizationId())
                .map(org -> "SUSPENDED".equalsIgnoreCase(org.getStatus()))
                .defaultIfEmpty(false)
                .flatMap(suspended -> suspended ? Mono.empty() : toDtoWithAccountType(agency));
    }

    public Mono<AgencyResponseDTO> getAgency(UUID id) {
        return agencyRepository.findById(Objects.requireNonNull(id))
                .flatMap(this::toDtoWithAccountType)
                .switchIfEmpty(Mono.error(new RuntimeException("Agence non trouvée")));
    }

    // NOUVEAU : Service de recherche d'agences
    public Flux<AgencyResponseDTO> searchAgencies(String keyword, String city) {
        return agencyRepository.searchAgencies(
                keyword != null && !keyword.isBlank() ? keyword : null,
                city != null && !city.isBlank() ? city : null
        ).flatMap(this::toCatalogDto, 16);
    }

    private Mono<AgencyResponseDTO> toDtoWithAccountType(AgencyEntity agency) {
        Mono<String> accountTypeMono = agency.getOrganizationId() == null
                ? Mono.just("")
                : organizationRepository.findById(agency.getOrganizationId())
                        .map(org -> org.getAccountType() == null ? "" : org.getAccountType())
                        .defaultIfEmpty("");
        Mono<com.yowyob.easyrental.modules.rating.dto.RatingStatsDTO> statsMono =
                ratingUseCase.getStatsForTarget("AGENCY", agency.getId())
                        .onErrorReturn(new com.yowyob.easyrental.modules.rating.dto.RatingStatsDTO(
                                0.0, 0L, java.util.Map.of()));
        // Option B : si l'agence n'a pas d'email propre, on résout celui du manager.
        Mono<String> managerEmailMono = (isBlank(agency.getEmail()) && agency.getManagerId() != null)
                ? userRepository.findById(agency.getManagerId())
                        .map(u -> u.getEmail() == null ? "" : u.getEmail())
                        .defaultIfEmpty("")
                : Mono.just("");
        return Mono.zip(accountTypeMono, statsMono, managerEmailMono)
                .map(t -> {
                    if (isBlank(agency.getEmail()) && !t.getT3().isEmpty()) {
                        agency.setEmail(t.getT3());
                    }
                    return agencyMapper.toDto(
                            agency,
                            t.getT1().isEmpty() ? null : t.getT1(),
                            t.getT2().average(),
                            t.getT2().count());
                });
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    @Transactional
    public Mono<AgencyResponseDTO> updateAgency(UUID id, AgencyRequestDTO request) {
        return agencyRepository.findById(Objects.requireNonNull(id))
            .switchIfEmpty(Mono.error(new RuntimeException("Agence non trouvée avec l'ID : " + id)))
                .flatMap(existing -> {
                    if(request.name() != null) {
                        existing.setName(request.name());
                    }
                    if(request.address() != null) {
                        existing.setAddress(request.address());
                    }
                    if(request.city() != null) {
                        existing.setCity(request.city());
                    }
                    if(request.phone() != null) {
                        existing.setPhone(request.phone());
                    }
                    if(request.email() != null) {
                        existing.setEmail(request.email());
                    }
                    if(request.description() != null) {
                        existing.setDescription(request.description());
                    }
                    if(request.postalCode() != null) {
                        existing.setPostalCode(request.postalCode());
                    }
                    if(request.region() != null) {
                        existing.setRegion(request.region());
                    }
                    if(request.managerId() != null) {
                        existing.setManagerId(request.managerId());
                    }
                    if(request.latitude() != null) {
                        existing.setLatitude(request.latitude());
                    }
                    if(request.longitude() != null) {
                        existing.setLongitude(request.longitude());
                    }
                    if(request.geofenceRadius() != null) {
                        existing.setGeofenceRadius(request.geofenceRadius());
                    }
                    if(request.is24Hours() != null) {
                        existing.setIs24Hours(request.is24Hours());
                    }
                    if(request.timezone() != null) {
                        existing.setTimezone(request.timezone());
                    }
                    if(request.workingHours() != null) {
                        existing.setWorkingHours(request.workingHours());
                    }
                    if(request.allowOnlineBooking() != null) {
                        existing.setAllowOnlineBooking(request.allowOnlineBooking());
                    }
                    if(request.depositPercentage() != null) {
                        existing.setDepositPercentage(request.depositPercentage());
                    }
                    if(request.logoUrl() != null) {
                        existing.setLogoUrl(request.logoUrl());
                    }
                    if(request.primaryColor() != null) {
                        existing.setPrimaryColor(request.primaryColor());
                    }
                    if(request.secondaryColor() != null) {
                        existing.setSecondaryColor(request.secondaryColor());
                    }
                    return agencyRepository.save(Objects.requireNonNull(existing));
                })
                .doOnSuccess(updated -> eventPublisher.publishEvent(
                        new AuditEvent("UPDATE_AGENCY", "AGENCY", "Updated agency: " + updated.getName())
                ))
                .map(agencyMapper::toDto);
    }

    public Mono<Void> deleteAgency(UUID id) {
        return agencyRepository.findById(Objects.requireNonNull(id))
                .flatMap(agency -> agencyRepository.deleteById(id)
                        .then(organizationRepository.findById(agency.getOrganizationId()))
                        .flatMap(org -> agencyRepository.countByOrganizationId(org.getId())
                                .flatMap(count -> {
                                    org.setCurrentAgencies(count.intValue());
                                    return organizationRepository.save(org);
                                }))
                        .then())
                .doOnSuccess(v -> eventPublisher.publishEvent(
                        new AuditEvent("DELETE_AGENCY", "AGENCY", "Deleted agency ID: " + id)
                ));
    }
}
