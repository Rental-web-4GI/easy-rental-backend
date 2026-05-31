package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.rental.domain.RentalEntity;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleResponseDTO;

public record RentalDetailResponseDTO(
    RentalEntity rental,
    VehicleResponseDTO vehicle,
    DriverResponseDTO driver, // Peut être null si aucun chauffeur n'est sélectionné
    AgencyResponseDTO agency
) {}
