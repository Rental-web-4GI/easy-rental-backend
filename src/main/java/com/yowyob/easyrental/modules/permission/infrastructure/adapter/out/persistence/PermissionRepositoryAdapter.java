package com.yowyob.easyrental.modules.permission.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import com.yowyob.easyrental.modules.permission.domain.port.out.PermissionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PermissionRepositoryAdapter implements PermissionRepositoryPort {

    private final PermissionRepository permissionRepository;

    @Override
    public Flux<PermissionEntity> findAll() {
        return permissionRepository.findAll();
    }

    @Override
    public Mono<PermissionEntity> findById(UUID id) {
        return permissionRepository.findById(id);
    }

    @Override
    public Flux<PermissionEntity> findByPosteId(UUID posteId) {
        return permissionRepository.findByPosteId(posteId);
    }
}
