package com.yowyob.easyrental.kernel.application;

import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelOrganizationAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KernelOrganizationBootstrapServiceTest {

    @Mock private KernelOrganizationAdapter kernelOrganizationAdapter;
    @InjectMocks private KernelOrganizationBootstrapService bootstrapService;

    @Test
    void shouldSubscribeAllDefaultServices() {
        UUID orgId = UUID.randomUUID();
        KernelRequestContext ctx = KernelRequestContext.builder().build();
        when(kernelOrganizationAdapter.subscribeService(eq(orgId), any(), eq(ctx)))
                .thenReturn(Mono.just(JsonNodeFactory.instance.objectNode()));

        StepVerifier.create(bootstrapService.subscribeDefaultServices(orgId, ctx))
                .verifyComplete();

        verify(kernelOrganizationAdapter, times(5)).subscribeService(eq(orgId), any(), eq(ctx));
    }
}
