package com.yowyob.easyrental.kernel.infrastructure.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.domain.port.out.KernelHttpPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KernelTpAdapterTest {

    @Mock
    private KernelHttpPort kernelHttpPort;

    private KernelTpAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new KernelTpAdapter(kernelHttpPort);
    }

    @Test
    void createClient_callsKernelHttpPortWithCorrectPath() throws Exception {
        JsonNode fakeResponse = new ObjectMapper().readTree("{\"id\":\"abc-123\",\"email\":\"client@test.com\"}");
        when(kernelHttpPort.post(eq("/api/clients"), anyMap(), any(KernelRequestContext.class)))
                .thenReturn(Mono.just(fakeResponse));

        Map<String, Object> payload = Map.of("email", "client@test.com", "thirdPartyType", "CLIENT");

        StepVerifier.create(adapter.createClient(payload, KernelRequestContext.empty()))
                .expectNextMatches(node -> "abc-123".equals(node.path("id").asText()))
                .verifyComplete();

        verify(kernelHttpPort).post(eq("/api/clients"), eq(payload), any(KernelRequestContext.class));
    }
}
