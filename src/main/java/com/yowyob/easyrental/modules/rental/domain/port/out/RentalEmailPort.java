package com.yowyob.easyrental.modules.rental.domain.port.out;

import java.math.BigDecimal;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for rental lifecycle emails (R2 — caution settlement).
 *
 * <p>In-app notifications go through {@code NotificationUseCase}; this port
 * covers the email channel that the plan requires for caution events.</p>
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
public interface RentalEmailPort {

    /**
     * Email the client when a deduction is applied on their caution at checkout.
     *
     * @param toEmail       client email (may be null → no-op)
     * @param deduction     amount retained
     * @param reason        retention motive
     * @param refunded      amount refunded
     */
    Mono<Void> sendCautionDeduction(String toEmail, BigDecimal deduction, String reason, BigDecimal refunded);

    /**
     * Email the client when their caution is fully refunded (no deduction).
     *
     * @param toEmail   client email (may be null → no-op)
     * @param refunded  full amount refunded
     */
    Mono<Void> sendCautionFullyRefunded(String toEmail, BigDecimal refunded);
}
