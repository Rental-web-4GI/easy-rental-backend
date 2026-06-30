package com.yowyob.easyrental.modules.staff.domain.port.out;

import com.yowyob.easyrental.modules.staff.domain.StaffOnboardingCredentials;
import reactor.core.publisher.Mono;

/**
 * Outgoing port to email staff onboarding credentials.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public interface StaffOnboardingEmailPort {

    Mono<Void> sendCredentials(StaffOnboardingCredentials credentials);
}
