package com.yowyob.easyrental.modules.subscription.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("subscription_plans")
public class SubscriptionPlanEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer durationDays;
    private Integer maxVehicles;
    private Integer maxDrivers;
    private Integer maxAgencies;
    private Integer maxUsers;
    @Builder.Default
    private Boolean hasGeofencing = false;
    @Builder.Default
    private Boolean hasChat = false;

    @Transient
    @Builder.Default
    @JsonIgnore
    private boolean isNewRecord = false;

    @Override
    @Transient
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}