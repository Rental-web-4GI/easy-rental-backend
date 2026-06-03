package com.yowyob.easyrental.modules.poste.domain.port.out;

import com.yowyob.easyrental.modules.poste.domain.PosteEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for poste persistence.
 */
public interface PosteRepositoryPort {

    Mono<PosteEntity> findById(UUID id);

    Mono<PosteEntity> save(PosteEntity poste);

    Flux<PosteEntity> findAllByOrganizationIdOrSystem(UUID organizationId);

    Mono<Void> addPermission(UUID posteId, UUID permissionId);

    Mono<Void> removeAllPermissions(UUID posteId);

    Flux<UUID> findPermissionIdsByPosteId(UUID posteId);

    Mono<Void> deleteOrganizationPostes();
}
