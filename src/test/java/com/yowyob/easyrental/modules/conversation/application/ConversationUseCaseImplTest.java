package com.yowyob.easyrental.modules.conversation.application;

import com.yowyob.easyrental.modules.conversation.domain.ConversationEntity;
import com.yowyob.easyrental.modules.conversation.domain.ConversationMessageEntity;
import com.yowyob.easyrental.modules.conversation.domain.ConversationType;
import com.yowyob.easyrental.modules.conversation.domain.ParticipantType;
import com.yowyob.easyrental.modules.conversation.domain.port.out.ConversationRepositoryPort;
import com.yowyob.easyrental.modules.conversation.dto.MessageDTO;
import com.yowyob.easyrental.modules.conversation.mapper.ConversationMapper;
import com.yowyob.easyrental.modules.notification.domain.port.in.NotificationUseCase;
import com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationUseCaseImplTest {

    @Mock
    private ConversationRepositoryPort repo;

    @Mock
    private ConversationMapper mapper;

    @Mock
    private NotificationUseCase notificationUseCase;

    @InjectMocks
    private ConversationUseCaseImpl useCase;

    @Test
    void openOrGet_existing_returnsSame() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        ConversationEntity existing = ConversationEntity.builder()
                .id(UUID.randomUUID())
                .type(ConversationType.CLIENT_AGENCY)
                .participantAType(ParticipantType.CLIENT)
                .participantAId(aId)
                .participantBType(ParticipantType.AGENCY)
                .participantBId(bId)
                .aUnread(0)
                .bUnread(0)
                .createdAt(Instant.now())
                .build();

        when(repo.findBetween("CLIENT", aId, "AGENCY", bId)).thenReturn(Mono.just(existing));

        StepVerifier.create(useCase.openOrGet(ParticipantType.CLIENT, aId, ParticipantType.AGENCY, bId))
                .expectNext(existing)
                .verifyComplete();

        verify(repo, never()).save(any());
    }

    @Test
    void openOrGet_new_createsDeducedType() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        when(repo.findBetween("CLIENT", aId, "AGENCY", bId)).thenReturn(Mono.empty());
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.openOrGet(ParticipantType.CLIENT, aId, ParticipantType.AGENCY, bId))
                .expectNextMatches(c -> c.getType() == ConversationType.CLIENT_AGENCY
                        && c.getParticipantAType() == ParticipantType.CLIENT)
                .verifyComplete();

        ArgumentCaptor<ConversationEntity> captor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(repo).save(captor.capture());
        ConversationEntity saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(ConversationType.CLIENT_AGENCY);
        assertThat(saved.getParticipantAType()).isEqualTo(ParticipantType.CLIENT);
        assertThat(saved.getParticipantAId()).isEqualTo(aId);
        assertThat(saved.getParticipantBType()).isEqualTo(ParticipantType.AGENCY);
        assertThat(saved.getParticipantBId()).isEqualTo(bId);
    }

    @Test
    void sendMessage_incrementsRecipientUnread() {
        UUID convId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        ConversationEntity conv = ConversationEntity.builder()
                .id(convId)
                .type(ConversationType.CLIENT_AGENCY)
                .participantAType(ParticipantType.CLIENT)
                .participantAId(clientId)
                .participantBType(ParticipantType.AGENCY)
                .participantBId(agencyId)
                .aUnread(0)
                .bUnread(0)
                .build();

        when(repo.findById(convId)).thenReturn(Mono.just(conv));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(repo.saveMessage(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(mapper.toMessageDto(any())).thenReturn(
                new MessageDTO(UUID.randomUUID(), convId, "CLIENT", clientId, "hi", Instant.now()));
        when(notificationUseCase.createNotification(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Mono.just(mock(NotificationResponseDTO.class)));

        StepVerifier.create(useCase.sendMessage(convId, ParticipantType.CLIENT, clientId, "hi"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<ConversationEntity> captor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(repo).save(captor.capture());
        ConversationEntity saved = captor.getValue();
        assertThat(saved.getBUnread()).isEqualTo(1);
        assertThat(saved.getAUnread()).isEqualTo(0);
        assertThat(saved.getLastMessageAt()).isNotNull();

        ArgumentCaptor<ConversationMessageEntity> msgCaptor = ArgumentCaptor.forClass(ConversationMessageEntity.class);
        verify(repo).saveMessage(msgCaptor.capture());
        assertThat(msgCaptor.getValue().getBody()).isEqualTo("hi");
    }

    @Test
    void sendMessage_rejectsNonParticipant() {
        UUID convId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();

        ConversationEntity conv = ConversationEntity.builder()
                .id(convId)
                .type(ConversationType.CLIENT_AGENCY)
                .participantAType(ParticipantType.CLIENT)
                .participantAId(clientId)
                .participantBType(ParticipantType.AGENCY)
                .participantBId(agencyId)
                .aUnread(0)
                .bUnread(0)
                .build();

        when(repo.findById(convId)).thenReturn(Mono.just(conv));

        StepVerifier.create(useCase.sendMessage(convId, ParticipantType.CLIENT, intruderId, "intrusion"))
                .expectErrorMatches(e -> e.getMessage().contains("NOT_A_PARTICIPANT"))
                .verify();

        verify(repo, never()).save(any());
        verify(repo, never()).saveMessage(any());
    }

    @Test
    void getMessages_rejectsNonParticipantButAllowsAdmin() {
        UUID convId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        UUID intruderId = UUID.randomUUID();

        ConversationEntity conv = ConversationEntity.builder()
                .id(convId)
                .type(ConversationType.CLIENT_AGENCY)
                .participantAType(ParticipantType.CLIENT)
                .participantAId(clientId)
                .participantBType(ParticipantType.AGENCY)
                .participantBId(agencyId)
                .build();

        when(repo.findById(convId)).thenReturn(Mono.just(conv));
        when(mapper.toMessageDto(any())).thenReturn(
                new MessageDTO(UUID.randomUUID(), convId, "CLIENT", clientId, "hi", Instant.now()));
        when(repo.findMessages(convId, 0, 50)).thenReturn(reactor.core.publisher.Flux.just(
                mock(ConversationMessageEntity.class)));

        // Un client tiers ne peut pas lire la conversation.
        StepVerifier.create(useCase.getMessages(convId, ParticipantType.CLIENT, intruderId, 0, 50))
                .expectErrorMatches(e -> e.getMessage().contains("NOT_A_PARTICIPANT"))
                .verify();

        // Un ADMIN (supervision) peut lire n'importe quelle conversation.
        StepVerifier.create(useCase.getMessages(convId, ParticipantType.ADMIN, null, 0, 50))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void markRead_rejectsNonParticipant() {
        UUID convId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        ConversationEntity conv = ConversationEntity.builder()
                .id(convId)
                .type(ConversationType.CLIENT_AGENCY)
                .participantAType(ParticipantType.CLIENT)
                .participantAId(clientId)
                .participantBType(ParticipantType.AGENCY)
                .participantBId(agencyId)
                .aUnread(3)
                .bUnread(2)
                .build();

        when(repo.findById(convId)).thenReturn(Mono.just(conv));

        StepVerifier.create(useCase.markRead(convId, ParticipantType.CLIENT, UUID.randomUUID()))
                .expectErrorMatches(e -> e.getMessage().contains("NOT_A_PARTICIPANT"))
                .verify();

        verify(repo, never()).save(any());
    }

    @Test
    void markRead_resetsReaderUnread() {
        UUID convId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        ConversationEntity conv = ConversationEntity.builder()
                .id(convId)
                .type(ConversationType.CLIENT_AGENCY)
                .participantAType(ParticipantType.CLIENT)
                .participantAId(clientId)
                .participantBType(ParticipantType.AGENCY)
                .participantBId(agencyId)
                .aUnread(3)
                .bUnread(2)
                .build();

        when(repo.findById(convId)).thenReturn(Mono.just(conv));
        when(repo.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.markRead(convId, ParticipantType.AGENCY, agencyId))
                .verifyComplete();

        ArgumentCaptor<ConversationEntity> captor = ArgumentCaptor.forClass(ConversationEntity.class);
        verify(repo).save(captor.capture());
        ConversationEntity saved = captor.getValue();
        assertThat(saved.getBUnread()).isEqualTo(0);
        assertThat(saved.getAUnread()).isEqualTo(3);
    }
}
