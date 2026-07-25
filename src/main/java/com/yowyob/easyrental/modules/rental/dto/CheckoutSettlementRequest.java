package com.yowyob.easyrental.modules.rental.dto;

import java.math.BigDecimal;

/**
 * Request body for the checkout settlement (agency validates the return):
 * how much of the held caution is retained, and why.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record CheckoutSettlementRequest(BigDecimal cautionDeduction, String retentionReason) {
}
