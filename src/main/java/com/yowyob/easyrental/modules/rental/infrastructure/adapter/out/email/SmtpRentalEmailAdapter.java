package com.yowyob.easyrental.modules.rental.infrastructure.adapter.out.email;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.modules.rental.domain.port.out.RentalEmailPort;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;

/**
 * SMTP rental email adapter. Active when {@code easy-rental.mail.enabled=true}.
 * Failures are logged and swallowed — an email hiccup must never break the
 * checkout settlement flow (the in-app notification already fired).
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easy-rental.mail.enabled", havingValue = "true")
public class SmtpRentalEmailAdapter implements RentalEmailPort {

    private final JavaMailSender mailSender;
    private final EasyRentalProperties easyRentalProperties;

    @Override
    public Mono<Void> sendCautionDeduction(String toEmail, BigDecimal deduction, String reason, BigDecimal refunded) {
        if (toEmail == null || toEmail.isBlank()) {
            return Mono.empty();
        }
        String body = """
                <p>Bonjour,</p>
                <p>Suite à l'inspection de retour de votre véhicule, une retenue a été appliquée sur votre caution :</p>
                <ul>
                  <li><strong>Retenue :</strong> %s XAF</li>
                  <li><strong>Motif :</strong> %s</li>
                  <li><strong>Montant remboursé :</strong> %s XAF</li>
                </ul>
                <p>Pour toute question, contactez l'agence via l'application Easy Rental.</p>
                """.formatted(deduction, reason, refunded);
        return sendHtml(toEmail, "Easy Rental — Retenue sur votre caution", body);
    }

    @Override
    public Mono<Void> sendCautionFullyRefunded(String toEmail, BigDecimal refunded) {
        if (toEmail == null || toEmail.isBlank()) {
            return Mono.empty();
        }
        String body = """
                <p>Bonjour,</p>
                <p>Votre véhicule a été rendu sans dommage constaté. Votre caution vous est
                intégralement remboursée :</p>
                <p><strong>Montant remboursé : %s XAF</strong></p>
                <p>Merci pour votre confiance. À bientôt sur Easy Rental !</p>
                """.formatted(refunded);
        return sendHtml(toEmail, "Easy Rental — Caution remboursée", body);
    }

    private Mono<Void> sendHtml(String to, String subject, String htmlBody) {
        return Mono.<Void>fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(easyRentalProperties.getMail().getFrom());
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);
                mailSender.send(message);
            } catch (Exception ex) {
                log.warn("Rental caution email to {} failed: {}", to, ex.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
