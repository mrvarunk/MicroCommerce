package com.microservices.stockservice.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration for integration tests.
 *
 * This configuration provides flexible setup for testing:
 * - Local testing: Uses Spring Boot Test's RandomPort (default)
 * - Kubernetes testing: Can be overridden with a base URL property
 *
 * To run tests against a Kubernetes deployment:
 * Set the property: -Dtest.base-url=http://localhost:30081
 *
 * For local testing (default):
 * Simply run: mvn clean test
 */
@TestConfiguration
public class StockServiceTestConfiguration {

    /**
     * Provides a TestRestTemplate bean configured with the appropriate base URL.
     * For local testing with @SpringBootTest(webEnvironment = RANDOM_PORT),
     * the TestRestTemplate is automatically configured by Spring Boot.
     *
     * This bean can be customized if needed for different environments.
     */
    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder();
    }
}
