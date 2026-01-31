package com.genai.knowitall.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for async task execution.
 *
 * Enables Spring's @Async annotation for background processing.
 * Thread pool configuration is defined in application.properties:
 * - spring.task.execution.pool.core-size
 * - spring.task.execution.pool.max-size
 * - spring.task.execution.pool.queue-capacity
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Spring Boot automatically configures ThreadPoolTaskExecutor
    // using properties from application.properties
}
