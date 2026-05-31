package com.yowyob.easyrental.modules.media.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaRepository extends R2dbcRepository<MediaEntity, UUID> {
}
