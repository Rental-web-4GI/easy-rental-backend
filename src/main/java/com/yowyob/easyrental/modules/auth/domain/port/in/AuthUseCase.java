package com.yowyob.easyrental.modules.auth.domain.port.in;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.AuthResponse;
import com.yowyob.easyrental.modules.auth.dto.LoginRequest;
import com.yowyob.easyrental.modules.auth.dto.RegisterClientResponse;
import com.yowyob.easyrental.modules.auth.dto.RegisterFreelanceRequest;
import com.yowyob.easyrental.modules.auth.dto.RegisterRequest;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;
import reactor.core.publisher.Mono;

/**
 * Incoming port for auth use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface AuthUseCase {
    Mono<AuthResponse> login(LoginRequest request);
    Mono<AuthResponse> confirmMfa(String mfaToken, String code);
    Mono<UserEntity> getCurrentUser();
    Mono<AuthResponse> refreshToken(String oldToken);
    Mono<RegisterClientResponse> registerClient(RegisterRequest request);
    Mono<OrganizationEntity> registerOrganization(OrgRegisterRequest request);
    Mono<OrganizationEntity> registerFreelance(RegisterFreelanceRequest request);
    Mono<Void> requestEmailVerification();
    Mono<Void> confirmEmailVerification(String verificationToken);
}
