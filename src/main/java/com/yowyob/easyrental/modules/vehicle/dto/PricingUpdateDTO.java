package com.yowyob.easyrental.modules.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.math.BigDecimal;

public record PricingUpdateDTO(
    @JsonAlias("price_per_hour") BigDecimal pricePerHour,
    @JsonAlias("price_per_day") BigDecimal pricePerDay,
    @JsonAlias("price_per_month") BigDecimal pricePerMonth
) {}
