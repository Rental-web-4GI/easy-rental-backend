package com.yowyob.easyrental.modules.permission.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.permission.domain.PermissionEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import java.util.UUID;

public interface PermissionRepository extends R2dbcRepository<PermissionEntity, UUID> {

    @Query("SELECT p.* FROM permissions p JOIN postes_permissions pp ON p.id = pp.permission_id WHERE pp.poste_id = :posteId")
    Flux<PermissionEntity> findByPosteId(UUID posteId);
}
