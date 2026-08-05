package com.suraj.rag.query.client.embedding;

import com.suraj.rag.query.config.EmbeddingServiceProperties;
import com.suraj.rag.query.dto.EmbeddingSearchRequest;
import com.suraj.rag.query.dto.EmbeddingSearchResponse;
import com.suraj.rag.query.exception.ErrorCode;
import com.suraj.rag.query.exception.QueryException;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class EmbeddingSearchClient {

    private static final String SEARCH_PATH = "/api/v1/embeddings/search";

    private final WebClient webClient;
    private final Duration requestTimeout;

    public EmbeddingSearchClient(
            WebClient.Builder webClientBuilder, EmbeddingServiceProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
        this.requestTimeout = properties.requestTimeout();
    }

    public Mono<EmbeddingSearchResponse> search(EmbeddingSearchRequest request) {
        return webClient
                .post()
                .uri(SEARCH_PATH)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(EmbeddingSearchResponse.class)
                .timeout(requestTimeout)
                .onErrorMap(
                        exception ->
                                new QueryException(
                                        ErrorCode.EMBEDDING_SEARCH_FAILED,
                                        HttpStatus.BAD_GATEWAY,
                                        "Embedding search failed"));
    }
}
