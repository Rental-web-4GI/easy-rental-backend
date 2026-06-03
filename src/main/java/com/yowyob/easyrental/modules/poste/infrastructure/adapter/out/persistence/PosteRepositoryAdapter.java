package com.yowyob.easyrental.modules.poste.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.poste.domain.PosteEntity;
import com.yowyob.easyrental.modules.poste.domain.port.out.PosteRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PosteRepositoryAdapter implements PosteRepositoryPort {

    private final PosteRepository posteRepository;

    @Override
    public Mono<PosteEntity> findById(UUID id) {
        return posteRepository.findById(id);
    }

    @Override
    public Mono<PosteEntity> save(PosteEntity poste) {
        return posteRepository.save(poste);
    }

    @Override
    public Flux<PosteEntity> findAllByOrganizationIdOrSystem(UUID organizationId) {
        return posteRepository.findAllByOrganizationIdOrSystem(organizationId);
    }

    @Override
    public Mono<Void> addPermission(UUID posteId, UUID permissionId) {
        return posteRepository.addPermission(posteId, permissionId);
    }

    @Override
    public Mono<Void> removeAllPermissions(UUID posteId) {
        return posteRepository.removeAllPermissions(posteId);
    }

    @Override
    public Flux<UUID> findPermissionIdsByPosteId(UUID posteId) {
        return posteRepository.findPermissionIdsByPosteId(posteId);
    }

    @Override
    public Mono<Void> deleteOrganizationPostes() {
        return posteRepository.deleteOrganizationPostes();
    }
}
