package com.yowyob.easyrental.modules.audit.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yowyob.easyrental.modules.audit.domain.AuditEventEntity;
import com.yowyob.easyrental.modules.audit.domain.port.out.AuditRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuditUseCaseImplTest {

    @Mock
    private AuditRepositoryPort repo;

    @InjectMocks
    private AuditUseCaseImpl useCase;

    @Test
    void record_persistsEvent() {
        UUID userId = UUID.randomUUID();
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.record(userId, "LOGIN_SUCCESS", null, null, "127.0.0.1", "test-ua", null))
                .verifyComplete();

        ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
        verify(repo).save(captor.capture());
        AuditEventEntity saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals("LOGIN_SUCCESS", saved.getAction());
        assertEquals("127.0.0.1", saved.getIp());
    }

    @Test
    void record_swallowsErrorsSilently() {
        when(repo.save(any())).thenReturn(Mono.error(new RuntimeException("db down")));

        // Ne doit PAS propager l'erreur -- le flux métier ne doit pas être interrompu.
        StepVerifier.create(useCase.record(null, "LOGIN_FAILED", null, null, null, null, null))
                .verifyComplete();
    }
}
