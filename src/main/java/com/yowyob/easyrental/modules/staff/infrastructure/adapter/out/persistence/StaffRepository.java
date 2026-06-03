package com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface StaffRepository extends R2dbcRepository<UserEntity, UUID> {

    /**
     * Utilisé pour vérifier si un utilisateur est déjà employé dans une
     * organisation précise
     */
    Mono<UserEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Récupérer tout le personnel d'une agence
     */
    Flux<UserEntity> findAllByAgencyId(UUID agencyId);

    /**
     * Récupérer tout le personnel d'une organisation
     */
    Flux<UserEntity> findAllByOrganizationId(UUID organizationId);

    // Liste le personnel d'une organisation (tous les utilisateurs ayant un orgId)
    Flux<UserEntity> findAllByOrganizationIdAndRoleNot(UUID organizationId, String role);

    // Recherche un membre par son email (pour éviter les doublons à la création)
    Mono<UserEntity> findByEmail(String email);
    
    @Query("SELECT * FROM users WHERE organization_id = :organizationId AND role = 'STAFF'")
    Flux<UserEntity> findAllStaffByOrganizationId(UUID organizationId);

    @Query("SELECT * FROM users WHERE agency_id = :agencyId AND role = 'STAFF'")
    Flux<UserEntity> findAllStaffByAgencyId(UUID agencyId);
    
    @Query("SELECT organization_id FROM users WHERE id = :staffId")
    Mono<UUID> findOrgIdByStaffId(UUID staffId);

    @Query("""
                SELECT COUNT(*) > 0
                FROM staff s
                JOIN postes p ON s.poste_id = p.id
                JOIN postes_permissions pp ON p.id = pp.poste_id
                JOIN permissions perm ON pp.permission_id = perm.id
                WHERE s.user_id = :id
                AND s.organization_id = :orgId
                AND perm.tag = :permissionTag
            """)
    Mono<Boolean> checkStaffPermission(UUID id, UUID orgId, String permissionTag);
}