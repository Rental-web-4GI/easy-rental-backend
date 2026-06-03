package com.yowyob.easyrental.modules.permission.domain.port.in;

import com.yowyob.easyrental.modules.permission.dto.PermissionResponseDTO;
import reactor.core.publisher.Flux;

/**
 * Incoming port for permission metadata use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface PermissionUseCase {

    Flux<PermissionResponseDTO> getAllPermissions();
}
