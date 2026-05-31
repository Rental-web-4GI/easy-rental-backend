package com.yowyob.easyrental.modules.staff.application;

import com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence.AgencyRepository;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
import com.yowyob.easyrental.modules.organization.application.OrganizationUseCaseImpl;
import com.yowyob.easyrental.modules.poste.application.PosteUseCaseImpl;
import com.yowyob.easyrental.modules.staff.dto.*;
import com.yowyob.easyrental.modules.staff.mapper.StaffMapper;
import com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence.StaffRepository;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionPlanRepository;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.modules.staff.domain.port.in.StaffUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffUseCaseImpl implements StaffUseCase {

    private final StaffRepository staffRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionPlanRepository planRepository;
    private final OrganizationUseCaseImpl organizationService;
    private final AgencyRepository agencyRepository;
    private final PosteUseCaseImpl posteService;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    // Dans StaffUseCaseImpl.java

    @Transactional
    public Mono<StaffResponseDTO> addStaffToOrganization(UUID orgId, StaffRequestDTO request) {
        return staffRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.<UserEntity>error(new RuntimeException("Cet email est déjà utilisé")))
                .switchIfEmpty(Mono.defer(() -> organizationRepository.findById(Objects.requireNonNull(orgId))
                        .switchIfEmpty(Mono.<OrganizationEntity>error(new RuntimeException("Organisation non trouvée")))
                        .flatMap(org -> planRepository.findById(Objects.requireNonNull(org.getSubscriptionPlanId()))
                                .flatMap(plan -> {

                                    // Debug log pour vérifier les valeurs en cas de blocage
                                    int current = (org.getCurrentUsers() != null) ? org.getCurrentUsers() : 0;
                                    int max = (plan.getMaxUsers() != null) ? plan.getMaxUsers() : 0;

                                    if (current >= max) {
                                        return Mono.<UserEntity>error(new RuntimeException(
                                                "Quota atteint : " + current + " / " + max
                                                        + " utilisateurs utilisés."));
                                    }

                                    UserEntity newUser = UserEntity.builder()
                                            .id(UUID.randomUUID())
                                            .firstname(request.firstname())
                                            .lastname(request.lastname())
                                            .fullname(request.firstname() + " " + request.lastname())
                                            .email(request.email())
                                            .password(passwordEncoder.encode("motdepasse"))
                                            .role("STAFF")
                                            .organizationId(orgId)
                                            .agencyId(request.agencyId())
                                            .posteId(request.posteId())
                                            .status("ACTIVE")
                                            .hiredAt(LocalDateTime.now())
                                            .isNewRecord(true)
                                            .build();

                                    return staffRepository.save(Objects.requireNonNull(newUser))
                                            .flatMap(savedUser -> organizationService.updateStaffCounter(orgId, 1)
                                                    .then(updateAgencyStaffCounter(request.agencyId(), 1))
                                                    .thenReturn(savedUser));
                                }))))
                .flatMap(this::enrichStaff);
    }

    public Flux<StaffResponseDTO> getStaffByOrganization(UUID orgId) {
        return staffRepository.findAllStaffByOrganizationId(orgId)
                .flatMap(this::enrichStaff);
    }

    public Flux<StaffResponseDTO> getStaffByAgency(UUID agencyId) {
        return staffRepository.findAllStaffByAgencyId(agencyId)
                .flatMap(this::enrichStaff);
    }

    public Mono<StaffResponseDTO> getStaffById(UUID id) {
        return staffRepository.findById(Objects.requireNonNull(id))
                .flatMap(this::enrichStaff)
                .switchIfEmpty(Mono.error(new RuntimeException("Staff non trouvé")));
    }

    @Transactional
    public Mono<Void> deleteStaff(UUID id) {
        return staffRepository.findById(Objects.requireNonNull(id))
                .flatMap(user -> staffRepository.delete(Objects.requireNonNull(user))
                        .then(organizationService.updateStaffCounter(user.getOrganizationId(), -1))
                        .then(updateAgencyStaffCounter(user.getAgencyId(), -1)))
                .doOnSuccess(v -> eventPublisher
                        .publishEvent(new AuditEvent("DELETE_STAFF", "STAFF", "Staff supprimé ID: " + id)));
    }

    private Mono<Void> updateAgencyStaffCounter(UUID agencyId, int increment) {
        return agencyRepository.findById(Objects.requireNonNull(agencyId))
                .flatMap(agency -> {
                    agency.setTotalPersonnel(agency.getTotalPersonnel() + increment);
                    return agencyRepository.save(agency);
                }).then();
    }

    private Mono<StaffResponseDTO> enrichStaff(UserEntity staff) {
        return posteService.getPosteById(staff.getPosteId())
                .map(posteDto -> staffMapper.toDto(staff, posteDto));
    }

    @Transactional
    public Mono<StaffResponseDTO> updateStaff(UUID staffId, StaffUpdateDTO request) {
        return staffRepository.findById(Objects.requireNonNull(staffId))
                .switchIfEmpty(Mono.<UserEntity>error(new RuntimeException("Staff non trouvé")))
                .flatMap(user -> {
                    UUID oldAgencyId = user.getAgencyId();
                    UUID newAgencyId = request.agencyId();

                    // 1. Gestion du changement d'agence (Compteurs)
                    Mono<Void> counterUpdate = Mono.empty();
                    if (newAgencyId != null && !newAgencyId.equals(oldAgencyId)) {
                        user.setAgencyId(newAgencyId);
                        // On décrémente l'ancienne agence et on incrémente la nouvelle
                        counterUpdate = updateAgencyStaffCounter(oldAgencyId, -1)
                                .then(updateAgencyStaffCounter(newAgencyId, 1));
                    }

                    // 2. Mise à jour des champs basiques
                    if (request.firstname() != null)
                        user.setFirstname(request.firstname());
                    if (request.lastname() != null)
                        user.setLastname(request.lastname());
                    if (request.posteId() != null)
                        user.setPosteId(request.posteId());
                    if (request.status() != null)
                        user.setStatus(request.status());

                    // Recalculer le fullname si nécessaire
                    user.setFullname(user.getFirstname() + " " + user.getLastname());

                    // 3. Sauvegarde et enrichissement
                    return counterUpdate
                            .then(staffRepository.save(user))
                            .flatMap(this::enrichStaff);
                })
                .doOnSuccess(s -> eventPublisher.publishEvent(
                        new AuditEvent("UPDATE_STAFF", "STAFF", "Mise à jour du staff : " + s.email())));
    }
}
