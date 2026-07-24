package com.yowyob.easyrental.modules.organization.dto;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;

import java.util.UUID;

public record OrgUserResponseDTO(
    UserEntity user,
    OrgResponseDTO organization,
    UUID defaultAgencyId
) {}
