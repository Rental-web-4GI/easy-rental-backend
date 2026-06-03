package com.yowyob.easyrental.modules.staff.infrastructure;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence.StaffRepository;
import com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.persistence.StaffRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaffRepositoryAdapterTest {

    @Mock
    private StaffRepository staffRepository;

    @InjectMocks
    private StaffRepositoryAdapter adapter;

    @Test
    void shouldFindStaffById() {
        UUID id = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(id).email("staff@test.com").build();
        when(staffRepository.findById(id)).thenReturn(Mono.just(user));

        StepVerifier.create(adapter.findById(id))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void shouldFindStaffByEmail() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("staff@test.com").build();
        when(staffRepository.findByEmail("staff@test.com")).thenReturn(Mono.just(user));

        StepVerifier.create(adapter.findByEmail("staff@test.com"))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void shouldFindAllStaffByAgencyId() {
        UUID agencyId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).agencyId(agencyId).build();
        when(staffRepository.findAllStaffByAgencyId(agencyId)).thenReturn(Flux.just(user));

        StepVerifier.create(adapter.findAllStaffByAgencyId(agencyId))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void shouldSaveStaff() {
        UserEntity user = UserEntity.builder().id(UUID.randomUUID()).email("save@test.com").build();
        when(staffRepository.save(user)).thenReturn(Mono.just(user));

        StepVerifier.create(adapter.save(user))
                .expectNext(user)
                .verifyComplete();
    }

    @Test
    void shouldFindOrgIdByStaffId() {
        UUID staffId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(staffRepository.findOrgIdByStaffId(staffId)).thenReturn(Mono.just(orgId));

        StepVerifier.create(adapter.findOrgIdByStaffId(staffId))
                .expectNext(orgId)
                .verifyComplete();
    }
}
