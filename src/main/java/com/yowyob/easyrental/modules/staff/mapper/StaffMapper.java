package com.yowyob.easyrental.modules.staff.mapper;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.poste.dto.PosteResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class StaffMapper {

    public StaffResponseDTO toDto(UserEntity user, PosteResponseDTO poste) {
        if (user == null) return null;

        return new StaffResponseDTO(
            user.getId(),
            user.getFirstname(),
            user.getLastname(),
            user.getEmail(),
            user.getAgencyId(),
            poste,
            user.getStatus(),
            user.getHiredAt()
        );
    }
}