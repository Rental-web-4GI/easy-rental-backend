package com.yowyob.easyrental.modules.driver.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("drivers")
public class DriverEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID organizationId;
    private UUID agencyId;

    private String firstname;
    private String lastname;
    private String tel;
    private Integer age;
    private Integer gender; // 0: Homme, 1: Femme

    // URLs stockées après upload via MediaUseCaseImpl
    private String profilUrl;
    private String cniUrl;
    private String drivingLicenseUrl;

    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, INACTIVE

    @Builder.Default
    private Double rating = 0.0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    @JsonIgnore
    private boolean isNewRecord = false;

    @Override
    @Transient
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
