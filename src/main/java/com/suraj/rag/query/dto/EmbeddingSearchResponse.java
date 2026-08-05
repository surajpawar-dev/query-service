package com.suraj.rag.query.dto;

import java.util.List;

public record EmbeddingSearchResponse(List<SourceChunk> matches) {}
