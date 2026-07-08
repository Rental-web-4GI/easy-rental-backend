package com.yowyob.easyrental.modules.support.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table("support_threads")
public class SupportThreadEntity implements Persistable<UUID> {

    @Id
    private UUID id;
    private String visitorEmail;
    private String visitorName;
    private String visitorSessionId;
    private String visitorRole;
    private UUID userId;
    private String subject;
    private String status;
    private Integer adminUnreadCount;
    private LocalDateTime lastMessageAt;
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
