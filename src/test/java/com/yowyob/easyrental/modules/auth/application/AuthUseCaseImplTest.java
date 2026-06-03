package com.yowyob.easyrental.modules.auth.application;

import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.LoginRequest;
import com.yowyob.easyrental.modules.auth.dto.RegisterRequest;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.shared.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private OrganizationRepositoryPort orgRepository;
    @Mock
    private SubscriptionPlanRepositoryPort planRepository;
    @Mock
    private SubscriptionUseCase subscriptionService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AuthUseCaseImpl authUseCase;

    @Test
    void shouldLoginWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("user@test.com", "password");
        UserEntity user = UserEntity.builder().id(UUID.randomUUID())
                .email("user@test.com").password("hash").role("CLIENT").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("password", "hash")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any())).thenReturn("token");

        StepVerifier.create(authUseCase.login(request))
                .expectNextMatches(r -> r.token().equals("token"))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenLoginFails() {
        LoginRequest request = new LoginRequest("user@test.com", "wrong");
        UserEntity user = UserEntity.builder().id(UUID.randomUUID())
                .email("user@test.com").password("hash").role("CLIENT").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        StepVerifier.create(authUseCase.login(request))
                .expectErrorMatches(e -> e.getMessage().equals("Bad credentials"))
                .verify();
    }

    @Test
    void shouldGetCurrentUserFromSecurityContext() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("user@test.com").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        var auth = new UsernamePasswordAuthenticationToken("user@test.com", null);

        StepVerifier.create(authUseCase.getCurrentUser()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void shouldRefreshTokenWhenValid() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID())
                .email("user@test.com").role("CLIENT").build();
        when(jwtUtil.validateToken("old-token")).thenReturn(true);
        when(jwtUtil.getUsernameFromToken("old-token")).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Mono.just(user));
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("new-token");

        StepVerifier.create(authUseCase.refreshToken("Bearer old-token"))
                .expectNextMatches(r -> r.token().equals("new-token"))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenRefreshTokenInvalid() {
        when(jwtUtil.validateToken("bad")).thenReturn(false);

        StepVerifier.create(authUseCase.refreshToken("bad"))
                .expectErrorMatches(e -> e.getMessage().equals("Invalid Token"))
                .verify();
    }

    @Test
    void shouldRegisterClient() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "new@test.com", "pass");
        when(userRepository.findByEmail("new@test.com")).thenReturn(Mono.empty());
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        UserEntity saved = UserEntity.builder().id(UUID.randomUUID()).email("new@test.com").role("CLIENT").build();
        when(userRepository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(authUseCase.registerClient(request))
                .expectNextMatches(u -> u.getEmail().equals("new@test.com"))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenRegisterClientEmailExists() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "exists@test.com", "pass");
        when(userRepository.findByEmail("exists@test.com"))
                .thenReturn(Mono.just(UserEntity.builder().id(UUID.randomUUID()).email("exists@test.com").build()));

        StepVerifier.create(authUseCase.registerClient(request))
                .expectErrorMatches(e -> e.getMessage().equals("Email already exists"))
                .verify();
    }

    @Test
    void shouldRegisterOrganization() {
        OrgRegisterRequest request = new OrgRegisterRequest("Jane", "Doe", "org@test.com", "pass", "My Org");
        UUID planId = UUID.randomUUID();
        SubscriptionPlanEntity freePlan = SubscriptionPlanEntity.builder()
                .id(planId).name("FREE").maxAgencies(5).maxUsers(10).price(BigDecimal.ZERO).build();

        when(userRepository.findByEmail("org@test.com")).thenReturn(Mono.empty());
        when(planRepository.findByName("FREE")).thenReturn(Mono.just(freePlan));
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            return Mono.just(u);
        });
        when(orgRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(subscriptionService.createHistoryRecord(any(), anyString(), any())).thenReturn(Mono.empty());

        StepVerifier.create(authUseCase.registerOrganization(request))
                .expectNextMatches(org -> org.getName().equals("My Org"))
                .verifyComplete();
    }
}
