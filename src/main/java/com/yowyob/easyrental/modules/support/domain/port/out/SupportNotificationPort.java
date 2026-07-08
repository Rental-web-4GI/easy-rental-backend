package com.yowyob.easyrental.modules.support.domain.port.out;

import reactor.core.publisher.Mono;

public interface SupportNotificationPort {
    Mono<Void> notifyAdminNewMessage(String visitorEmail, String preview, String consoleUrl);
    Mono<Void> notifyVisitorReplyAvailable(String visitorEmail, String helpUrl);
}
