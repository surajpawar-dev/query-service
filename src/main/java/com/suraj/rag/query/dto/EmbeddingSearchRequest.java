package com.suraj.rag.query.dto;

import java.util.List;
import java.util.UUID;

public record EmbeddingSearchRequest(String query, Integer topK, List<UUID> documentIds) {}
