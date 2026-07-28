package com.yowyob.easyrental.modules.rating.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * R2DBC entity mapped on the {@code ratings} table (migration 26 from R1).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("ratings")
public class RatingEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    private UUID rentalId;
    private RaterType raterType;
    private UUID raterId;
    private TargetType targetType;
    private UUID targetId;
    private Short stars;
    private String comment;
    private Instant createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
