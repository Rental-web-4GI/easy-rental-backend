package com.yowyob.easyrental.modules.vehicle.dto;

import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import java.util.List;

public record ScheduleUpdateDTO(
    List<ScheduleRequestDTO> schedules
) {}
