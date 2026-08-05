package com.suraj.rag.query.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record QueryRequest(
        @NotBlank @Size(max = 2000) String question,
        @Min(1) @Max(50) Integer topK,
        List<UUID> documentIds,
        Boolean includeSources) {}
