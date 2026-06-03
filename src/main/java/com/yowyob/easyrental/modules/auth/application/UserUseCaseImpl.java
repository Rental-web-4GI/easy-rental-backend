package com.yowyob.easyrental.modules.auth.application;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.PasswordUpdateDTO;
import com.yowyob.easyrental.modules.auth.dto.UserProfileUpdateDTO;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import com.yowyob.easyrental.modules.permission.domain.port.out.PermissionRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.port.in.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserUseCaseImpl implements UserUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepositoryPort permissionRepository;

    @Transactional
    public Mono<UserEntity> updateProfile(UUID userId, UserProfileUpdateDTO dto) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")))
            .flatMap(user -> {
                if (dto.firstname() != null) {
                    user.setFirstname(dto.firstname());
                }
                if (dto.lastname() != null) {
                    user.setLastname(dto.lastname());
                }
                user.setFullname(user.getFirstname() + " " + user.getLastname());
                return userRepository.save(user);
            });
    }

    @Transactional
    public Mono<Void> updatePassword(UUID userId, PasswordUpdateDTO dto) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new RuntimeException("Utilisateur non trouvé")))
            .flatMap(user -> {
                if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
                    return Mono.error(new RuntimeException("L'ancien mot de passe est incorrect"));
                }
                user.setPassword(passwordEncoder.encode(dto.newPassword()));
                return userRepository.save(user).then();
            });
    }

    public Flux<PermissionEntity> getUserPermissions(UUID userId) {
        return userRepository.findById(userId)
            .flatMapMany(user -> {
                if ("ADMIN".equals(user.getRole()) || "ORGANIZATION".equals(user.getRole())) {
                    return permissionRepository.findAll(); // Ils ont tous les droits
                }
                if (user.getPosteId() != null) {
                    return permissionRepository.findByPosteId(user.getPosteId());
                }
                return Flux.empty();
            });
    }
}
