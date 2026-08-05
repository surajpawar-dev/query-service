package com.suraj.rag.query.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        @NotBlank String provider,
        @NotBlank String baseUrl,
        @NotBlank String model,
        Duration requestTimeout,
        double temperature,
        int maxContextCharacters) {}
