package com.yowyob.easyrental.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record UserProfileUpdateDTO(
    @NotBlank(message = "Le prénom est requis") String firstname,
    @NotBlank(message = "Le nom est requis") String lastname
) {}
