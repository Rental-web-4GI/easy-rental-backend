package com.yowyob.easyrental.modules.auth.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.PasswordUpdateDTO;
import com.yowyob.easyrental.modules.auth.dto.UserProfileUpdateDTO;
import com.yowyob.easyrental.modules.auth.domain.port.in.AuthUseCase;
import com.yowyob.easyrental.modules.auth.domain.port.in.UserUseCase;
import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Gestion du profil utilisateur et sécurité")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserUseCase userUseCase;
    private final AuthUseCase authUseCase;

    @Operation(summary = "Mettre à jour le profil de l'utilisateur connecté")
    @PutMapping("/profile")
    public Mono<ResponseEntity<UserEntity>> updateProfile(@RequestBody @Valid UserProfileUpdateDTO dto) {
        return authUseCase.getCurrentUser()
                .flatMap(user -> userUseCase.updateProfile(user.getId(), dto))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Changer le mot de passe de l'utilisateur connecté")
    @PutMapping("/password")
    public Mono<ResponseEntity<String>> updatePassword(@RequestBody @Valid PasswordUpdateDTO dto) {
        return authUseCase.getCurrentUser()
                .flatMap(user -> userUseCase.updatePassword(user.getId(), dto))
                .then(Mono.just(ResponseEntity.ok("Mot de passe mis à jour avec succès")));
    }

    @Operation(summary = "Lister les permissions de l'utilisateur connecté")
    @GetMapping("/me/permissions")
    public Flux<PermissionEntity> getMyPermissions() {
        return authUseCase.getCurrentUser()
                .flatMapMany(user -> userUseCase.getUserPermissions(user.getId()));
    }
}
