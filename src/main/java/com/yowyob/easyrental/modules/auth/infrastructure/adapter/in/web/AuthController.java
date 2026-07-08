package com.yowyob.easyrental.modules.auth.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.AuthResponse;
import com.yowyob.easyrental.modules.auth.dto.LoginRequest;
import com.yowyob.easyrental.modules.auth.dto.MfaConfirmRequest;
import com.yowyob.easyrental.modules.auth.dto.RegisterClientResponse;
import com.yowyob.easyrental.modules.auth.dto.RegisterRequest;
import com.yowyob.easyrental.modules.auth.domain.port.in.AuthUseCase;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return authUseCase.login(request)
                .map(response -> {
                    if (Boolean.TRUE.equals(response.mfaRequired())) {
                        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
                    }
                    return ResponseEntity.ok(response);
                });
    }

    @PostMapping("/login/mfa/confirm")
    public Mono<ResponseEntity<AuthResponse>> confirmMfa(@RequestBody MfaConfirmRequest request) {
        return authUseCase.confirmMfa(request.mfaToken(), request.code())
                .map(ResponseEntity::ok);
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<UserEntity>> me() {
        return authUseCase.getCurrentUser()
                .map(ResponseEntity::ok);
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return authUseCase.refreshToken(authHeader)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/register/client")
    public Mono<ResponseEntity<RegisterClientResponse>> registerClient(@RequestBody RegisterRequest request) {
        return authUseCase.registerClient(request)
                .map(body -> ResponseEntity.status(HttpStatus.CREATED).body(body));
    }

    @PostMapping("/register/organizationOwner")
    public Mono<ResponseEntity<OrganizationEntity>> registerOrganization(@RequestBody OrgRegisterRequest request) {
        return authUseCase.registerOrganization(request)
                .map(ResponseEntity::ok);
    }
}
