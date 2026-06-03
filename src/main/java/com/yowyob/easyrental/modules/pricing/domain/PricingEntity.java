package com.yowyob.easyrental.modules.pricing.domain;

import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("pricings")
public class PricingEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID organizationId;
    private ResourceType resourceType;
    private UUID resourceId;

    private BigDecimal pricePerHour;
    private BigDecimal pricePerDay;
    @Builder.Default
    private String currency = "XAF";

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Transient @Builder.Default @JsonIgnore private boolean isNewRecord = false;
    @Override public boolean isNew() { return isNewRecord || id == null; }
}
