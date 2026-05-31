package com.yowyob.easyrental.modules.poste.domain;

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
@Table("postes")
public class PosteEntity implements Persistable<UUID> {
    @Id
    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;
    private LocalDateTime createdAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    @Transient
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
