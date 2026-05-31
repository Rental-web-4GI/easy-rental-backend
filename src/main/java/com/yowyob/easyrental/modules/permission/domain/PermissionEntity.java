package com.yowyob.easyrental.modules.permission.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("permissions")
public class PermissionEntity {
    @Id
    private UUID id;
    private String name;
    private String description;
    private String tag;
    private String module;
}
