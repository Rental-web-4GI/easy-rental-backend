package com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.email;

import com.yowyob.easyrental.modules.staff.domain.StaffOnboardingCredentials;
import com.yowyob.easyrental.modules.staff.domain.port.out.StaffOnboardingEmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Logs onboarding credentials when SMTP is disabled (local dev).
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "easy-rental.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingStaffOnboardingEmailAdapter implements StaffOnboardingEmailPort {

    @Override
    public Mono<Void> sendCredentials(StaffOnboardingCredentials credentials) {
        return Mono.fromRunnable(() -> log.info(
                "Staff onboarding email (SMTP disabled) — to={}, password={}, agencyUrl={}. "
                        + "Configure easy-rental.mail.enabled=true for production delivery.",
                credentials.email(),
                credentials.temporaryPassword(),
                credentials.agencyLoginUrl()))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
