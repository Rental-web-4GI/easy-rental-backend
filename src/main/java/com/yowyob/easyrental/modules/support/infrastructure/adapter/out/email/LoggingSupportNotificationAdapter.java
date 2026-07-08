package com.yowyob.easyrental.modules.support.infrastructure.adapter.out.email;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.modules.support.domain.port.out.SupportNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easy-rental.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingSupportNotificationAdapter implements SupportNotificationPort {

    private final EasyRentalProperties easyRentalProperties;

    @Override
    public Mono<Void> notifyAdminNewMessage(String visitorEmail, String preview, String consoleUrl) {
        return Mono.fromRunnable(() -> log.info(
                "Support message (SMTP disabled) — admin={}, from={}, preview={}, console={}",
                easyRentalProperties.getSupport().getAdminEmail(),
                visitorEmail,
                preview,
                consoleUrl))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    @Override
    public Mono<Void> notifyVisitorReplyAvailable(String visitorEmail, String helpUrl) {
        return Mono.fromRunnable(() -> log.info(
                "Support reply notification (SMTP disabled) — to={}, helpUrl={}",
                visitorEmail,
                helpUrl))
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }
}
