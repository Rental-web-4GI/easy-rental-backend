package com.yowyob.easyrental.modules.auth.application;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.in.AuthUseCase;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.auth.dto.AuthResponse;
import com.yowyob.easyrental.modules.auth.dto.LoginRequest;
import com.yowyob.easyrental.modules.auth.dto.RegisterRequest;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.shared.events.AuditEvent;
import com.yowyob.easyrental.shared.security.JwtUtil;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthUseCaseImpl implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final OrganizationRepositoryPort orgRepository;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final SubscriptionUseCase subscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final ApplicationEventPublisher eventPublisher;

    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByEmail(request.email())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .map(u -> {
                    eventPublisher.publishEvent(new AuditEvent("LOGIN", "AUTH", "User logged in: " + u.getEmail()));
                    return new AuthResponse(jwtUtil.generateToken(u.getEmail(), u.getRole()));
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Bad credentials")));
    }

    public Mono<UserEntity> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userRepository::findByEmail);
    }

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

    public Mono<UserEntity> registerClient(RegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity user = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .firstname(request.firstname())
                            .lastname(request.lastname())
                            .fullname(request.firstname() + " " + request.lastname())
                            .email(request.email())
                            .password(passwordEncoder.encode(request.password()))
                            .role("CLIENT")
                            .isNewRecord(true)
                            .build();
                    return userRepository.save(Objects.requireNonNull(user))
                            .doOnSuccess(u -> eventPublisher.publishEvent(new AuditEvent("REGISTER_CLIENT", "AUTH",
                                    "New client: " + u.getEmail())));
                })).cast(UserEntity.class);
    }

    @Transactional
    public Mono<OrganizationEntity> registerOrganization(OrgRegisterRequest request) {
        return userRepository.findByEmail(request.email())
                .flatMap(existing -> Mono.<OrganizationEntity>error(new RuntimeException("Email already exists")))
                .switchIfEmpty(Mono.defer(() ->
                    planRepository.findByName("FREE")
                        .switchIfEmpty(Mono.error(new RuntimeException("Plan FREE non configuré en base")))
                        .flatMap(freePlan -> {
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
                                OrganizationEntity org = OrganizationEntity.builder()
                                        .id(UUID.randomUUID())
                                        .name(request.orgName())
                                        .ownerId(savedUser.getId())
                                        .email(savedUser.getEmail())
                                        .country("CM")
                                        .timezone("Africa/Douala")
                                        .subscriptionPlanId(freePlan.getId())
                                        .subscriptionAutoRenew(true)
                                        .isVerified(false)
                                        .isNewRecord(true)
                                        .isDriverBookingRequired(false)
                                        .build();

                                return orgRepository.save(Objects.requireNonNull(org))
                                        .flatMap(savedOrg ->
                                            subscriptionService.createHistoryRecord(savedOrg.getId(),
                                                    freePlan.getName(), null)
                                                .thenReturn(savedOrg)
                                        )
                                        .doOnSuccess(o -> eventPublisher.publishEvent(
                                            new AuditEvent("REGISTER_ORG", "AUTH",
                                                    "New Org: " + o.getName() + " with plan FREE")
                                        ));
                            });
                        })
                ));
    }
}
