package com.yowyob.easyrental.modules.vehicle.dto;

import java.math.BigDecimal;

public record PricingUpdateDTO(
    BigDecimal pricePerHour,
    BigDecimal pricePerDay
) {}
