package com.yowyob.easyrental.modules.staff.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Staff invite response, optionally exposing dev credentials when SMTP is disabled.
 *
 * @author Easy Rental Team
 * @since 2026-06-29
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffInviteResponseDTO(
        StaffResponseDTO staff,
        String temporaryPassword,
        String agencyLoginUrl,
        Boolean emailSent
) {
}
