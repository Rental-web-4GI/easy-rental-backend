package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KernelResourceAdapterTest {

    @Mock private KernelHttpPort kernelHttpPort;
    @InjectMocks private KernelResourceAdapter kernelResourceAdapter;

    @Test
    void shouldPostVehicleResourceToKernel() {
        UUID orgId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        KernelRequestContext ctx = KernelRequestContext.builder().build();
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", "LT-88");
        payload.put("resourceType", "VEHICLE");

        when(kernelHttpPort.post(eq("/api/resources"), any(), any()))
                .thenReturn(Mono.just(JsonNodeFactory.instance.objectNode().put("id", UUID.randomUUID().toString())));

        StepVerifier.create(kernelResourceAdapter.createResource(orgId, agencyId, payload, ctx))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        verify(kernelHttpPort).post(eq("/api/resources"), captor.capture(), any());
        assertEquals(orgId.toString(), captor.getValue().get("organizationId"));
        assertEquals(agencyId.toString(), captor.getValue().get("agencyId"));
    }
}
