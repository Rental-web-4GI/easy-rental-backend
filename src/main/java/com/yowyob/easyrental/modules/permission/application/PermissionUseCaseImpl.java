package com.yowyob.easyrental.modules.permission.application;

import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import com.yowyob.easyrental.modules.permission.domain.port.in.PermissionUseCase;
import com.yowyob.easyrental.modules.permission.dto.PermissionResponseDTO;
import com.yowyob.easyrental.modules.permission.domain.port.out.PermissionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class PermissionUseCaseImpl implements PermissionUseCase {

    private final PermissionRepositoryPort permissionRepository;

    @Override
    public Flux<PermissionResponseDTO> getAllPermissions() {
        return permissionRepository.findAll().map(this::toDto);
    }

    private PermissionResponseDTO toDto(PermissionEntity entity) {
        return new PermissionResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTag(),
                entity.getModule()
        );
    }
}
