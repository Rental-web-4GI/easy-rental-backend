package com.yowyob.easyrental.modules.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("notifications")
public class NotificationEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    /**
     * ID de la location associée à cette notification
     */
    @Column("location_id")
    private UUID locationId;

    /**
     * ID de la ressource destinataire (client, chauffeur ou agence)
     */
    @Column("resource_id")
    private UUID resourceId;

    /**
     * Type de ressource (CLIENT, DRIVER, AGENCY)
     */
    @Column("resource_type")
    private String resourceType; // CLIENT, DRIVER, AGENCY

    /**
     * Motif de la notification (RESERVATION, LOCATION_START, LOCATION_END)
     */
    @Column("reason")
    private String reason; // RESERVATION, LOCATION_START, LOCATION_END

    /**
     * ID du véhicule impliqué (optionnel)
     */
    @Column("vehicle_id")
    private UUID vehicleId;

    /**
     * ID du chauffeur impliqué (optionnel)
     */
    @Column("driver_id")
    private UUID driverId;

    /**
     * Date/heure de création de la notification
     */
    @Column("created_at")
    private LocalDateTime createdAt;

    /**
     * Indique si la notification a été lue
     */
    @Column("is_read")
    @Builder.Default
    private Boolean isRead = false;

    /**
     * Détails supplémentaires (message personnalisé, JSON, etc.)
     */
    @Column("details")
    private String details;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
