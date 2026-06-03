package com.yowyob.easyrental.modules.subscription.domain;

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
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("subscriptions") // FIX : Pointer vers la table d'historique
public class SubscriptionEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID organizationId;
    private String planType;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

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