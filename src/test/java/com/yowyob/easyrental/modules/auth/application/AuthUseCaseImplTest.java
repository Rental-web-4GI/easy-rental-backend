package com.yowyob.easyrental.modules.auth.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.infrastructure.dto.KernelLoginResult;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.AuthResponse;
import com.yowyob.easyrental.modules.auth.dto.LoginRequest;
import com.yowyob.easyrental.modules.auth.dto.RegisterRequest;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.in.SubscriptionUseCase;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.shared.exception.ValidationException;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

import static org.mockito.ArgumentMatchers.anyMap;
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
    @Mock
    private KernelClientProperties kernelProperties;
    @Mock
    private com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAuthAdapter kernelAuthAdapter;
    @Mock
    private com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter kernelOrganizationAdapter;
    @Mock
    private com.yowyob.easyrental.kernel.application.KernelUserMappingService kernelUserMappingService;
    @Mock
    private com.yowyob.easyrental.kernel.application.KernelSessionStore kernelSessionStore;
    @Mock
    private com.yowyob.easyrental.kernel.application.KernelOrganizationBootstrapService kernelOrganizationBootstrapService;
    @Mock
    private EasyRentalProperties easyRentalProperties;

    @InjectMocks
    private AuthUseCaseImpl authUseCase;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(kernelProperties.isIntegrationEnabled()).thenReturn(false);
        EasyRentalProperties.Client client = new EasyRentalProperties.Client();
        client.setSkipKernelAuth(true);
        org.mockito.Mockito.lenient().when(easyRentalProperties.getClient()).thenReturn(client);
    }

    @Test
    void shouldReturnMfaRequiredWhenKernelLoginRequiresMfa() {
        when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
        EasyRentalProperties.Client clientProps = new EasyRentalProperties.Client();
        clientProps.setSkipKernelAuth(false);
        when(easyRentalProperties.getClient()).thenReturn(clientProps);
        LoginRequest request = new LoginRequest("admin@test.com", "password");
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Mono.empty());
        when(kernelAuthAdapter.login("admin@test.com", "password"))
                .thenReturn(Mono.just(com.yowyob.easyrental.kernel.infrastructure.dto.KernelLoginResult
                        .mfaRequired("mfa-token", "EMAIL")));

        StepVerifier.create(authUseCase.login(request))
                .expectNextMatches(r -> Boolean.TRUE.equals(r.mfaRequired())
                        && "mfa-token".equals(r.mfaToken()))
                .verifyComplete();
    }

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
                .expectErrorMatches(e -> e instanceof ValidationException
                        && e.getMessage().contains("SESSION_EXPIRED"))
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
                .expectNextMatches(r -> r.user().getEmail().equals("new@test.com")
                        && !r.emailVerificationRequired())
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenRegisterClientEmailExists() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "exists@test.com", "pass");
        when(userRepository.findByEmail("exists@test.com"))
                .thenReturn(Mono.just(UserEntity.builder().id(UUID.randomUUID()).email("exists@test.com").build()));

        StepVerifier.create(authUseCase.registerClient(request))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    void shouldRegisterClientWithKernelEmailVerificationRequired() {
        RegisterRequest request = new RegisterRequest("John", "Doe", "kernel-client@test.com", "pass");
        ObjectNode signUpData = new ObjectMapper().createObjectNode();
        signUpData.put("status", "EMAIL_VERIFICATION_REQUIRED");
        signUpData.put("id", UUID.randomUUID().toString());
        UserEntity saved = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("kernel-client@test.com")
                .role("CLIENT")
                .build();

        when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
        EasyRentalProperties.Client clientProps = new EasyRentalProperties.Client();
        clientProps.setSkipKernelAuth(false);
        when(easyRentalProperties.getClient()).thenReturn(clientProps);
        when(userRepository.findByEmail("kernel-client@test.com")).thenReturn(Mono.empty());
        when(kernelAuthAdapter.signUp(anyMap())).thenReturn(Mono.just(signUpData));
        when(userRepository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(authUseCase.registerClient(request))
                .expectNextMatches(r -> r.emailVerificationRequired()
                        && r.user().getEmail().equals("kernel-client@test.com"))
                .verifyComplete();
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

    @Test
    void refreshToken_whenLocalTokenValid_andKernelEnabled_andSessionExpired_refreshesKernelToken() {
        // Arrange
        String email = "owner@test.com";
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email(email).role("ORGANIZATION").build();

        when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
        when(jwtUtil.validateToken(anyString())).thenReturn(true);
        when(jwtUtil.getUsernameFromToken(anyString())).thenReturn(email);
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("new-local-token");
        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));
        // kernel session expirée (resolve retourne empty)
        // refresh token disponible
        when(kernelSessionStore.resolve(email)).thenReturn(Optional.empty());
        when(kernelSessionStore.resolveRefreshToken(email)).thenReturn(Optional.of("old-refresh-token"));
        when(kernelAuthAdapter.refresh("old-refresh-token"))
                .thenReturn(Mono.just(KernelLoginResult.authenticated("new-kernel-token")));

        // Act
        AuthResponse response = authUseCase.refreshToken("Bearer some-valid-local-token").block();

        // Assert
        assertNotNull(response);
        assertNotNull(response.token());
        verify(kernelSessionStore).store(email, "new-kernel-token");
    }

    @Test
    void refreshToken_whenLocalTokenInvalid_throwsValidationException() {
        when(jwtUtil.validateToken("invalid.token.here")).thenReturn(false);

        // Act & Assert
        StepVerifier.create(authUseCase.refreshToken("Bearer invalid.token.here"))
                .expectErrorMatches(ex -> ex instanceof ValidationException
                        && ex.getMessage().contains("SESSION_EXPIRED"))
                .verify();
    }
}
