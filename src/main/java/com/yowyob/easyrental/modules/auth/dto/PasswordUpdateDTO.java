package com.yowyob.easyrental.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordUpdateDTO(
    @NotBlank(message = "L'ancien mot de passe est requis") String oldPassword,
    @NotBlank(message = "Le nouveau mot de passe est requis") String newPassword
) {}
