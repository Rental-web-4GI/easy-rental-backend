package com.yowyob.easyrental.modules.support.infrastructure.adapter.out.email;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.modules.support.domain.port.out.SupportNotificationPort;
import com.yowyob.easyrental.shared.exception.ValidationException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easy-rental.mail.enabled", havingValue = "true")
public class SmtpSupportNotificationAdapter implements SupportNotificationPort {

    private final JavaMailSender mailSender;
    private final EasyRentalProperties easyRentalProperties;

    @Override
    public Mono<Void> notifyAdminNewMessage(String visitorEmail, String preview, String consoleUrl) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(easyRentalProperties.getMail().getFrom());
                helper.setTo(easyRentalProperties.getSupport().getAdminEmail());
                helper.setSubject("Easy Rental — Nouveau message support");
                helper.setText("""
                        <p>Un utilisateur a envoyé un message sur le chat support.</p>
                        <p><strong>Email :</strong> %s</p>
                        <p><strong>Extrait :</strong> %s</p>
                        <p><a href="%s">Ouvrir la console admin</a></p>
                        """.formatted(visitorEmail, preview, consoleUrl), true);
                mailSender.send(message);
            } catch (Exception ex) {
                throw new ValidationException("SUPPORT_EMAIL_FAILED: Unable to notify admin");
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> notifyVisitorReplyAvailable(String visitorEmail, String helpUrl) {
        return Mono.fromRunnable(() -> {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(easyRentalProperties.getMail().getFrom());
                helper.setTo(visitorEmail);
                helper.setSubject("Easy Rental — Réponse à votre message");
                helper.setText("""
                        <p>Bonjour,</p>
                        <p>Un administrateur Easy Rental a répondu à votre message.</p>
                        <p>Consultez votre messagerie sur notre page d'aide :</p>
                        <p><a href="%s">%s</a></p>
                        """.formatted(helpUrl, helpUrl), true);
                mailSender.send(message);
            } catch (Exception ex) {
                throw new ValidationException("SUPPORT_EMAIL_FAILED: Unable to notify visitor");
            }
        }).subscribeOn(Schedulers.boundedElastic()).then();
    }
}
