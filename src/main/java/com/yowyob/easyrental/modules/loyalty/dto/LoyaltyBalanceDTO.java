package com.yowyob.easyrental.modules.loyalty.dto;

/**
 * Loyalty balance snapshot exposed to API consumers.
 *
 * @author Easy Rental Team
 * @since 2026-07-26
 */
public record LoyaltyBalanceDTO(int balance, String tier, int annualPoints) {
}
