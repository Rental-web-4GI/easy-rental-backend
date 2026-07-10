package com.yowyob.easyrental.modules.organization.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Organization owner registration payload.
 * Jackson uses SNAKE_CASE globally ({@code org_name}); {@code orgName} is accepted as alias
 * for frontend clients that still send camelCase.
 *
 * @author Easy Rental Team
 * @since 2026-03-01
 */
public record OrgRegisterRequest(
        String firstname,
        String lastname,
        String email,
        String password,
        @JsonAlias("orgName") String orgName) {
}
