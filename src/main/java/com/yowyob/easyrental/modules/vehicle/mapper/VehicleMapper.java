package com.yowyob.easyrental.modules.vehicle.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.dto.EngineDTO;
import com.yowyob.easyrental.modules.vehicle.dto.FonctionnalitiesDTO;
import com.yowyob.easyrental.modules.vehicle.dto.FuelEfficiencyDTO;
import com.yowyob.easyrental.modules.vehicle.dto.InsuranceDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VehicleMapper {

    private final ObjectMapper objectMapper;

    public VehicleResponseDTO toDto(VehicleEntity entity, VehicleCategoryEntity category, PricingEntity pricing) {
        if (entity == null) {
            return null;
        }

        try {
            // Désérialisation des champs JSONB
            FonctionnalitiesDTO functionalities = entity.getFunctionalities() != null ?
                objectMapper.readValue(entity.getFunctionalities().asString(), FonctionnalitiesDTO.class) : null;

            EngineDTO engineDetails = entity.getEngineDetails() != null ?
                objectMapper.readValue(entity.getEngineDetails().asString(), EngineDTO.class) : null;

            FuelEfficiencyDTO fuelEfficiency = entity.getFuelEfficiency() != null ?
                objectMapper.readValue(entity.getFuelEfficiency().asString(), FuelEfficiencyDTO.class) : null;

            InsuranceDTO insuranceDetails = entity.getInsuranceDetails() != null ?
                objectMapper.readValue(entity.getInsuranceDetails().asString(), InsuranceDTO.class) : null;

            String[] description = entity.getDescriptionList() != null ?
                objectMapper.readValue(entity.getDescriptionList().asString(), String[].class) : new String[0];

            String[] images = entity.getImagesList() != null ?
                objectMapper.readValue(entity.getImagesList().asString(), String[].class) : new String[0];

            return new VehicleResponseDTO(
                entity.getId(),
                entity.getAgencyId(),
                entity.getCategoryId(),
                entity.getLicencePlate(),
                entity.getVinNumber(),
                entity.getBrand(),
                entity.getModel(),
                entity.getYearProduction(),
                entity.getPlaces(),
                entity.getKilometrage(),
                entity.getStatut(),
                entity.getColor(),
                entity.getTransmission(),
                functionalities,
                engineDetails,
                fuelEfficiency,
                insuranceDetails,
                description,
                images,
                pricing // Injection du prix
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du mapping des données JSON du véhicule", e);
        }
    }
}
