package com.yowyob.easyrental.modules.rental.dto;

import java.math.BigDecimal;

/**
 * Request body for the checkout settlement (agency validates the return).
 *
 * <p>L'agent saisit le <b>coût total des dommages</b> constatés (jugement libre,
 * la comparaison check-in/check-out sert de justificatif). Le système en déduit :
 * retenue sur caution = min(damageCost, cautionHeld), remboursement = le reste,
 * et supplément dû (créance) = max(0, damageCost − cautionHeld).</p>
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public record CheckoutSettlementRequest(BigDecimal damageCost, String reason) {
}
