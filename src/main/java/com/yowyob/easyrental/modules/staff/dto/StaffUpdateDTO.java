package com.yowyob.easyrental.modules.staff.dto;

import java.util.UUID;

public record StaffUpdateDTO(
    String firstname,
    String lastname,
    UUID agencyId,
    UUID posteId,
    String status
) {}