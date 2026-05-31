package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.shared.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PaymentRequest(
    @NotNull BigDecimal amount,
    @NotNull PaymentMethod method
) {}
