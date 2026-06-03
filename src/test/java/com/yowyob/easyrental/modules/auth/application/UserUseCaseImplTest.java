package com.yowyob.easyrental.modules.auth.application;

import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.PasswordUpdateDTO;
import com.yowyob.easyrental.modules.auth.dto.UserProfileUpdateDTO;
import com.yowyob.easyrental.modules.permission.domain.port.out.PermissionRepositoryPort;
import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PermissionRepositoryPort permissionRepository;

    @InjectMocks
    private UserUseCaseImpl userUseCase;

    @Test
    void shouldUpdateProfile() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).firstname("Old").lastname("Name").build();
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(userUseCase.updateProfile(userId, new UserProfileUpdateDTO("New", "User")))
                .expectNextMatches(u -> u.getFirstname().equals("New") && u.getFullname().equals("New User"))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenUserNotFoundForProfileUpdate() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        StepVerifier.create(userUseCase.updateProfile(userId, new UserProfileUpdateDTO("A", "B")))
                .expectErrorMatches(e -> e.getMessage().equals("Utilisateur non trouvé"))
                .verify();
    }

    @Test
    void shouldUpdatePassword() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).password("hash").build();
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("old", "hash")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("newHash");
        when(userRepository.save(any())).thenReturn(Mono.just(user));

        StepVerifier.create(userUseCase.updatePassword(userId, new PasswordUpdateDTO("old", "new")))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenOldPasswordIncorrect() {
        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).password("hash").build();
        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        StepVerifier.create(userUseCase.updatePassword(userId, new PasswordUpdateDTO("wrong", "new")))
                .expectErrorMatches(e -> e.getMessage().equals("L'ancien mot de passe est incorrect"))
                .verify();
    }

    @Test
    void shouldReturnAllPermissionsForAdmin() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Mono.just(
                UserEntity.builder().id(userId).role("ADMIN").build()));
        PermissionEntity perm = PermissionEntity.builder()
                .id(UUID.randomUUID()).name("vehicle:read").build();
        when(permissionRepository.findAll()).thenReturn(Flux.just(perm));

        StepVerifier.create(userUseCase.getUserPermissions(userId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldReturnPermissionsByPoste() {
        UUID userId = UUID.randomUUID();
        UUID posteId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Mono.just(
                UserEntity.builder().id(userId).role("STAFF").posteId(posteId).build()));
        when(permissionRepository.findByPosteId(posteId)).thenReturn(Flux.just(
                PermissionEntity.builder().id(UUID.randomUUID()).name("staff:read").build()));

        StepVerifier.create(userUseCase.getUserPermissions(userId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyPermissionsWhenNoPoste() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Mono.just(
                UserEntity.builder().id(userId).role("STAFF").posteId(null).build()));

        StepVerifier.create(userUseCase.getUserPermissions(userId))
                .verifyComplete();
    }
}
