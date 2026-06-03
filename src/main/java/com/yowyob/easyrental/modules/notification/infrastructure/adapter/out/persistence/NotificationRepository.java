package com.yowyob.easyrental.modules.notification.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.notification.domain.NotificationEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends R2dbcRepository<NotificationEntity, UUID> {

    // --- CLIENT ---
    @Query("SELECT * FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' ORDER BY created_at"
            + " DESC")
    Flux<NotificationEntity> findNotificationsByClientId(UUID clientId);

    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :clientId AND resource_type = 'CLIENT' AND is_read"
            + " = false")
    Mono<Long> countUnreadByClientId(UUID clientId);

    // --- AGENCE ---
    @Query("SELECT * FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' ORDER BY created_at"
            + " DESC")
    Flux<NotificationEntity> findNotificationsByAgencyId(UUID agencyId);

    @Query("SELECT COUNT(*) FROM notifications WHERE resource_id = :agencyId AND resource_type = 'AGENCY' AND is_read"
            + " = false")
    Mono<Long> countUnreadByAgencyId(UUID agencyId);

    // --- ORGANISATION (Agrégation) ---
    // Récupère les notifications de type 'AGENCY' dont l'ID est dans la liste des agences de l'org
    @Query("SELECT * FROM notifications WHERE resource_type = 'AGENCY' AND resource_id IN (:agencyIds) ORDER BY"
            + " created_at DESC")
    Flux<NotificationEntity> findNotificationsByAgencyIds(List<UUID> agencyIds);

    @Query("SELECT COUNT(*) FROM notifications WHERE resource_type = 'AGENCY' AND resource_id IN (:agencyIds) AND"
            + " is_read = false")
    Mono<Long> countUnreadByAgencyIds(List<UUID> agencyIds);

    // --- DRIVER ---
    @Query("SELECT * FROM notifications WHERE resource_id = :driverId AND resource_type = 'DRIVER' ORDER BY created_at"
            + " DESC")
    Flux<NotificationEntity> findNotificationsByDriverId(UUID driverId);
}
