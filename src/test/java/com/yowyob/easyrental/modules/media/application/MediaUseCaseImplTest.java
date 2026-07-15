package com.yowyob.easyrental.modules.media.application;

import com.yowyob.easyrental.kernel.config.KernelClientProperties;
import com.yowyob.easyrental.kernel.domain.KernelAuthClaims;
import com.yowyob.easyrental.kernel.domain.KernelRequestContext;
import com.yowyob.easyrental.kernel.infrastructure.KernelContextHolder;
import com.yowyob.easyrental.kernel.infrastructure.adapter.KernelFileAdapter;
import com.yowyob.easyrental.kernel.security.KernelAuthenticationToken;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaUseCaseImplTest {

    @Mock private MediaRepositoryPort mediaRepository;
    @Mock private UserRepositoryPort userRepository;
    @Mock private OrganizationRepositoryPort organizationRepository;
    @Mock private KernelFileAdapter kernelFileAdapter;
    @Mock private KernelClientProperties kernelProperties;
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

    @Test
    void shouldUploadFileForKernelOrganizationUser() throws Exception {
        Path tempDir = Files.createTempDirectory("upload-kernel-org");
        ReflectionTestUtils.setField(mediaUseCase, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(mediaUseCase, "baseUrl", "http://localhost:8080");

        UUID userId = UUID.randomUUID();
        UUID kernelUserId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("org@test.com").role("ORGANIZATION").build();
        OrganizationEntity org = OrganizationEntity.builder().id(UUID.randomUUID()).name("Sahel org").build();
        FilePart filePart = mock(FilePart.class);
        HttpHeaders headers = new HttpHeaders();

        when(filePart.filename()).thenReturn("vehicle.png");
        when(filePart.headers()).thenReturn(headers);
        when(filePart.transferTo(any(Path.class))).thenReturn(Mono.empty());
        when(userRepository.findByKernelUserId(kernelUserId)).thenReturn(Mono.just(user));
        when(organizationRepository.findByOwnerId(userId)).thenReturn(Mono.just(org));
        when(mediaRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        KernelAuthClaims claims = new KernelAuthClaims(
                kernelUserId.toString(),
                kernelUserId.toString(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of("ROLE_ORGANIZATION_ADMIN"),
                List.of());
        var auth = new KernelAuthenticationToken(claims, "kernel-token");

        StepVerifier.create(mediaUseCase.uploadFile(filePart)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNextMatches(m -> m.getFileUrl().contains("/uploads/"))
                .verifyComplete();
    }

    @Test
    void uploadFile_whenKernelEnabled_uploadsToKernel() throws Exception {
        Path tempDir = Files.createTempDirectory("upload-kernel-enabled");
        ReflectionTestUtils.setField(mediaUseCase, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(mediaUseCase, "baseUrl", "http://localhost:8080");

        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("client@test.com")
                .role("CLIENT").lastname("Dupont").build();

        FilePart filePart = mock(FilePart.class);
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_JPEG);
        when(filePart.filename()).thenReturn("photo.jpg");
        when(filePart.headers()).thenReturn(partHeaders);

        KernelFileAdapter.KernelFileResult kernelResult =
                new KernelFileAdapter.KernelFileResult(
                        "file-uuid-123",
                        "https://kernel-core.yowyob.com/api/files/file-uuid-123/content",
                        "photo.jpg");

        when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
        when(kernelFileAdapter.upload(eq(filePart), any(KernelRequestContext.class)))
                .thenReturn(Mono.just(kernelResult));
        when(userRepository.findByEmail("client@test.com")).thenReturn(Mono.just(user));
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var auth = new UsernamePasswordAuthenticationToken("client@test.com", null);

        KernelRequestContext kernelCtx = KernelRequestContext.builder()
                .bearerToken(Optional.of("test-token"))
                .organizationId(Optional.empty())
                .agencyId(Optional.empty())
                .build();

        MediaEntity result = mediaUseCase.uploadFile(filePart)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                .contextWrite(ctx -> KernelContextHolder.withContext(ctx, kernelCtx))
                .block();

        assertNotNull(result);
        assertEquals("https://kernel-core.yowyob.com/api/files/file-uuid-123/content", result.getFileUrl());
    }

    @Test
    void uploadFile_whenKernelFails_fallsBackToLocal() throws Exception {
        Path tempDir = Files.createTempDirectory("upload-kernel-fallback");
        ReflectionTestUtils.setField(mediaUseCase, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(mediaUseCase, "baseUrl", "http://localhost:8080");

        UUID userId = UUID.randomUUID();
        UserEntity user = UserEntity.builder().id(userId).email("client@test.com")
                .role("CLIENT").lastname("Doe").build();

        FilePart filePart = mock(FilePart.class);
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.IMAGE_JPEG);
        when(filePart.filename()).thenReturn("doc.jpg");
        when(filePart.headers()).thenReturn(partHeaders);
        when(filePart.transferTo(any(Path.class))).thenReturn(Mono.empty());

        when(kernelProperties.isIntegrationEnabled()).thenReturn(true);
        when(kernelFileAdapter.upload(any(), any())).thenReturn(Mono.error(new RuntimeException("Timeout")));
        when(userRepository.findByEmail("client@test.com")).thenReturn(Mono.just(user));
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var auth = new UsernamePasswordAuthenticationToken("client@test.com", null);

        KernelRequestContext kernelCtx = KernelRequestContext.empty();

        StepVerifier.create(mediaUseCase.uploadFile(filePart)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                        .contextWrite(ctx -> KernelContextHolder.withContext(ctx, kernelCtx)))
                .expectNextCount(1)
                .verifyComplete();
    }
}
