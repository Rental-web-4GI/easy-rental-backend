package com.project.apirental.modules.agency.dto;

import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * DTO de création/mise à jour d'agence.
 * Téléphone Cameroun : exactement 9 chiffres (ex: 678123456)
 */
public record AgencyRequestDTO(
    String name,
    String description,
    String address,
    String city,
    String country,
    String postalCode,
    String region,
    @Pattern(
        regexp = "^[0-9]{9}$",
        message = "Le numéro de téléphone doit contenir exactement 9 chiffres (ex: 678123456)"
    )
    String phone,
    String email,
    UUID managerId,
    Double latitude,
    Double longitude,
    Double geofenceRadius,
    Boolean is24Hours,
    String timezone,
    String workingHours,
    Boolean allowOnlineBooking,
    Double depositPercentage,
    String logoUrl,
    String primaryColor,
    String secondaryColor
) {}