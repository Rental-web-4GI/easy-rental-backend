package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.shared.enums.RentalType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record RentalInitRequest(
    @NotNull UUID vehicleId,
    @NotNull UUID driverId, // Obligatoire selon la logique métier
    @NotNull LocalDateTime startDate,
    @NotNull LocalDateTime endDate,
    @NotNull RentalType rentalType, // DAILY ou HOURLY
    @NotNull String clientPhone
) {}
