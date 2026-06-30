package com.yowyob.easyrental.modules.rental.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.yowyob.easyrental.shared.enums.PaymentMethod;
import com.yowyob.easyrental.shared.enums.RentalType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgencyRentalRequest(
    @NotNull @JsonAlias("client_name") String clientName,
    @NotNull @JsonAlias("client_phone") String clientPhone,
    @JsonAlias("client_email") String clientEmail,
    @JsonAlias("cni_number") String cniNumber,
    @NotNull @JsonAlias("vehicle_id") UUID vehicleId,
    @JsonAlias("driver_id") UUID driverId,
    @NotNull @JsonAlias("start_date") LocalDateTime startDate,
    @NotNull @JsonAlias("end_date") LocalDateTime endDate,
    @NotNull @JsonAlias("rental_type") RentalType rentalType,
    /** Walk-in counter payment (defaults to 60 % deposit when null). */
    @JsonAlias("initial_payment_amount") BigDecimal initialPaymentAmount,
    @JsonAlias("payment_method") PaymentMethod paymentMethod
) {}
