package com.yowyob.easyrental.modules.rental.domain;

import com.yowyob.easyrental.shared.enums.RentalStatus;
import com.yowyob.easyrental.shared.enums.RentalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("rentals")
public class RentalEntity implements Persistable<UUID> {
    @Id
    private UUID id;

    private UUID clientId; // Nullable pour les walk-ins
    private String clientName; // Pour les walk-ins
    private String clientPhone;
    private String clientEmail; // NOUVEAU
    private String cniNumber;   // NOUVEAU

    private UUID agencyId;
    private UUID vehicleId;
    private UUID driverId;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private RentalStatus status;
    private RentalType rentalType;

    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal commissionAmount;
    private BigDecimal depositAmount;

    // R2 pricing/tracking fields
    private BigDecimal rentalAmount;
    private BigDecimal cautionAmount;

    @Builder.Default
    private BigDecimal rentalAmountPaid = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cautionAmountPaid = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cautionHeld = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cautionDeducted = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal cautionRefunded = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal supplementDue = BigDecimal.ZERO;

    private BigDecimal requestedUpfront;

    private Integer startOdometer;
    private Integer endOdometer;

    @Builder.Default
    private BigDecimal trackedKm = BigDecimal.ZERO;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNewRecord = false;

    @Override
    public boolean isNew() {
        return isNewRecord || id == null;
    }
}
