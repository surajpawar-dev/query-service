package com.suraj.rag.query.config;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.embedding-service")
public record EmbeddingServiceProperties(
        @NotBlank String baseUrl, int defaultTopK, Duration requestTimeout) {}
