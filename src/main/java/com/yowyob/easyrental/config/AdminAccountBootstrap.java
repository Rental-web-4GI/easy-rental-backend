package com.yowyob.easyrental.config;

import com.yowyob.easyrental.modules.auth.domain.UserEntity;
import com.yowyob.easyrental.modules.auth.infrastructure.adapter.out.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Ensures a single platform ADMIN account exists for local console access.
 *
 * @author Easy Rental Team
 * @since 2026-07-07
 */
@Component
@Profile("local")
@ConditionalOnProperty(name = "easy-rental.admin.bootstrap-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class AdminAccountBootstrap implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EasyRentalProperties easyRentalProperties;

    @Override
    public void run(String... args) {
        try {
            bootstrapAdmin().block();
            log.info("Platform ADMIN account ready ({})", resolveEmail());
        } catch (Exception error) {
            log.error("Failed to bootstrap platform ADMIN account", error);
        }
    }

    private Mono<Void> bootstrapAdmin() {
        String email = resolveEmail();
        String encodedPassword = passwordEncoder.encode(easyRentalProperties.getAdmin().getPassword());

        return userRepository.findByEmail(email)
                .flatMap(existing -> {
                    existing.setRole("ADMIN");
                    existing.setPassword(encodedPassword);
                    existing.setFirstname(easyRentalProperties.getAdmin().getFirstname());
                    existing.setLastname(easyRentalProperties.getAdmin().getLastname());
                    existing.setFullname(
                            easyRentalProperties.getAdmin().getFirstname() + " "
                                    + easyRentalProperties.getAdmin().getLastname());
                    existing.setStatus("ACTIVE");
                    existing.setOrganizationId(null);
                    existing.setAgencyId(null);
                    existing.setPosteId(null);
                    return userRepository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity admin = UserEntity.builder()
                            .id(UUID.randomUUID())
                            .email(email)
                            .password(encodedPassword)
                            .role("ADMIN")
                            .firstname(easyRentalProperties.getAdmin().getFirstname())
                            .lastname(easyRentalProperties.getAdmin().getLastname())
                            .fullname(easyRentalProperties.getAdmin().getFirstname() + " "
                                    + easyRentalProperties.getAdmin().getLastname())
                            .status("ACTIVE")
                            .isNewRecord(true)
                            .build();
                    return userRepository.save(admin);
                }))
                .then();
    }

    private String resolveEmail() {
        String configured = easyRentalProperties.getAdmin().getEmail();
        if (configured != null && !configured.isBlank()) {
            return configured.trim().toLowerCase();
        }
        return easyRentalProperties.getSupport().getAdminEmail().trim().toLowerCase();
    }
}
