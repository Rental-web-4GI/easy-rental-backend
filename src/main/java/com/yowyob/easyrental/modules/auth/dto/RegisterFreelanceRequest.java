package com.yowyob.easyrental.modules.auth.dto;

import java.util.UUID;

/**
 * Payload pour l'inscription d'un particulier "Freelance"
 * (une organisation individuelle avec une seule agence auto-créée).
 */
public record RegisterFreelanceRequest(
    String firstname,
    String lastname,
    String email,
    String phone,
    String city,
    String password,
    UUID planId
) {}
