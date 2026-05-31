package com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence;

import java.util.UUID;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends R2dbcRepository<UserEntity, UUID> {
    Mono<UserEntity> findByEmail(String email);

    @Modifying
    @Query("UPDATE users SET organization_id = null, agency_id = null, poste_id = null")
    Mono<Void> clearUserReferences();
}
