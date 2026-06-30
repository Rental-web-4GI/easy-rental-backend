package com.yowyob.easyrental.modules.staff.infrastructure.adapter.out.email;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.modules.staff.domain.StaffOnboardingCredentials;
import com.yowyob.easyrental.modules.staff.domain.port.out.StaffOnboardingEmailPort;
import com.yowyob.easyrental.shared.exception.ValidationException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Sends staff onboarding credentials via SMTP.
 *
 * @author Easy Rental Team
 * @since 2026-06-26
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easy-rental.mail.enabled", havingValue = "true")
public class SmtpStaffOnboardingEmailAdapter implements StaffOnboardingEmailPort {

    private final JavaMailSender mailSender;
    private final EasyRentalProperties easyRentalProperties;

    @Override
    public Mono<Void> sendCredentials(StaffOnboardingCredentials credentials) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(easyRentalProperties.getMail().getFrom());
                helper.setTo(credentials.email());
                helper.setSubject("Vos accès Easy Rental Agence");
                helper.setText(buildBody(credentials), true);
                mailSender.send(message);
            } catch (Exception ex) {
                throw new ValidationException("STAFF_EMAIL_FAILED: Unable to send onboarding email");
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String buildBody(StaffOnboardingCredentials credentials) {
        return """
                <p>Bonjour %s %s,</p>
                <p>Votre compte agent Easy Rental a été créé. Connectez-vous à la plateforme agence :</p>
                <p><strong>URL :</strong> <a href="%s">%s</a></p>
                <p><strong>Email :</strong> %s</p>
                <p><strong>Mot de passe temporaire :</strong> %s</p>
                <p>Changez votre mot de passe après la première connexion.</p>
                """.formatted(
                credentials.firstname(),
                credentials.lastname(),
                credentials.agencyLoginUrl(),
                credentials.agencyLoginUrl(),
                credentials.email(),
                credentials.temporaryPassword());
    }
}
