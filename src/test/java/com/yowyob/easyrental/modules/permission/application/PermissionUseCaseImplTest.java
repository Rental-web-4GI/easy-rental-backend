package com.yowyob.easyrental.modules.permission.application;

import com.yowyob.easyrental.modules.permission.domain.port.out.PermissionRepositoryPort;
import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionUseCaseImplTest {

    @Mock
    private PermissionRepositoryPort permissionRepository;

    @InjectMocks
    private PermissionUseCaseImpl permissionUseCase;

    @Test
    void shouldReturnAllPermissions() {
        PermissionEntity perm = PermissionEntity.builder()
                .id(UUID.randomUUID()).name("vehicle:read").description("Read vehicles")
                .tag("vehicle").module("vehicle").build();
        when(permissionRepository.findAll()).thenReturn(Flux.just(perm));

        StepVerifier.create(permissionUseCase.getAllPermissions())
                .expectNextMatches(dto -> dto.name().equals("vehicle:read"))
                .verifyComplete();
    }
}
