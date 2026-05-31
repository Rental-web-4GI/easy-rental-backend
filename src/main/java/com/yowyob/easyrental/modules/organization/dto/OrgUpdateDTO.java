package com.yowyob.easyrental.modules.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO pour la mise à jour des informations de l'organisation.
 * Validations spécifiques au Cameroun (OHADA) :
 *  - Téléphone : 9 chiffres (ex: 678123456)
 *  - RCCM      : ex. RC/DLA/2020/B/1234
 *  - NIU       : ex. M000123456789A (14 chars : M ou P, 12 chiffres, 1 lettre maj)
 */
public record OrgUpdateDTO(
    @NotNull
    String name,
    @NotNull
    String description,
    @NotNull
    String address,
    @NotNull
    String city,
    @NotNull
    String postalCode,
    @NotNull
    String region,
    @NotNull
    @Pattern(
        regexp = "^[0-9]{9}$",
        message = "Le numéro de téléphone doit contenir exactement 9 chiffres (ex: 678123456)"
    )
    String phone,
    @NotNull
    @Email
    String email,
    @NotNull
    String website,
    @NotNull
    String timezone,
    @NotNull
    String logoUrl,
    @NotNull
    @Pattern(
        regexp = "^RC/[A-Z]{2,3}/\\d{4}/[A-Z]/\\d+$",
        message = "Format RCCM invalide. Exemple attendu : RC/DLA/2020/B/1234"
    )
    String registrationNumber,
    @NotNull
    @Pattern(
        regexp = "^[MP]\\d{12}[A-Z]$",
        message = "Format NIU invalide. Exemple attendu : M000123456789A (14 caractères)"
    )
    String taxNumber,
    @NotNull
    Boolean isDriverBookingRequired
) {}
