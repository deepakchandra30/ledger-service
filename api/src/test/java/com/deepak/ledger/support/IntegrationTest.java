package com.deepak.ledger.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.lang.annotation.*;

/**
 * Real Postgres and real Kafka. Containers are reused across the suite, which
 * keeps the integration run under a minute on CI.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public @interface IntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    class Containers {

        @Bean
        @ServiceConnection
        PostgreSQLContainer<?> postgres() {
            return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withReuse(true);
        }

        @Bean
        @ServiceConnection
        ConfluentKafkaContainer kafka() {
            return new ConfluentKafkaContainer(
                    DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).withReuse(true);
        }
    }
}
