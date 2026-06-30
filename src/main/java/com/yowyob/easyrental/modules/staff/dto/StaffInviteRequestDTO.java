package com.yowyob.easyrental.modules.staff.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to provision a staff member on kernel (account + invite).
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
public record StaffInviteRequestDTO(
        @NotBlank String firstname,
        @NotBlank String lastname,
        @NotBlank @Email String email,
        @NotNull @JsonProperty("agency_id") @JsonAlias("agencyId") UUID agencyId,
        @NotNull @JsonProperty("kernel_role_id") @JsonAlias("kernelRoleId") UUID kernelRoleId
) {
}
