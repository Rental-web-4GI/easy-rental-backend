package com.yowyob.easyrental.modules.poste.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.poste.domain.PosteEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface PosteRepository extends R2dbcRepository<PosteEntity, UUID> {

    // Récupère les postes de l'organisation + les postes par défaut (NULL)
    @Query("SELECT * FROM postes WHERE organization_id = :organizationId OR organization_id IS NULL")
    Flux<PosteEntity> findAllByOrganizationIdOrSystem(UUID organizationId);

    // Gestion manuelle de la relation ManyToMany
    @Modifying
    @Query("INSERT INTO postes_permissions (poste_id, permission_id) VALUES (:posteId, :permissionId)")
    Mono<Void> addPermission(UUID posteId, UUID permissionId);

    @Modifying
    @Query("DELETE FROM postes_permissions WHERE poste_id = :posteId")
    Mono<Void> removeAllPermissions(UUID posteId);

    // Récupérer les ID des permissions
    @Query("SELECT permission_id FROM postes_permissions WHERE poste_id = :posteId")
    Flux<UUID> findPermissionIdsByPosteId(UUID posteId);

    @Modifying
    @Query("DELETE FROM postes WHERE organization_id IS NOT NULL")
    Mono<Void> deleteOrganizationPostes();
}
