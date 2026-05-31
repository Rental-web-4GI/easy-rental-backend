package com.yowyob.easyrental.modules.rental.dto;

import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import java.math.BigDecimal;
import java.util.UUID;

public record RentalInitResponse(
    boolean isAllowed, // True si chauffeur inclus, False sinon
    String message,
    UUID rentalId, // Null si isAllowed est false
    BigDecimal totalAmount,
    BigDecimal depositAmount,
    BigDecimal commissionAmount,
    AgencyResponseDTO agencyDetails // Pour contacter l'agence si refusé
) {}
