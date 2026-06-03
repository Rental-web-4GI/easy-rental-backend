package com.yowyob.easyrental.modules.agency.mapper;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AgencyMapper {
    public AgencyResponseDTO toDto(AgencyEntity entity) {
        if (entity == null) {
            return null;
        }
        return new AgencyResponseDTO(
            entity.getId(),
            entity.getOrganizationId(),
            entity.getName(),
            entity.getDescription(),
            entity.getAddress(),
            entity.getAliasAddress(), 
            entity.getCity(),
            entity.getCountry(), 
            entity.getPostalCode(), 
            entity.getRegion(), 
            entity.getLatitude(),
            entity.getLongitude(),  
            entity.getGeofenceRadius(),  
            entity.getEmail(),
            entity.getPhone(),
            entity.getManagerId(),
            entity.getIs24Hours(),
            entity.getTimezone(),
            entity.getWorkingHours(),
            entity.getAllowOnlineBooking(),
            entity.getDepositPercentage(),
            entity.getLogoUrl(),
            entity.getPrimaryColor(),
            entity.getSecondaryColor()
        );
    }
}