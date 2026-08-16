package com.suraj.rag.query.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.suraj.rag.query.config.LlmProperties;
import com.suraj.rag.query.dto.QueryHistoryMessage;
import com.suraj.rag.query.dto.QueryMode;
import com.suraj.rag.query.dto.QueryRequest;
import com.suraj.rag.query.dto.SourceChunk;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

    @Test
    void buildsPromptWithQuestionAndSources() {
        PromptBuilder builder =
                new PromptBuilder(
                        new LlmProperties(
                                "ollama",
                                "http://localhost:11434",
                                "llama3.1",
                                Duration.ofSeconds(30),
                                0.2,
                                5000));
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        String prompt =
                builder.build(
                        new QueryRequest(
                                "What is the refund rule?",
                                QueryMode.ALL_DOCUMENTS,
                                5,
                                null,
                                true,
                                List.of(
                                        new QueryHistoryMessage(
                                                "user", "We were discussing the policy PDF."))),
                        List.of(
                                new SourceChunk(
                                        documentId,
                                        chunkId,
                                        1,
                                        "Refunds are allowed within 30 days.",
                                        0.92,
                                        Map.of("page", 3))));

        assertThat(prompt).contains("What is the refund rule?");
        assertThat(prompt).contains("We were discussing the policy PDF.");
        assertThat(prompt).contains("Refunds are allowed within 30 days.");
        assertThat(prompt).contains(documentId.toString());
        assertThat(prompt).contains(chunkId.toString());
    }
}
