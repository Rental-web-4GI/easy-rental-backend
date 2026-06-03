package com.yowyob.easyrental.modules.auth.domain.port.in;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.dto.PasswordUpdateDTO;
import com.yowyob.easyrental.modules.auth.dto.UserProfileUpdateDTO;
import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for user use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface UserUseCase {
    Mono<UserEntity> updateProfile(UUID userId, UserProfileUpdateDTO dto);
    Mono<Void> updatePassword(UUID userId, PasswordUpdateDTO dto);
    Flux<PermissionEntity> getUserPermissions(UUID userId);
}
