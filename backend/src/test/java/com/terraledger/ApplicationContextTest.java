package com.terraledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * M0 smoke test: the Spring context loads without errors.
 * Uses test profile so no live DB / chain is required.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationContextTest {

    @Test
    void contextLoads() {
        // passes if the Spring context starts without throwing
    }
}
