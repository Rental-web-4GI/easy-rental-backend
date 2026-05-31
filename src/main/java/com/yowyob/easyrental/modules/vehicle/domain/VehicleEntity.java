package com.yowyob.easyrental.modules.vehicle.domain;

import io.r2dbc.postgresql.codec.Json;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("vehicles")
public class VehicleEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    private UUID organizationId;
    private UUID agencyId;
    private UUID categoryId;

    private String licencePlate;
    private String vinNumber;
    private String brand;
    private String model;
    private LocalDateTime yearProduction;
    private Integer places;
    private Double kilometrage;
    private String transmission;
    private String color;
    @Builder.Default
    private String statut = "AVAILABLE"; // AVAILABLE, RENTED, MAINTENANCE, UNAVAILABLE

    // Stockage JSONB
    private Json functionalities;
    private Json engineDetails;
    private Json fuelEfficiency;
    private Json insuranceDetails;
    private Json descriptionList;
    private Json imagesList;

    @Builder.Default
    private Double rating = 0.0;

    private LocalDateTime createdAt;

    @Transient @Builder.Default @JsonIgnore private boolean isNewRecord = false;
    @Override public boolean isNew() { return isNewRecord || id == null; }
}
