package com.yowyob.easyrental.modules.driver.mapper;

import com.yowyob.easyrental.modules.driver.domain.DriverEntity;
import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.pricing.domain.PricingEntity;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper {
    public DriverResponseDTO toDto(DriverEntity entity, PricingEntity pricing) {
        if (entity == null) {
            return null;
        }
        return new DriverResponseDTO(
            entity.getId(),
            entity.getOrganizationId(),
            entity.getAgencyId(),
            entity.getFirstname(),
            entity.getLastname(),
            entity.getTel(),
            entity.getAge(),
            entity.getGender(),
            entity.getProfilUrl(),
            entity.getCniUrl(),
            entity.getDrivingLicenseUrl(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            pricing
        );
    }
}
