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
