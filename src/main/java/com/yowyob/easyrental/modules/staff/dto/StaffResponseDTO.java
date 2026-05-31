package com.yowyob.easyrental.modules.staff.dto;

import com.yowyob.easyrental.modules.poste.dto.PosteResponseDTO;
import java.time.LocalDateTime;
import java.util.UUID;

public record StaffResponseDTO(
    UUID id,
    String firstname,
    String lastname,
    String email,
    UUID agencyId,
    PosteResponseDTO poste,
    String status,
    LocalDateTime hiredAt
) {}