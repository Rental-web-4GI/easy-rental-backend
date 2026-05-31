package com.yowyob.easyrental.modules.auth.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class UserEntity implements Persistable<UUID> {
    @Id
    @NonNull
    private UUID id;
    private String firstname;
    private String lastname;
    private String fullname;
    private String email;
    private String password;
    private String role;

    // Nouveaux champs Staff
    private UUID organizationId;
    private UUID agencyId;
    private UUID posteId;
    private String status;
    private LocalDateTime hiredAt;

    // Champ technique pour indiquer à R2DBC si c'est un INSERT ou UPDATE
    @Transient
    @Builder.Default
    @JsonIgnore
    private boolean isNewRecord = false;

    @Override
    @Transient
    public boolean isNew() {
        // Si isNewRecord est true OU si l'id est null, c'est un INSERT
        return isNewRecord || id == null;
    }
}
