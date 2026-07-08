package com.yowyob.easyrental.modules.rental.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.yowyob.easyrental.shared.enums.RentalType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record RentalInitRequest(
    @NotNull @JsonAlias("vehicle_id") UUID vehicleId,
    @JsonAlias("driver_id") UUID driverId,
    @NotNull @JsonAlias("start_date") LocalDateTime startDate,
    @NotNull @JsonAlias("end_date") LocalDateTime endDate,
    @NotNull @JsonAlias("rental_type") RentalType rentalType,
    @NotNull @JsonAlias("client_phone") String clientPhone
) {}
