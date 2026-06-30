package com.yowyob.easyrental.modules.driver.infrastructure.adapter.in.web;

import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.driver.domain.port.in.DriverUseCase;
import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import com.yowyob.easyrental.modules.driver.dto.DriverDetailResponseDTO;
import com.yowyob.easyrental.modules.driver.dto.DriverStatusUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Management", description = "Gestion des conducteurs et leurs documents")
@SecurityRequirement(name = "bearerAuth")
public class DriverController {

    private final DriverUseCase driverUseCase;

    @Operation(summary = "Créer un conducteur avec fichiers (Profil, CNI, Permis)")
    @PostMapping(value = "/org/{orgId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<DriverResponseDTO>> create(
            @PathVariable UUID orgId,
            @Parameter(description = "ID de l'agence d'affectation", required = true)
            @RequestPart("agencyId") String agencyIdStr,

            @Parameter(required = true) @RequestPart("firstname") String firstname,
            @Parameter(required = true) @RequestPart("lastname") String lastname,
            @Parameter(required = true) @RequestPart("tel") String tel,
            @Parameter(required = true) @RequestPart("age") String ageStr,
            @Parameter(description = "0: Homme, 1: Femme", required = true)
            @RequestPart("gender") String genderStr,

            @Parameter(description = "Photo de profil", required = true)
            @RequestPart("profil") FilePart profilFile,

            @Parameter(description = "Scan de la CNI", required = true)
            @RequestPart("cni") FilePart cniFile,

            @Parameter(description = "Scan du permis de conduire", required = true)
            @RequestPart("license") FilePart licenseFile,

            @Parameter(description = "Numéro CNI")
            @RequestPart(value = "cniNumber", required = false) String cniNumber,
            @Parameter(description = "Numéro permis")
            @RequestPart(value = "licenseNumber", required = false) String licenseNumber,
            @Parameter(description = "Expiration permis (YYYY-MM-DD)")
            @RequestPart(value = "licenseExpiry", required = false) String licenseExpiryStr,
            @Parameter(description = "Années d'expérience")
            @RequestPart(value = "yearsExperience", required = false) String yearsExperienceStr,

            @Parameter(description = "Prix horaire (XAF)")
            @RequestPart(value = "pricePerHour", required = false) String pricePerHourStr,
            @Parameter(description = "Prix journalier (XAF)")
            @RequestPart(value = "pricePerDay", required = false) String pricePerDayStr,
            @Parameter(description = "Prix mensuel (XAF)")
            @RequestPart(value = "pricePerMonth", required = false) String pricePerMonthStr
    ) {
        // Conversion des types
        UUID agencyId = UUID.fromString(agencyIdStr);
        Integer age = Integer.parseInt(ageStr);
        Integer gender = Integer.parseInt(genderStr);
        java.time.LocalDate licenseExpiry = (licenseExpiryStr != null && !licenseExpiryStr.isBlank())
            ? java.time.LocalDate.parse(licenseExpiryStr) : null;
        Integer yearsExperience = (yearsExperienceStr != null && !yearsExperienceStr.isBlank())
            ? Integer.parseInt(yearsExperienceStr) : null;
        BigDecimal pricePerHour = parseOptionalPrice(pricePerHourStr);
        BigDecimal pricePerDay = parseOptionalPrice(pricePerDayStr);
        BigDecimal pricePerMonth = parseOptionalPrice(pricePerMonthStr);

        return driverUseCase.createDriver(
                orgId, agencyId, firstname, lastname, tel, age, gender,
                cniNumber, licenseNumber, licenseExpiry, yearsExperience,
                profilFile, cniFile, licenseFile,
                pricePerHour, pricePerDay, pricePerMonth
        ).map(ResponseEntity::ok);
    }

    private static BigDecimal parseOptionalPrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return new BigDecimal(value);
    }

    @Operation(summary = "Lister les conducteurs d'une organisation")
    @GetMapping("/org/{orgId}")
    public Flux<DriverResponseDTO> getByOrg(@PathVariable UUID orgId) {
        return driverUseCase.getDriversByOrg(orgId);
    }

    @Operation(summary = "Lister les conducteurs d'une agence")
    @GetMapping("/agency/{agencyId}")
    public Flux<DriverResponseDTO> getByAgency(@PathVariable UUID agencyId) {
        return driverUseCase.getDriversByAgency(agencyId);
    }

    @Operation(summary = "Détails d'un conducteur")
    @GetMapping("/{id}")
    public Mono<ResponseEntity<DriverResponseDTO>> getById(@PathVariable UUID id) {
        return driverUseCase.getDriverById(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtenir les détails complets (Planning + Prix) d'un conducteur")
    @GetMapping("/{id}/details")
    public Mono<ResponseEntity<DriverDetailResponseDTO>> getDriverDetails(@PathVariable UUID id) {
        return driverUseCase.getDriverDetails(id).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour le prix du chauffeur")
    @PutMapping("/{id}/pricing")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<DriverDetailResponseDTO>> updatePricing(
            @PathVariable UUID id,
            @RequestBody PricingUpdateDTO request) {
        return driverUseCase.updateDriverPricing(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Ajouter des indisponibilités (Planning) au chauffeur")
    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<DriverDetailResponseDTO>> updateSchedule(
            @PathVariable UUID id,
            @RequestBody ScheduleUpdateDTO request) {
        return driverUseCase.updateDriverSchedules(id, request).map(ResponseEntity::ok);
    }

    @Operation(summary = "Mettre à jour le statut du chauffeur (ACTIVE / INACTIVE)")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ORGANIZATION') or hasRole('STAFF')")
    public Mono<ResponseEntity<DriverResponseDTO>> updateStatus(
            @PathVariable UUID id,
            @RequestBody DriverStatusUpdateDTO request) {
        return driverUseCase.updateDriverStatus(id, request.status()).map(ResponseEntity::ok);
    }

    @Operation(summary = "Changer l'agence d'un conducteur")
    @PutMapping("/{id}/change-agency")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<DriverResponseDTO>> changeAgency(
            @PathVariable UUID id,
            @RequestParam UUID newAgencyId) {
        return driverUseCase.changeAgency(id, newAgencyId).map(ResponseEntity::ok);
    }

    @Operation(summary = "Supprimer un conducteur")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public Mono<ResponseEntity<Void>> delete(@PathVariable UUID id) {
        return driverUseCase.deleteDriver(id).then(Mono.just(ResponseEntity.noContent().build()));
    }
}
