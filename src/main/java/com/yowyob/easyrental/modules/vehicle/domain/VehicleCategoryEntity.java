package com.yowyob.easyrental.modules.vehicle.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Table("vehicle_categories")
public class VehicleCategoryEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;

    @Transient @Builder.Default @JsonIgnore private boolean isNewRecord = false;
    @Override public boolean isNew() { return isNewRecord || id == null; }
}