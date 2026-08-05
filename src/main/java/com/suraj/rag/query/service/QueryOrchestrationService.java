package com.suraj.rag.query.service;

import com.suraj.rag.query.client.embedding.EmbeddingSearchClient;
import com.suraj.rag.query.client.llm.LlmClient;
import com.suraj.rag.query.config.EmbeddingServiceProperties;
import com.suraj.rag.query.config.QueryProperties;
import com.suraj.rag.query.dto.EmbeddingSearchRequest;
import com.suraj.rag.query.dto.QueryRequest;
import com.suraj.rag.query.dto.QueryStreamEvent;
import com.suraj.rag.query.dto.SourceChunk;
import com.suraj.rag.query.exception.ErrorCode;
import com.suraj.rag.query.exception.QueryException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class QueryOrchestrationService {

    private final EmbeddingSearchClient embeddingSearchClient;
    private final LlmClient llmClient;
    private final PromptBuilder promptBuilder;
    private final EmbeddingServiceProperties embeddingServiceProperties;
    private final QueryProperties queryProperties;

    public QueryOrchestrationService(
            EmbeddingSearchClient embeddingSearchClient,
            LlmClient llmClient,
            PromptBuilder promptBuilder,
            EmbeddingServiceProperties embeddingServiceProperties,
            QueryProperties queryProperties) {
        this.embeddingSearchClient = embeddingSearchClient;
        this.llmClient = llmClient;
        this.promptBuilder = promptBuilder;
        this.embeddingServiceProperties = embeddingServiceProperties;
        this.queryProperties = queryProperties;
    }

    public Flux<QueryStreamEvent> stream(QueryRequest request) {
        validateQuestionLength(request);
        int topK =
                request.topK() == null ? embeddingServiceProperties.defaultTopK() : request.topK();
        boolean includeSources =
                request.includeSources() == null
                        ? queryProperties.includeSourcesByDefault()
                        : request.includeSources();

        return embeddingSearchClient
                .search(new EmbeddingSearchRequest(request.question(), topK, request.documentIds()))
                .flatMapMany(
                        response -> {
                            List<SourceChunk> sources =
                                    response.matches() == null ? List.of() : response.matches();
                            if (sources.isEmpty()) {
                                return Flux.error(
                                        new QueryException(
                                                ErrorCode.NO_CONTEXT_FOUND,
                                                HttpStatus.NOT_FOUND,
                                                "No relevant context found for the question"));
                            }
                            String prompt = promptBuilder.build(request, sources);
                            Flux<QueryStreamEvent> answerEvents =
                                    llmClient.streamAnswer(prompt).map(QueryStreamEvent::token);
                            Flux<QueryStreamEvent> sourceEvents =
                                    includeSources
                                            ? Flux.just(QueryStreamEvent.sources(sources))
                                            : Flux.empty();
                            return answerEvents
                                    .concatWith(sourceEvents)
                                    .concatWithValues(QueryStreamEvent.done());
                        });
    }

    private void validateQuestionLength(QueryRequest request) {
        if (request.question() != null
                && request.question().length() > queryProperties.maxQuestionLength()) {
            throw new QueryException(
                    ErrorCode.VALIDATION_ERROR,
                    HttpStatus.BAD_REQUEST,
                    "Question exceeds maximum configured length");
        }
    }
}
