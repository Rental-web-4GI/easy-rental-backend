package com.yowyob.easyrental.modules.staff.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAdministrationAdapter;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelAuthAdapter;
import com.yowyob.easyrental.kernel.infrastructure.dto.KernelLoginResult;
import com.yowyob.easyrental.kernel.security.KernelJwtValidator;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.staff.domain.port.out.StaffOnboardingEmailPort;
import com.yowyob.easyrental.modules.staff.dto.StaffInviteRequestDTO;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KernelStaffProvisioningServiceTest {

    @Mock private KernelAuthAdapter kernelAuthAdapter;
    @Mock private KernelAdministrationAdapter kernelAdministrationAdapter;
    @Mock private KernelClientProperties kernelProperties;
    @Mock private KernelJwtValidator kernelJwtValidator;
    @Mock private StaffOnboardingEmailPort staffOnboardingEmailPort;
    @Mock private EasyRentalProperties easyRentalProperties;

    @InjectMocks private KernelStaffProvisioningService service;

    @Test
    void shouldProvisionExistingKernelUser() {
        UUID kernelUserId = UUID.randomUUID();
        UserEntity user = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("agent@test.com")
                .kernelUserId(kernelUserId)
                .role("CLIENT")
                .build();
        StaffInviteRequestDTO request = new StaffInviteRequestDTO(
                "Jane", "Doe", "agent@test.com", UUID.randomUUID(), UUID.randomUUID());

        StepVerifier.create(service.provisionKernelUser(request, email -> Mono.just(user)))
                .expectNextMatches(result -> result.kernelUserId().equals(kernelUserId) && !result.newAccount())
                .verifyComplete();
    }

    @Test
    void shouldCreateKernelAccountWhenUserMissing() {
        UUID kernelUserId = UUID.randomUUID();
        StaffInviteRequestDTO request = new StaffInviteRequestDTO(
                "Jane", "Doe", "new-agent@test.com", UUID.randomUUID(), UUID.randomUUID());
        ObjectNode signUpData = new ObjectMapper().createObjectNode();
        signUpData.put("status", "EMAIL_VERIFICATION_REQUIRED");

        when(kernelProperties.getTenantId()).thenReturn("11111111-1111-1111-1111-111111111111");
        when(kernelAuthAdapter.signUp(anyMap())).thenReturn(Mono.just(signUpData));
        when(kernelAuthAdapter.login(eq("new-agent@test.com"), any())).thenReturn(
                Mono.just(KernelLoginResult.authenticated("token-123")));
        when(kernelJwtValidator.validate("token-123")).thenReturn(Mono.just(
                new KernelAuthClaims(kernelUserId.toString(), "new-agent@test.com", Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of())));

        StepVerifier.create(service.provisionKernelUser(request, email -> Mono.empty()))
                .expectNextMatches(result -> result.kernelUserId().equals(kernelUserId) && result.newAccount())
                .verifyComplete();
    }

    @Test
    void shouldFallbackToLocalProvisionWhenKernelLoginFailsAfterSignup() {
        StaffInviteRequestDTO request = new StaffInviteRequestDTO(
                "Jane", "Doe", "new-agent@test.com", UUID.randomUUID(), UUID.randomUUID());
        ObjectNode signUpData = new ObjectMapper().createObjectNode();
        signUpData.put("status", "EMAIL_VERIFICATION_REQUIRED");

        when(kernelProperties.getTenantId()).thenReturn("11111111-1111-1111-1111-111111111111");
        when(kernelAuthAdapter.signUp(anyMap())).thenReturn(Mono.just(signUpData));
        when(kernelAuthAdapter.login(eq("new-agent@test.com"), any())).thenReturn(
                Mono.error(new RuntimeException("AUTH_INVALID_CREDENTIALS")));

        StepVerifier.create(service.provisionKernelUser(request, email -> Mono.empty()))
                .expectNextMatches(result -> result.kernelUserId() == null
                        && result.newAccount()
                        && result.temporaryPassword() != null)
                .verifyComplete();
    }

    @Test
    void shouldRejectOrganizationOwnerEmail() {
        UserEntity owner = UserEntity.builder()
                .id(UUID.randomUUID())
                .email("owner@test.com")
                .kernelUserId(UUID.randomUUID())
                .role("ORGANIZATION")
                .build();
        StaffInviteRequestDTO request = new StaffInviteRequestDTO(
                "Owner", "Test", "owner@test.com", UUID.randomUUID(), UUID.randomUUID());

        StepVerifier.create(service.provisionKernelUser(request, email -> Mono.just(owner)))
                .expectError(ValidationException.class)
                .verify();
    }
}
