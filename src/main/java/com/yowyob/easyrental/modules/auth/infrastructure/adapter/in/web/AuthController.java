package com.yowyob.easyrental.modules.auth.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.*;
import com.yowyob.easyrental.modules.auth.application.AuthUseCaseImpl;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.dto.OrgRegisterRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCaseImpl authUseCaseImpl;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {
        return authUseCaseImpl.login(request).map(ResponseEntity::ok);
    }

    // Récupérer l'utilisateur connecté
    @GetMapping("/me")
    public Mono<ResponseEntity<UserEntity>> me() {
        return authUseCaseImpl.getCurrentUser()
                .map(ResponseEntity::ok);
    }

    // Rafraîchir le token
    @PostMapping("/refresh")
    public Mono<ResponseEntity<AuthResponse>> refresh(ServerWebExchange exchange) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        return authUseCaseImpl.refreshToken(authHeader)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/register/client")
    public Mono<ResponseEntity<UserEntity>> registerClient(@RequestBody RegisterRequest request) {
        return authUseCaseImpl.registerClient(request)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/register/organizationOwner")
    public Mono<ResponseEntity<OrganizationEntity>> registerOrganization(@RequestBody OrgRegisterRequest request) {
        return authUseCaseImpl.registerOrganization(request)
                .map(ResponseEntity::ok);
    }
}
