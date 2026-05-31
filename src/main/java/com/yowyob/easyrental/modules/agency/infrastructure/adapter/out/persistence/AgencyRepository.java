package com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface AgencyRepository extends R2dbcRepository<AgencyEntity, UUID> {
    Flux<AgencyEntity> findAllByOrganizationId(UUID organizationId);

    @Query("SELECT organization_id FROM agencies WHERE id = :agencyId")
    Mono<UUID> findOrgIdByAgencyId(UUID agencyId);

    // Recherche d'agences par mot-clé (nom/adresse) et/ou ville
    @Query("SELECT * FROM agencies WHERE " +
           "(:keyword::text IS NULL OR name ILIKE '%' || :keyword || '%' OR address ILIKE '%' || :keyword || '%') " +
           "AND (:city::text IS NULL OR city ILIKE '%' || :city || '%')")
    Flux<AgencyEntity> searchAgencies(String keyword, String city);
}
