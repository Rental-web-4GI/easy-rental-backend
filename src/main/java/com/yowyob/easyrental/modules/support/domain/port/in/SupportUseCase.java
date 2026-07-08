package com.yowyob.easyrental.modules.support.domain.port.in;

import com.yowyob.easyrental.modules.support.dto.SupportConfigResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportConversationResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportMessageRequest;
import com.yowyob.easyrental.modules.support.dto.SupportMessageResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportReplyRequest;
import com.yowyob.easyrental.modules.support.dto.SupportSendResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupportUseCase {

    Mono<SupportConfigResponseDTO> getPublicConfig();

    Mono<SupportSendResponseDTO> sendUserMessage(SupportMessageRequest request);

    Flux<SupportMessageResponseDTO> getConversationMessages(String email, String visitorSessionId);

    Flux<SupportConversationResponseDTO> listConversationsForAdmin();

    Flux<SupportMessageResponseDTO> getConversationMessagesForAdmin(String email, String visitorSessionId);

    Mono<SupportMessageResponseDTO> replyAsAdmin(String email, String visitorSessionId, SupportReplyRequest request);

    Mono<Void> markConversationAsRead(String email, String visitorSessionId);
}
