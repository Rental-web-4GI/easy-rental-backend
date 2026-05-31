package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.shared.enums.RentalType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgencyRentalRequest(
    @NotNull String clientName,
    @NotNull String clientPhone,
    String clientEmail, // Optionnel
    String cniNumber,   // Optionnel
    @NotNull UUID vehicleId,
    UUID driverId,      // Optionnel
    @NotNull LocalDateTime startDate,
    @NotNull LocalDateTime endDate,
    @NotNull RentalType rentalType
) {}
