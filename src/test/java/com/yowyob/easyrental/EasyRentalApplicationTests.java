package com.yowyob.easyrental;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Full context smoke test — requires PostgreSQL (and optional Redis/Kafka/ES).
 * Run manually with: {@code ./mvnw test -Dtest=EasyRentalApplicationTests -Dspring.profiles.active=local}
 */
@SpringBootTest
@ActiveProfiles("test")
@Disabled("Full Spring context needs R2DBC PostgreSQL; unit tests cover business logic")
class EasyRentalApplicationTests {

    @Test
    void contextLoads() {
    }
}
