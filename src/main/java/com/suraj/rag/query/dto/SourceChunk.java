package com.suraj.rag.query.dto;

import java.util.Map;
import java.util.UUID;

public record SourceChunk(
        UUID documentId,
        UUID chunkId,
        Integer chunkOrder,
        String content,
        double score,
        Map<String, Object> metadata) {}
