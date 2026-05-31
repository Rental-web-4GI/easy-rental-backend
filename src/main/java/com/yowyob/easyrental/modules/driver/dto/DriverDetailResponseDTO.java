package com.yowyob.easyrental.modules.driver.dto;

import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.review.dto.ReviewResponseDTO;
import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import java.util.List;

public record DriverDetailResponseDTO(
    DriverResponseDTO driver,
    PricingEntity pricing,
    List<ScheduleEntity> schedule,
    Double rating,
    List<ReviewResponseDTO> reviews,
    Boolean isDriverBookingRequired
) {}
