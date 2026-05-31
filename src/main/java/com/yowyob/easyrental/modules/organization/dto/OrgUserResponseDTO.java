package com.yowyob.easyrental.modules.organization.dto;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;

public record OrgUserResponseDTO(
    UserEntity user,
    OrgResponseDTO organization
) {}
