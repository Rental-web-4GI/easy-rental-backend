package com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.email;

import com.yowyob.easyrental.modules.rental.domain.port.out.RentalEmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Fallback rental email adapter — logs instead of sending.
 * Active when {@code easy-rental.mail.enabled} is false or absent (dev/local).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "easy-rental.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingRentalEmailAdapter implements RentalEmailPort {

    @Override
    public Mono<Void> sendCautionDeduction(String toEmail, BigDecimal deduction, String reason, BigDecimal refunded) {
        log.info("[mail-disabled] Caution deduction to {}: retained={} reason='{}' refunded={}",
                toEmail, deduction, reason, refunded);
        return Mono.empty();
    }

    @Override
    public Mono<Void> sendCautionFullyRefunded(String toEmail, BigDecimal refunded) {
        log.info("[mail-disabled] Caution fully refunded to {}: {}", toEmail, refunded);
        return Mono.empty();
    }
}
