package com.yowyob.easyrental.modules.poste.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record PosteRequestDTO(
    String name,
    String description,
    @JsonProperty("permission_ids")
    @JsonAlias("permissionIds")
    List<UUID> permissionIds
) {}
