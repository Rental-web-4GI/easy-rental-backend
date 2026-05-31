package com.yowyob.easyrental.modules.organization.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("organizations")
public class OrganizationEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;

    // Legal & Business
    private String registrationNumber;
    private String taxNumber;
    private String businessLicense; // Path to file or URL

    // Contact & Location
    private String address;
    private String city;
    @Builder.Default
    private String country = "CM";
    private String postalCode;
    private String region;
    private String phone;
    private String email;
    private String website;

    // Status
    @Builder.Default
    private Boolean isVerified = false;
    private LocalDateTime verificationDate;

    // Metrics (Counters)
    @Builder.Default
    private Integer currentAgencies = 0;
    @Builder.Default
    private Integer currentVehicles = 0;
    @Builder.Default
    private Integer currentDrivers = 0;
    @Builder.Default
    private Integer currentUsers = 0;

    // Settings
    @Builder.Default
    private String timezone = "Africa/Douala";
    private String logoUrl;

    // Subscription
    private UUID subscriptionPlanId;
    private LocalDateTime subscriptionExpiresAt;
    @Builder.Default
    private Boolean subscriptionAutoRenew = true;

    // Financial Metrics
    @Builder.Default
    private Integer totalRentals = 0;
    @Builder.Default
    private Double monthlyRevenue = 0.0;
    @Builder.Default
    private Double yearlyRevenue = 0.0;

    @Transient
    @Builder.Default
    @JsonIgnore
    private boolean isNewRecord = false;

    @Override
    @Transient
    public boolean isNew() {
        return isNewRecord || id == null;
    }

    @Builder.Default
    private Boolean isDriverBookingRequired = true;
}
