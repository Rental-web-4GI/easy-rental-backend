package com.yowyob.easyrental.modules.media.domain;

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
@Table("medias")
public class MediaEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private String filename;
    private String originalFilename;
    private String fileType;
    private String fileUrl;
    private UUID uploaderId;
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
