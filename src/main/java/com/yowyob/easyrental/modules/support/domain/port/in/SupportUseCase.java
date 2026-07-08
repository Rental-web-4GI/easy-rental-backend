package com.yowyob.easyrental.modules.support.domain.port.in;

import com.yowyob.easyrental.modules.support.dto.SupportConfigResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportMessageRequest;
import com.yowyob.easyrental.modules.support.dto.SupportMessageResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportReplyRequest;
import com.yowyob.easyrental.modules.support.dto.SupportSendResponseDTO;
import com.yowyob.easyrental.modules.support.dto.SupportThreadResponseDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SupportUseCase {
    Mono<SupportConfigResponseDTO> getPublicConfig();
    Mono<SupportSendResponseDTO> sendUserMessage(SupportMessageRequest request);
    Flux<SupportMessageResponseDTO> getThreadMessages(UUID threadId);
    Flux<SupportThreadResponseDTO> listAllThreads();
    Mono<SupportMessageResponseDTO> replyAsAdmin(UUID threadId, SupportReplyRequest request);
}
