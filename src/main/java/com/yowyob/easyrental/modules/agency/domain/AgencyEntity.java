package com.yowyob.easyrental.modules.agency.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("agencies")
public class AgencyEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;
    
    // Address & Location
    private String address;
    private String aliasAddress;
    private String city;
    @Builder.Default
    private String country = "CM";
    private String postalCode;
    private String region;
    private Double latitude;
    private Double longitude;
    private Double geofenceRadius;

    // Contact
    private String email;
    private String phone;
    private UUID managerId;

    // Configuration & Settings
    @Builder.Default
    private Boolean is24Hours = false;
    private String timezone;
    private String currency;
    private String language;
    private String workingHours;
    
    @Builder.Default
    private Boolean allowOnlineBooking = true;
    @Builder.Default
    private Boolean requireDeposit = true;
    private Double depositPercentage;
    private Integer minRentalHours;
    private Integer maxAdvanceBookingDays;

    // Design
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;

    // Metrics (Updated by other services usually)
    @Builder.Default
    private Integer activeVehicles = 0;
    @Builder.Default
    private Integer totalVehicles = 0;
    @Builder.Default
    private Integer activeDrivers = 0;
    @Builder.Default
    private Integer totalDrivers = 0;
    @Builder.Default
    private Integer totalPersonnel = 0;
    @Builder.Default
    private Integer totalRentals = 0;
    @Builder.Default
    private Double monthlyRevenue = 0.0;

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