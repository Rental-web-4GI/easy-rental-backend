package com.yowyob.easyrental.modules.poste.application;

import com.yowyob.easyrental.modules.permission.domain.port.out.PermissionRepositoryPort;
import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import com.yowyob.easyrental.modules.poste.domain.port.out.PosteRepositoryPort;
import com.yowyob.easyrental.modules.poste.domain.PosteEntity;
import com.yowyob.easyrental.modules.poste.dto.PosteRequestDTO;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PosteUseCaseImplTest {

    @Mock
    private PosteRepositoryPort posteRepository;
    @Mock
    private PermissionRepositoryPort permissionRepository;

    @InjectMocks
    private PosteUseCaseImpl posteUseCase;

    @Test
    void shouldRejectUpdateForSystemPoste() {
        UUID id = UUID.randomUUID();
        PosteEntity systemPoste = PosteEntity.builder().id(id).organizationId(null).name("Admin").build();
        when(posteRepository.findById(id)).thenReturn(Mono.just(systemPoste));

        StepVerifier.create(posteUseCase.updatePoste(id, new PosteRequestDTO("X", "Y", List.of())))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    void shouldCreatePoste() {
        UUID orgId = UUID.randomUUID();
        UUID permId = UUID.randomUUID();
        PosteEntity saved = PosteEntity.builder().id(UUID.randomUUID()).organizationId(orgId).name("Manager").build();
        PermissionEntity perm = PermissionEntity.builder().id(permId).name("read").build();

        when(posteRepository.save(any())).thenReturn(Mono.just(saved));
        when(posteRepository.addPermission(saved.getId(), permId)).thenReturn(Mono.empty());
        when(posteRepository.findPermissionIdsByPosteId(saved.getId())).thenReturn(Flux.just(permId));
        when(permissionRepository.findById(permId)).thenReturn(Mono.just(perm));

        StepVerifier.create(posteUseCase.createPoste(orgId, new PosteRequestDTO("Manager", "Desc", List.of(permId))))
                .expectNextMatches(dto -> dto.name().equals("Manager"))
                .verifyComplete();
    }

    @Test
    void shouldGetAvailablePostes() {
        UUID orgId = UUID.randomUUID();
        PosteEntity poste = PosteEntity.builder().id(UUID.randomUUID()).organizationId(orgId).name("Staff").build();
        when(posteRepository.findAllByOrganizationIdOrSystem(orgId)).thenReturn(Flux.just(poste));
        when(posteRepository.findPermissionIdsByPosteId(poste.getId())).thenReturn(Flux.empty());

        StepVerifier.create(posteUseCase.getAvailablePostes(orgId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetPosteById() {
        UUID id = UUID.randomUUID();
        PosteEntity poste = PosteEntity.builder().id(id).organizationId(UUID.randomUUID()).name("Role").build();
        when(posteRepository.findById(id)).thenReturn(Mono.just(poste));
        when(posteRepository.findPermissionIdsByPosteId(id)).thenReturn(Flux.empty());

        StepVerifier.create(posteUseCase.getPosteById(id))
                .expectNextMatches(dto -> dto.name().equals("Role"))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenPosteIdIsNull() {
        StepVerifier.create(posteUseCase.getPosteById(null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
