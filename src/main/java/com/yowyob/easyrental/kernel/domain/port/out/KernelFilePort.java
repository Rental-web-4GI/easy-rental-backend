package com.yowyob.easyrental.kernel.domain.port.out;

import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

/**
 * Outbound port for kernel file-core media storage.
 * Local default remains disk uploads/ until file-core credentials and API are wired.
 *
 * @author Easy Rental Team
 * @since 2026-07-08
 */
public interface KernelFilePort {

    /**
     * Upload a file and return a publicly reachable URL.
     */
    Mono<String> upload(FilePart filePart, String objectKey, KernelRequestContext context);
}
