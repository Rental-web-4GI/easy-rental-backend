package com.yowyob.easyrental.modules.agency.dto;

import java.util.UUID;

public record AgencyResponseDTO(
    UUID id,
    UUID organizationId,
    String name,
    String description,
    String address,
    String aliasAddress,
    String city,
    String country,
    String postalCode,
    String region,
    Double latitude,
    Double longitude,
    Double geofenceRadius,
    String email,
    String phone,
    UUID managerId,
    Boolean is24Hours,
    String timezone,
    String workingHours,
    Boolean allowOnlineBooking,
    Double depositPercentage,
    String logoUrl,
    String primaryColor,
    String secondaryColor
) {}
