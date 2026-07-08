package com.yowyob.easyrental.modules.support.application;

import com.yowyob.easyrental.config.EasyRentalProperties;
import com.yowyob.easyrental.modules.support.domain.SupportMessageEntity;
import com.yowyob.easyrental.modules.support.domain.SupportThreadEntity;
import com.yowyob.easyrental.modules.support.domain.port.in.SupportUseCase;
import com.yowyob.easyrental.modules.support.domain.port.out.SupportNotificationPort;
import com.yowyob.easyrental.modules.support.domain.port.out.SupportRepositoryPort;
import com.yowyob.easyrental.modules.support.dto.SupportConfigResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportMessageRequest;
import com.yowyob.easyrental.modules.support.dto.SupportMessageResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportReplyRequest;
import com.yowyob.easyrental.modules.support.dto.SupportSendResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportThreadResponseDTO;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
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
        LocalDateTime now = LocalDateTime.now();
        Mono<SupportThreadEntity> threadMono = request.threadId() != null
                ? supportRepository.findThreadById(request.threadId())
                        .switchIfEmpty(Mono.error(new ValidationException("Support thread not found")))
                : Mono.just(SupportThreadEntity.builder()
                        .id(UUID.randomUUID())
                        .visitorEmail(request.email().trim().toLowerCase())
                        .visitorName(request.authorName())
                        .subject("Support Easy Rental")
                        .status(STATUS_OPEN)
                        .adminUnreadCount(0)
                        .createdAt(now)
                        .isNewRecord(true)
                        .build());

        return threadMono.flatMap(thread -> {
            thread.setLastMessageAt(now);
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
                            thread.getVisitorEmail(),
                            truncate(request.body(), 120),
                            easyRentalProperties.getSupport().getConsoleUrl()))
                    .thenReturn(new SupportSendResponseDTO(
                            thread.getId(),
                            "Message envoyé. Consultez votre messagerie pour la réponse de l'administrateur."));
        });
    }

    @Override
    public Flux<SupportMessageResponseDTO> getThreadMessages(UUID threadId) {
        return supportRepository.findMessagesByThreadId(Objects.requireNonNull(threadId))
                .map(this::mapMessage);
    }

    @Override
    public Flux<SupportThreadResponseDTO> listAllThreads() {
        return supportRepository.findAllThreads().map(this::mapThread);
    }

    @Override
    @Transactional
    public Mono<SupportMessageResponseDTO> replyAsAdmin(UUID threadId, SupportReplyRequest request) {
        LocalDateTime now = LocalDateTime.now();
        return supportRepository.findThreadById(Objects.requireNonNull(threadId))
                .switchIfEmpty(Mono.error(new ValidationException("Support thread not found")))
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
                            .then(supportRepository.saveMessage(message))
                            .then(supportNotificationPort.notifyVisitorReplyAvailable(
                                    thread.getVisitorEmail(),
                                    easyRentalProperties.getSupport().getHelpUrl()))
                            .thenReturn(mapMessage(message));
                });
    }

    private SupportThreadResponseDTO mapThread(SupportThreadEntity entity) {
        return new SupportThreadResponseDTO(
                entity.getId(),
                entity.getVisitorEmail(),
                entity.getVisitorName(),
                entity.getSubject(),
                entity.getStatus(),
                entity.getAdminUnreadCount() != null ? entity.getAdminUnreadCount() : 0,
                entity.getLastMessageAt(),
                entity.getCreatedAt());
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
