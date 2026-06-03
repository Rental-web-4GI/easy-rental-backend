package com.yowyob.easyrental.modules.subscription.mapper;

import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.subscription.domain.SubscriptionPlanEntity;
import com.yowyob.easyrental.modules.subscription.dto.SubscriptionResponseDTO;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class SubscriptionMapper {

    public SubscriptionResponseDTO toResponseDTO(OrganizationEntity org, SubscriptionPlanEntity plan) {
        boolean isExpired = org.getSubscriptionExpiresAt() != null
                && org.getSubscriptionExpiresAt().isBefore(LocalDateTime.now());
        
        return new SubscriptionResponseDTO(
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getDurationDays(),
                plan.getMaxVehicles(),
                plan.getMaxAgencies(),
                org.getSubscriptionExpiresAt(),
                isExpired
        );
    }
}