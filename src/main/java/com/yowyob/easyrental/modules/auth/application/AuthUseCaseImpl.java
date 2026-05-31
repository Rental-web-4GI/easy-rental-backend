package com.yowyob.easyrental.modules.auth.application;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.*;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;
import com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence.UserRepository;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence.OrganizationRepository;
// import com.yowyob.easyrental.modules.subscription.domain.SubscriptionEntity;
import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionPlanRepository;
// import com.yowyob.easyrental.modules.subscription.infrastructure.adapter.out.persistence.SubscriptionRepository;
import com.yowyob.easyrental.modules.subscription.application.SubscriptionUseCaseImpl;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.shared.security.JwtUtil;
import com.yowyob.easyrental.modules.auth.domain.port.in.AuthUseCase;
import lombok.RequiredArgsConstructor;

import java.util.Objects;
// import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
// import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

@Service
@RequiredArgsConstructor
public class AuthUseCaseImpl implements AuthUseCase {

    private final UserRepository userRepository;
    private final OrganizationRepository orgRepository;
    private final SubscriptionPlanRepository planRepository; 
    // private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionUseCaseImpl subscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    // Authentification (Login)
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .map(u -> {
                    // Audit de succès
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH", "User logged in: " + u.getEmail()));
                    return new AuthResponse(jwtUtil.generateToken(u.getEmail(), u.getRole()));
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Bad credentials")));
    }

    // Nouvelle méthode : Récupérer l'utilisateur courant via le contexte de sécurité
    public Mono<UserEntity> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userRepository::findByEmail);
    }

    // Nouvelle méthode : Refresh Token
    public Mono<AuthResponse> refreshToken(String oldToken) {
        if (oldToken.startsWith("Bearer ")) {
            oldToken = oldToken.substring(7);
        }

        if (jwtUtil.validateToken(oldToken)) {
            String email = jwtUtil.getUsernameFromToken(oldToken);
            return userRepository.findByEmail(email)
                    .map(user -> new AuthResponse(jwtUtil.generateToken(user.getEmail(), user.getRole())));
        }
        return Mono.error(new RuntimeException("Invalid Token"));
    }

    // Inscription Client Simple
    public Mono<UserEntity> registerClient(RegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity user = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .firstname(request.firstname())
                            .lastname(request.lastname())
                            .fullname(request.firstname() +" "+ request.lastname())
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .role("CLIENT")
                            .isNewRecord(true) // <--- IMPORTANT : Force l'INSERT
                            .build();
                    return userRepository.save(Objects.requireNonNull(user))
                            .doOnSuccess(u -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH", "New client: " + u.getEmail())));
                })).cast(UserEntity.class);
    }

    // Scénario Organisation: Création User + Création Org
    @Transactional
    public Mono<OrganizationEntity> registerOrganization(OrgRegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.<OrganizationEntity>error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() -> 
                    // 1. On cherche d'abord le plan FREE
                    planRepository.findByName("FREE")
                        .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré en base")))
                        .flatMap(freePlan -> {
                            
                            // 2. Créer le User
                            UserEntity user = UserEntity.builder()
                                    .id(UUID.randomUUID())
                                    .firstname(request.firstname())
                                    .lastname(request.lastname())
                                    .fullname(request.firstname() + " " + request.lastname())
                                    .email(request.email())
                                    .password(passwordEncoder.encode(request.password()))
                                    .role("ORGANIZATION")
                                    .isNewRecord(true)
                                    .build();

                            return userRepository.save(Objects.requireNonNull(user)).flatMap(savedUser -> {
                                
                                // 3. Créer l'Organisation AVEC le plan ID déjà présent
                                OrganizationEntity org = OrganizationEntity.builder()
                                        .id(UUID.randomUUID())
                                        .name(request.orgName())
                                        .ownerId(savedUser.getId())
                                        .email(savedUser.getEmail())
                                        .country("CM")
                                        .timezone("Africa/Douala")
                                        .subscriptionPlanId(freePlan.getId()) // <--- FIX : Injecté ici
                                        .subscriptionAutoRenew(true)
                                        .isVerified(false)
                                        .isNewRecord(true)
                                        .isDriverBookingRequired(false)
                                        .build();

                                return orgRepository.save(Objects.requireNonNull(org))
                                        .flatMap(savedOrg -> 
                                            // 4.APPEL AU SERVICE : Remplir la table subscriptions (historique)
                                            subscriptionService.createHistoryRecord(savedOrg.getId(), freePlan.getName(), null)
                                                .thenReturn(savedOrg) // On retourne l'organisation finale
                                        )
                                        .doOnSuccess(o -> eventPublisher.publishEvent(
                                            new AuditEvent("REGISTER_ORG", "AUTH", "New Org: " + o.getName() + " with plan FREE")
                                        ));
                            });
                        })
                ));
    }

  
}
