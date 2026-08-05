package com.suraj.rag.query.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.query")
public record QueryProperties(int maxQuestionLength, boolean includeSourcesByDefault) {}
