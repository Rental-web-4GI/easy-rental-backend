package com.yowyob.easyrental.modules.staff.dto;

import java.util.UUID;

public record StaffRequestDTO(
    String firstname,
    String lastname,
    String email,
    UUID agencyId,
    UUID posteId
) {}