package com.yowyob.easyrental.shared.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ScheduleRequestDTO(
    @NotNull LocalDateTime startDate,
    @NotNull LocalDateTime endDate,
    @NotNull String status, // ex: MAINTENANCE, SICK, UNAVAILABLE
    String reason // Le motif
) {}
