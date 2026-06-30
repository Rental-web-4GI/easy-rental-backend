package com.yowyob.easyrental.modules.staff.domain;

/**
 * Credentials emailed to a newly provisioned staff member.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public record StaffOnboardingCredentials(
        String email,
        String temporaryPassword,
        String agencyLoginUrl,
        String firstname,
        String lastname
) {
}
