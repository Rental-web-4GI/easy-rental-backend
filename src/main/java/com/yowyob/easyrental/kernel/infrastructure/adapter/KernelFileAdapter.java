package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelFilePort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Placeholder file-core adapter. Activated when {@code kernel.file-core.enabled=true}.
 * Until then MediaUseCaseImpl keeps using local uploads/.
 *
 * @author Easy Rental Team
 * @since 2026-07-08
 */
@Component
@ConditionalOnProperty(prefix = "kernel.file-core", name = "enabled", havingValue = "true")
public class KernelFileAdapter implements KernelFilePort {

    @Override
    public Mono<String> upload(FilePart filePart, String objectKey, KernelRequestContext context) {
        return Mono.error(new UnsupportedOperationException(
                "file-core upload is not configured yet. Set kernel.file-core.enabled=false "
                        + "or implement KernelFileAdapter against the kernel file API."));
    }
}
