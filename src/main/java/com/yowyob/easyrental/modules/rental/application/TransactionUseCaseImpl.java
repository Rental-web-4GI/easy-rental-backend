package com.yowyob.easyrental.modules.rental.application;

import com.yowyob.easyrental.modules.rental.dto.TransactionDetailResponseDTO;
import com.yowyob.easyrental.modules.rental.dto.TransactionResponseDTO;
import com.yowyob.easyrental.modules.rental.domain.port.out.PaymentRepositoryPort;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionPlanRepositoryPort;
import com.yowyob.easyrental.modules.subscription.domain.port.out.SubscriptionRepositoryPort;
import com.yowyob.easyrental.modules.auth.domain.port.out.AuthUserPort;
import com.yowyob.easyrental.modules.rental.domain.port.in.RentalUseCase;
import com.yowyob.easyrental.modules.rental.domain.port.in.TransactionUseCase;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionUseCaseImpl implements TransactionUseCase {

    private final PaymentRepositoryPort paymentRepository;
    private final SubscriptionRepositoryPort subscriptionRepository;
    private final SubscriptionPlanRepositoryPort planRepository;
    private final RentalRepositoryPort rentalRepository;

    private final RentalUseCase rentalUseCase;
    private final AuthUserPort authUserPort;

    /**
     * Libellé humain d'un paiement selon sa catégorie R2 (rental_portion vs caution).
     * Les paiements legacy (paymentCategory null) restent « Location ».
     */
    private static String describe(String paymentCategory, String rentalIdShort) {
        String cat = paymentCategory == null ? "" : paymentCategory;
        return switch (cat) {
            case "CAUTION" -> "Caution (escrow) #" + rentalIdShort;
            case "CAUTION_REFUND" -> "Remboursement caution #" + rentalIdShort;
            case "CAUTION_RETENTION" -> "Retenue caution (dommages) #" + rentalIdShort;
            case "SUPPLEMENT_DUE" -> "Supplément dû (créance) #" + rentalIdShort;
            case "SUPPLEMENT_PAID" -> "Supplément encaissé #" + rentalIdShort;
            default -> "Location #" + rentalIdShort;
        };
    }

    // =================================================================================
    // NOUVELLE MÉTHODE : Obtenir les détails complets d'une transaction
    // =================================================================================
    public Mono<TransactionDetailResponseDTO> getTransactionDetails(UUID transactionId) {
        // 1. On cherche d'abord si c'est un paiement de location
        Mono<TransactionDetailResponseDTO> paymentMono = paymentRepository.findById(transactionId)
            .flatMap(payment -> rentalUseCase.getRentalDetails(payment.getRentalId())
                .map(rentalDetails -> new TransactionDetailResponseDTO(
                    payment.getId(),
                    "RENTAL_PAYMENT",
                    payment.getAmount(),
                    "Paiement Location #" + payment.getRentalId().toString().substring(0, 8),
                    payment.getTransactionDate(),
                    payment.getTransactionRef(),
                    "COMPLETED",
                    payment.getPaymentMethod(),
                    rentalDetails,
                    null
                )));

        // 2. Si ce n'est pas un paiement, on cherche si c'est un paiement d'abonnement
        Mono<TransactionDetailResponseDTO> subMono = subscriptionRepository.findById(transactionId)
            .flatMap(sub -> planRepository.findByName(sub.getPlanType())
                .map(plan -> new TransactionDetailResponseDTO(
                    sub.getId(),
                    "SUBSCRIPTION_COST",
                    plan.getPrice().negate(),
                    "Abonnement " + plan.getName(),
                    sub.getStartDate(),
                    "SUB-" + sub.getId().toString().substring(0, 8),
                    sub.getStatus(),
                    null,
                    null,
                    plan
                )));

        // On retourne l'un ou l'autre, ou une erreur si introuvable
        return paymentMono.switchIfEmpty(subMono)
            .switchIfEmpty(Mono.error(new ResourceNotFoundException("Transaction not found")));
    }

    /**
     * Transactions d'un Client (Uniquement ses paiements de location)
     */
    public Flux<TransactionResponseDTO> getClientTransactions(UUID clientId) {
        return paymentRepository.findAllByClientId(clientId)
            .flatMap(payment -> rentalRepository.findById(payment.getRentalId())
                .map(rental -> paymentToTransaction(payment, rental.getId().toString().substring(0, 8))));
    }

    /**
     * Transactions d'une Agence (Uniquement les revenus locatifs)
     */
    public Flux<TransactionResponseDTO> getAgencyTransactions(UUID agencyId) {
        return paymentRepository.findAllByAgencyId(agencyId)
            .flatMap(payment -> rentalRepository.findById(payment.getRentalId())
                .map(rental -> paymentToTransaction(payment, rental.getId().toString().substring(0, 8))));
    }

    /** Construit une transaction ventilée (revenu vs caution) à partir d'un paiement. */
    private TransactionResponseDTO paymentToTransaction(
            com.yowyob.easyrental.modules.rental.domain.PaymentEntity payment, String rentalIdShort) {
        java.math.BigDecimal rentalPortion = payment.getRentalPortion() != null
                ? payment.getRentalPortion() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal cautionPortion = payment.getCautionPortion() != null
                ? payment.getCautionPortion() : java.math.BigDecimal.ZERO;
        // Paiement legacy sans ventilation → tout compte comme revenu location.
        if (payment.getPaymentCategory() == null && payment.getRentalPortion() == null
                && payment.getCautionPortion() == null) {
            rentalPortion = payment.getAmount();
        }
        return new TransactionResponseDTO(
                payment.getId(),
                "RENTAL_PAYMENT",
                payment.getAmount(),
                describe(payment.getPaymentCategory(), rentalIdShort),
                payment.getTransactionDate(),
                payment.getTransactionRef(),
                "COMPLETED",
                payment.getPaymentMethod(),
                payment.getPaymentCategory() == null ? "RENTAL_FEE" : payment.getPaymentCategory(),
                rentalPortion,
                cautionPortion);
    }

    /**
     * Transactions d'une Organisation (Revenus Locatifs - Coûts Abonnements)
     * Fusionne les flux et trie par date décroissante.
     */
    public Flux<TransactionResponseDTO> getOrganizationTransactions(UUID orgId) {

        // 1. Flux des revenus locatifs (Positif)
        Flux<TransactionResponseDTO> rentalIncomeFlux = paymentRepository.findAllRentalPaymentsByOrganizationId(orgId)
            .map(payment -> paymentToTransaction(payment, "org"));

        // 2. Flux des dépenses d'abonnement (Négatif ou Informatif)
        Flux<TransactionResponseDTO> subscriptionExpenseFlux = subscriptionRepository
                .findAllByOrganizationIdOrderByStartDateDesc(orgId)
            .flatMap(sub -> planRepository.findByName(sub.getPlanType())
                .map(plan -> new TransactionResponseDTO(
                    sub.getId(),
                    "SUBSCRIPTION_COST",
                    plan.getPrice().negate(), // On met en négatif pour indiquer une dépense
                    "Abonnement " + plan.getName(),
                    sub.getStartDate(),
                    "SUB-" + sub.getId().toString().substring(0, 8),
                    sub.getStatus(),
                    null,
                    "SUBSCRIPTION",
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO
                )));

        // 3. Fusion et Tri
        return Flux.merge(rentalIncomeFlux, subscriptionExpenseFlux)
            .sort(Comparator.comparing(TransactionResponseDTO::date).reversed());
    }

    @Override
    public Flux<TransactionResponseDTO> getClientTransactionsByEmail(String email) {
        return authUserPort.findByEmail(email)
                .flatMapMany(user -> getClientTransactions(user.getId()));
    }
}
