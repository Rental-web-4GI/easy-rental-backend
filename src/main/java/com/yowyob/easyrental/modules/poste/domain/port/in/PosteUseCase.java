package com.yowyob.easyrental.modules.poste.domain.port.in;

import com.yowyob.easyrental.modules.poste.dto.PosteRequestDTO;
import com.yowyob.easyrental.modules.poste.dto.PosteResponseDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for poste use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface PosteUseCase {
    Mono<PosteResponseDTO> createPoste(UUID orgId, PosteRequestDTO request);
    Flux<PosteResponseDTO> getAvailablePostes(UUID orgId);
    Mono<PosteResponseDTO> updatePoste(UUID posteId, PosteRequestDTO request);
    Mono<PosteResponseDTO> getPosteById(UUID id);
}
