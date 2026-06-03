package com.yowyob.easyrental.modules.media.application;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.domain.port.out.UserRepositoryPort;
import com.yowyob.easyrental.modules.media.domain.MediaEntity;
import com.yowyob.easyrental.modules.media.domain.port.out.MediaRepositoryPort;
import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaUseCaseImplTest {

    @Mock private MediaRepositoryPort mediaRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @InjectMocks private MediaUseCaseImpl mediaUseCase;

    @Test
    void shouldInitUploadDirectory() throws Exception {
        Path tempDir = Files.createTempDirectory("upload-test");
        ReflectionTestUtils.setField(mediaUseCase, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(mediaUseCase, "baseUrl", "http://localhost:8080");

        mediaUseCase.init();

        StepVerifier.create(Files.exists(tempDir) ? Mono.just(true) : Mono.empty())
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldUploadFileForClient() throws Exception {
        Path tempDir = Files.createTempDirectory("upload-client");
        ReflectionTestUtils.setField(mediaUseCase, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(mediaUseCase, "baseUrl", "http://localhost:8080");

        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("client@test.com")
                .role("CLIENT").lastname("Doe").build();
        FilePart filePart = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);

        when(filePart.filename()).thenReturn("photo.jpg");
        when(filePart.headers()).thenReturn(headers);
        when(filePart.transferTo(any(Path.class))).thenReturn(Mono.empty());
        when(userRepository.findByEmail("client@test.com")).thenReturn(Mono.just(user));
        when(mediaRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var auth = new UsernamePasswordAuthenticationToken("client@test.com", null);

        StepVerifier.create(mediaUseCase.uploadFile(filePart)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNextMatches(MediaEntity::isNew)
                .verifyComplete();
    }

    @Test
    void shouldUploadFileForOrganization() throws Exception {
        Path tempDir = Files.createTempDirectory("upload-org");
        ReflectionTestUtils.setField(mediaUseCase, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(mediaUseCase, "baseUrl", "http://localhost:8080");

        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("org@test.com").role("ORGANIZATION").build();
        OrganizationEntity org = OrganizationEntity.builder().id(UUID.randomUUID()).name("My Org").build();
        FilePart filePart = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();

        when(filePart.filename()).thenReturn("logo.png");
        when(filePart.headers()).thenReturn(headers);
        when(filePart.transferTo(any(Path.class))).thenReturn(Mono.empty());
        when(userRepository.findByEmail("org@test.com")).thenReturn(Mono.just(user));
        when(organizationRepository.findByOwnerId(userId)).thenReturn(Mono.just(org));
        when(mediaRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var auth = new UsernamePasswordAuthenticationToken("org@test.com", null);

        StepVerifier.create(mediaUseCase.uploadFile(filePart)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNextMatches(m -> m.getFileUrl().contains("/uploads/"))
                .verifyComplete();
    }
}
