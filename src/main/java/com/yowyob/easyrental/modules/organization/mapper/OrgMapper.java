package com.yowyob.easyrental.modules.organization.mapper;

import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.dto.OrgResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class OrgMapper {

    public OrgResponseDTO toDto(OrganizationEntity entity) {
        if (entity == null) {
            return null;
        }
        return new OrgResponseDTO(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            entity.getOwnerId(),
            entity.getRegistrationNumber(),
            entity.getTaxNumber(),
            entity.getBusinessLicense(),
            entity.getAddress(),
            entity.getCity(),
            entity.getCountry(),
            entity.getPostalCode(),
            entity.getRegion(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getWebsite(),
            entity.getIsVerified(),
            entity.getVerificationDate(),
            entity.getCurrentAgencies(),
            entity.getCurrentVehicles(),
            entity.getCurrentDrivers(),
            entity.getTimezone(),
            entity.getLogoUrl(),
            entity.getSubscriptionPlanId(),
            entity.getSubscriptionExpiresAt(),
            entity.getTotalRentals(),
            entity.getMonthlyRevenue(),
            entity.getYearlyRevenue(),
            entity.getIsDriverBookingRequired()
        );
    }
}
