package com.suraj.rag.query.client.llm;

import com.suraj.rag.query.config.LlmProperties;
import com.suraj.rag.query.exception.ErrorCode;
import com.suraj.rag.query.exception.QueryException;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
public class OllamaChatClient implements LlmClient {

    private static final String CHAT_PATH = "/api/chat";

    private final WebClient webClient;
    private final LlmProperties properties;
    private final Duration requestTimeout;

    public OllamaChatClient(WebClient.Builder webClientBuilder, LlmProperties properties) {
        this.webClient = webClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
        this.requestTimeout = properties.requestTimeout();
    }

    @Override
    public Flux<String> streamGroundedAnswer(String prompt) {
        return streamAnswer(
                "You answer using only the supplied context. If the context is insufficient, say that clearly.",
                prompt);
    }

    @Override
    public Flux<String> streamDirectAnswer(String question) {
        return streamAnswer(
                "You are a helpful assistant. Keep answers direct and useful.", question);
    }

    private Flux<String> streamAnswer(String systemMessage, String userMessage) {
        OllamaChatRequest request =
                new OllamaChatRequest(
                        properties.model(),
                        List.of(
                                new OllamaMessage("system", systemMessage),
                                new OllamaMessage("user", userMessage)),
                        true,
                        new OllamaOptions(properties.temperature()));

        return webClient
                .post()
                .uri(CHAT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(OllamaChatStreamResponse.class)
                .timeout(requestTimeout)
                .filter(
                        response ->
                                response.message() != null && response.message().content() != null)
                .map(response -> response.message().content())
                .filter(token -> !token.isBlank())
                .onErrorMap(
                        exception ->
                                new QueryException(
                                        ErrorCode.LLM_STREAM_FAILED,
                                        HttpStatus.BAD_GATEWAY,
                                        "LLM streaming failed"));
    }

    private record OllamaChatRequest(
            String model, List<OllamaMessage> messages, boolean stream, OllamaOptions options) {}

    private record OllamaOptions(double temperature) {}

    private record OllamaMessage(String role, String content) {}

    private record OllamaChatStreamResponse(OllamaMessage message, boolean done) {}
}
