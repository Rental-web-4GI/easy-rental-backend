package com.yowyob.easyrental.modules.support.application;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.modules.support.domain.SupportConstants;
import com.yowyob.easyrental.modules.support.domain.SupportMessageEntity;
import com.yowyob.easyrental.modules.support.domain.SupportThreadEntity;
import com.yowyob.easyrental.modules.support.domain.port.in.SupportUseCase;
import com.yowyob.easyrental.modules.support.domain.port.out.SupportNotificationPort;
import com.yowyob.easyrental.modules.support.domain.port.out.SupportRepositoryPort;
import com.yowyob.easyrental.modules.support.dto.SupportConfigResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportConversationResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportMessageRequest;
import com.yowyob.easyrental.modules.support.dto.SupportMessageResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportReplyRequest;
import com.yowyob.easyrental.modules.support.dto.SupportSendResponseDTO;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportUseCaseImpl implements SupportUseCase {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String STATUS_OPEN = "OPEN";

    private final SupportRepositoryPort supportRepository;
    private final SupportNotificationPort supportNotificationPort;
    private final EasyRentalProperties easyRentalProperties;

    @Override
    public Mono<SupportConfigResponseDTO> getPublicConfig() {
        return Mono.just(new SupportConfigResponseDTO(
                easyRentalProperties.getSupport().getAdminEmail(),
                easyRentalProperties.getSupport().getHelpUrl(),
                easyRentalProperties.getSupport().getConsoleUrl()));
    }

    @Override
    @Transactional
    public Mono<SupportSendResponseDTO> sendUserMessage(SupportMessageRequest request) {
        if (!StringUtils.hasText(request.body())) {
            return Mono.error(new ValidationException("Message body is required"));
        }

        String sessionId = normalizeSessionId(request.visitorSessionId());
        String email = normalizeEmail(request.email());
        if (!StringUtils.hasText(email) && !StringUtils.hasText(sessionId)) {
            return Mono.error(new ValidationException("Email or visitor session is required"));
        }

        LocalDateTime now = LocalDateTime.now();
        Mono<SupportThreadEntity> threadMono = resolveThreadForMessage(
                request.threadId(), email, sessionId, now, request);

        return threadMono.flatMap(thread -> {
            thread.setLastMessageAt(now);
            if (StringUtils.hasText(request.authorName())) {
                thread.setVisitorName(request.authorName().trim());
            }
            if (StringUtils.hasText(request.visitorRole())) {
                thread.setVisitorRole(request.visitorRole().trim());
            }
            thread.setAdminUnreadCount((thread.getAdminUnreadCount() != null ? thread.getAdminUnreadCount() : 0) + 1);

            SupportMessageEntity message = SupportMessageEntity.builder()
                    .id(UUID.randomUUID())
                    .threadId(thread.getId())
                    .senderRole(ROLE_USER)
                    .body(request.body().trim())
                    .createdAt(now)
                    .isNewRecord(true)
                    .build();

            return supportRepository.saveThread(thread)
                    .then(supportRepository.saveMessage(message))
                    .then(supportNotificationPort.notifyAdminNewMessage(
                            displayEmail(thread),
                            truncate(request.body(), 120),
                            easyRentalProperties.getSupport().getConsoleUrl()))
                    .thenReturn(new SupportSendResponseDTO(
                            thread.getId(),
                            "Message envoyé. Un administrateur vous répondra ici."));
        });
    }

    @Override
    public Flux<SupportMessageResponseDTO> getConversationMessages(String email, String visitorSessionId) {
        return loadConversationMessages(email, visitorSessionId).map(this::mapMessage);
    }

    @Override
    public Flux<SupportConversationResponseDTO> listConversationsForAdmin() {
        return supportRepository.findAllThreads()
                .collectList()
                .flatMapMany(threads -> Flux.fromIterable(groupConversations(threads)));
    }

    @Override
    public Flux<SupportMessageResponseDTO> getConversationMessagesForAdmin(String email, String visitorSessionId) {
        return loadConversationMessages(email, visitorSessionId).map(this::mapMessage);
    }

    @Override
    @Transactional
    public Mono<SupportMessageResponseDTO> replyAsAdmin(
            String email,
            String visitorSessionId,
            SupportReplyRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return resolveLatestThread(email, visitorSessionId)
                .switchIfEmpty(Mono.error(new ValidationException("Support conversation not found")))
                .flatMap(thread -> {
                    thread.setLastMessageAt(now);
                    thread.setAdminUnreadCount(0);
                    SupportMessageEntity message = SupportMessageEntity.builder()
                            .id(UUID.randomUUID())
                            .threadId(thread.getId())
                            .senderRole(ROLE_ADMIN)
                            .body(request.body().trim())
                            .createdAt(now)
                            .isNewRecord(true)
                            .build();

                    return supportRepository.saveThread(thread)
                            .then(markConversationAsRead(email, visitorSessionId))
                            .then(supportRepository.saveMessage(message))
                            .then(supportNotificationPort.notifyVisitorReplyAvailable(
                                    displayEmail(thread),
                                    easyRentalProperties.getSupport().getHelpUrl()))
                            .thenReturn(mapMessage(message));
                });
    }

    @Override
    @Transactional
    public Mono<Void> markConversationAsRead(String email, String visitorSessionId) {
        Flux<SupportThreadEntity> threads = conversationThreads(email, visitorSessionId);
        return threads.flatMap(thread -> {
                    thread.setAdminUnreadCount(0);
                    return supportRepository.saveThread(thread);
                })
                .then();
    }

    private Mono<SupportThreadEntity> resolveThreadForMessage(
            UUID threadId,
            String email,
            String sessionId,
            LocalDateTime now,
            SupportMessageRequest request) {
        if (threadId != null) {
            return supportRepository.findThreadById(threadId)
                    .switchIfEmpty(Mono.error(new ValidationException("Support thread not found")));
        }
        if (StringUtils.hasText(email)) {
            return supportRepository.findLatestThreadByVisitorEmail(email)
                    .switchIfEmpty(Mono.defer(() -> Mono.just(newAnonymousOrAccountThread(email, null, now, request))));
        }
        return supportRepository.findLatestThreadByVisitorSessionId(sessionId)
                .switchIfEmpty(Mono.defer(() -> Mono.just(newAnonymousOrAccountThread(null, sessionId, now, request))));
    }

    private SupportThreadEntity newAnonymousOrAccountThread(
            String email,
            String sessionId,
            LocalDateTime now,
            SupportMessageRequest request) {
        String resolvedEmail = StringUtils.hasText(email)
                ? email
                : SupportConstants.anonymousEmail(sessionId);
        return SupportThreadEntity.builder()
                .id(UUID.randomUUID())
                .visitorEmail(resolvedEmail)
                .visitorSessionId(sessionId)
                .visitorName(request.authorName())
                .visitorRole(request.visitorRole())
                .subject("Support Easy Rental")
                .status(STATUS_OPEN)
                .adminUnreadCount(0)
                .createdAt(now)
                .isNewRecord(true)
                .build();
    }

    private Mono<SupportThreadEntity> resolveLatestThread(String email, String visitorSessionId) {
        String normalizedEmail = normalizeEmail(email);
        String sessionId = normalizeSessionId(visitorSessionId);
        if (StringUtils.hasText(normalizedEmail)) {
            return supportRepository.findLatestThreadByVisitorEmail(normalizedEmail);
        }
        if (StringUtils.hasText(sessionId)) {
            return supportRepository.findLatestThreadByVisitorSessionId(sessionId);
        }
        return Mono.empty();
    }

    private Flux<SupportMessageEntity> loadConversationMessages(String email, String visitorSessionId) {
        String normalizedEmail = normalizeEmail(email);
        String sessionId = normalizeSessionId(visitorSessionId);
        if (StringUtils.hasText(normalizedEmail)) {
            return supportRepository.findAllMessagesByVisitorEmail(normalizedEmail);
        }
        if (StringUtils.hasText(sessionId)) {
            return supportRepository.findAllMessagesByVisitorSessionId(sessionId);
        }
        return Flux.error(new ValidationException("Email or visitor session is required"));
    }

    private Flux<SupportThreadEntity> conversationThreads(String email, String visitorSessionId) {
        String normalizedEmail = normalizeEmail(email);
        String sessionId = normalizeSessionId(visitorSessionId);
        if (StringUtils.hasText(normalizedEmail)) {
            return supportRepository.findAllThreadsByVisitorEmail(normalizedEmail);
        }
        if (StringUtils.hasText(sessionId)) {
            return supportRepository.findAllThreadsByVisitorSessionId(sessionId);
        }
        return Flux.empty();
    }

    private List<SupportConversationResponseDTO> groupConversations(List<SupportThreadEntity> threads) {
        Map<String, SupportConversationResponseDTO> grouped = new HashMap<>();
        for (SupportThreadEntity thread : threads) {
            String key = conversationKey(thread);
            SupportConversationResponseDTO existing = grouped.get(key);
            int unread = thread.getAdminUnreadCount() != null ? thread.getAdminUnreadCount() : 0;
            if (existing == null) {
                grouped.put(key, mapConversation(thread, unread));
                continue;
            }
            int mergedUnread = existing.adminUnreadCount() + unread;
            LocalDateTime existingLast = existing.lastMessageAt();
            LocalDateTime threadLast = thread.getLastMessageAt();
            boolean threadIsNewer = threadLast != null
                    && (existingLast == null || threadLast.isAfter(existingLast));
            grouped.put(key, new SupportConversationResponseDTO(
                    key,
                    existing.displayLabel(),
                    existing.visitorEmail(),
                    existing.visitorSessionId(),
                    existing.visitorRole(),
                    mergedUnread,
                    threadIsNewer ? threadLast : existingLast,
                    threadIsNewer ? thread.getId() : existing.canonicalThreadId()
            ));
        }
        return grouped.values().stream()
                .sorted(Comparator.comparing(
                        SupportConversationResponseDTO::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private SupportConversationResponseDTO mapConversation(SupportThreadEntity thread, int unread) {
        return new SupportConversationResponseDTO(
                conversationKey(thread),
                displayLabel(thread),
                SupportConstants.isAnonymousEmail(thread.getVisitorEmail()) ? null : thread.getVisitorEmail(),
                thread.getVisitorSessionId(),
                thread.getVisitorRole(),
                unread,
                thread.getLastMessageAt(),
                thread.getId()
        );
    }

    private String conversationKey(SupportThreadEntity thread) {
        if (StringUtils.hasText(thread.getVisitorSessionId())) {
            return "session:" + thread.getVisitorSessionId();
        }
        return "email:" + thread.getVisitorEmail().trim().toLowerCase();
    }

    private String displayLabel(SupportThreadEntity thread) {
        if (StringUtils.hasText(thread.getVisitorName())) {
            String role = StringUtils.hasText(thread.getVisitorRole()) ? " · " + thread.getVisitorRole() : "";
            return thread.getVisitorName() + role;
        }
        if (SupportConstants.isAnonymousEmail(thread.getVisitorEmail())) {
            return "Visiteur anonyme";
        }
        return thread.getVisitorEmail();
    }

    private String displayEmail(SupportThreadEntity thread) {
        if (SupportConstants.isAnonymousEmail(thread.getVisitorEmail())) {
            return easyRentalProperties.getSupport().getAdminEmail();
        }
        return thread.getVisitorEmail();
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email) || SupportConstants.isAnonymousEmail(email)) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    private String normalizeSessionId(String sessionId) {
        return StringUtils.hasText(sessionId) ? sessionId.trim() : null;
    }

    private SupportMessageResponseDTO mapMessage(SupportMessageEntity entity) {
        return new SupportMessageResponseDTO(
                entity.getId(),
                entity.getThreadId(),
                entity.getSenderRole(),
                entity.getBody(),
                entity.getCreatedAt());
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
