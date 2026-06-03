package com.yowyob.easyrental.modules.media.domain.port.in;

import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/**
 * Incoming port for media use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface MediaUseCase {
    void init();
    Mono<MediaEntity> uploadFile(FilePart filePart);
}
