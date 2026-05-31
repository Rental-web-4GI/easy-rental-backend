package com.yowyob.easyrental.modules.vehicle.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VehicleRequestDTO(
    UUID agencyId,
    UUID categoryId,
   String licencePlate,
    String vinNumber,
    String brand,
    String model,
    LocalDateTime yearProduction,
    Integer places,
    Double kilometrage,
    String statut,
    String color,
    String transmission,
    
    // Équipements
    FonctionnalitiesDTO functionalities,
    
    // Technique
    EngineDTO engineDetails,
    FuelEfficiencyDTO fuelEfficiency,
    
    
    // Assurance
    InsuranceDTO insuranceDetails,
    
    String[] description,
    String[] images
) {}