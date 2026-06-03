package com.yowyob.easyrental.modules.review.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Table("reviews")
public class ReviewEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private UUID resourceId;
    private ResourceType resourceType;
    private Integer rating;
    private String comment;
    private String authorName;
    private LocalDateTime createdAt;

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
