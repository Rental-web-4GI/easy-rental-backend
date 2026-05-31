package com.yowyob.easyrental.modules.audit.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.audit.domain.AuditEntity;

import java.util.UUID;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends R2dbcRepository<AuditEntity, UUID> {
}
