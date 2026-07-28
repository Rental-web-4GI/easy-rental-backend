package com.yowyob.easyrental.modules.rental.dto;

import java.math.BigDecimal;

public record RentalPricingBreakdown(
        BigDecimal rentalAmount,
        BigDecimal cautionAmount,
        BigDecimal commissionAmount,
        BigDecimal totalDue,
        BigDecimal requestedUpfront
) {}
