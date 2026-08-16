package com.suraj.rag.query.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suraj.rag.query.client.embedding.EmbeddingSearchClient;
import com.suraj.rag.query.client.llm.LlmClient;
import com.suraj.rag.query.config.EmbeddingServiceProperties;
import com.suraj.rag.query.config.LlmProperties;
import com.suraj.rag.query.config.QueryProperties;
import com.suraj.rag.query.dto.EmbeddingSearchRequest;
import com.suraj.rag.query.dto.EmbeddingSearchResponse;
import com.suraj.rag.query.dto.QueryHistoryMessage;
import com.suraj.rag.query.dto.QueryMode;
import com.suraj.rag.query.dto.QueryRequest;
import com.suraj.rag.query.dto.QueryStreamEvent;
import com.suraj.rag.query.dto.SourceChunk;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class QueryOrchestrationServiceTest {

    private EmbeddingSearchClient embeddingSearchClient;
    private LlmClient llmClient;
    private QueryOrchestrationService service;

    @BeforeEach
    void setUp() {
        embeddingSearchClient = org.mockito.Mockito.mock(EmbeddingSearchClient.class);
        llmClient = org.mockito.Mockito.mock(LlmClient.class);
        PromptBuilder promptBuilder =
                new PromptBuilder(
                        new LlmProperties(
                                "ollama",
                                "http://localhost:11434",
                                "llama3.1",
                                Duration.ofSeconds(30),
                                0.2,
                                5000));
        service =
                new QueryOrchestrationService(
                        embeddingSearchClient,
                        llmClient,
                        promptBuilder,
                        new EmbeddingServiceProperties(
                                "http://localhost:8082", 5, Duration.ofSeconds(30)),
                        new QueryProperties(2000, true));
    }

    @Test
    void generalModeCallsLlmDirectlyWithoutEmbeddingSearch() {
        List<QueryHistoryMessage> history =
                List.of(new QueryHistoryMessage("user", "My name is Suraj."));
        when(llmClient.streamDirectAnswer("What is my name?", history))
                .thenReturn(Flux.just("Paris"));

        Flux<QueryStreamEvent> stream =
                service.stream(
                        new QueryRequest(
                                "What is my name?", QueryMode.GENERAL, null, null, false, history));

        StepVerifier.create(stream)
                .expectNext(QueryStreamEvent.token("Paris"))
                .expectNext(QueryStreamEvent.done())
                .verifyComplete();
        verify(embeddingSearchClient, never()).search(any());
        verify(llmClient).streamDirectAnswer("What is my name?", history);
    }

    @Test
    void documentModeSearchesEmbeddingsBeforeGroundedLlmCall() {
        UUID documentId = UUID.randomUUID();
        SourceChunk source =
                new SourceChunk(
                        documentId,
                        UUID.randomUUID(),
                        1,
                        "Refunds are allowed within 30 days.",
                        0.9,
                        Map.of());
        when(embeddingSearchClient.search(any()))
                .thenReturn(Mono.just(new EmbeddingSearchResponse(List.of(source))));
        when(llmClient.streamGroundedAnswer(any())).thenReturn(Flux.just("Refunds are allowed."));

        Flux<QueryStreamEvent> stream =
                service.stream(
                        new QueryRequest(
                                "What is the refund rule?",
                                QueryMode.SPECIFIC,
                                8,
                                List.of(documentId),
                                true,
                                List.of(
                                        new QueryHistoryMessage(
                                                "user", "We were discussing store policy."))));

        StepVerifier.create(stream)
                .expectNext(QueryStreamEvent.token("Refunds are allowed."))
                .expectNext(QueryStreamEvent.sources(List.of(source)))
                .expectNext(QueryStreamEvent.done())
                .verifyComplete();
        ArgumentCaptor<EmbeddingSearchRequest> requestCaptor =
                ArgumentCaptor.forClass(EmbeddingSearchRequest.class);
        verify(embeddingSearchClient).search(requestCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().documentIds())
                .containsExactly(documentId);
        verify(llmClient).streamGroundedAnswer(any());
    }
}
