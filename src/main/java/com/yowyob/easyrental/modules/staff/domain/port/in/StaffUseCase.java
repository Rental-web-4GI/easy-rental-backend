package com.yowyob.easyrental.modules.staff.domain.port.in;

import com.yowyob.easyrental.modules.staff.dto.KernelRoleResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffInviteRequestDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffInviteResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffRequestDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffResponseDTO;
import com.yowyob.easyrental.modules.staff.dto.StaffUpdateDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for staff use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface StaffUseCase {
    Mono<StaffResponseDTO> addStaffToOrganization(UUID orgId, StaffRequestDTO request);
    Mono<StaffInviteResponseDTO> provisionStaffToOrganization(UUID orgId, StaffInviteRequestDTO request);
    Flux<KernelRoleResponseDTO> listKernelRoles(UUID orgId);
    Flux<StaffResponseDTO> getStaffByOrganization(UUID orgId);
    Flux<StaffResponseDTO> getStaffByAgency(UUID agencyId);
    Mono<StaffResponseDTO> getStaffById(UUID id);
    Mono<Void> deleteStaff(UUID id);
    Mono<StaffResponseDTO> updateStaff(UUID staffId, StaffUpdateDTO request);
}
