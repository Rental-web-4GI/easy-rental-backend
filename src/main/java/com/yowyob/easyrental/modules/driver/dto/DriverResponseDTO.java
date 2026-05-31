package com.yowyob.easyrental.modules.driver.dto;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import java.time.LocalDateTime;
import java.util.UUID;

public record DriverResponseDTO(
    UUID id,
    UUID organizationId,
    UUID agencyId,
    String firstname,
    String lastname,
    String tel,
    Integer age,
    Integer gender,
    String profilUrl,
    String cniUrl,
    String drivingLicenseUrl,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    PricingEntity pricing
) {}
