package com.suraj.rag.query.client.llm;

import com.suraj.rag.query.config.LlmProperties;
import com.suraj.rag.query.dto.QueryHistoryMessage;
import com.suraj.rag.query.exception.ErrorCode;
import com.suraj.rag.query.exception.QueryException;
import java.time.Duration;
import java.util.ArrayList;
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
    public Flux<String> streamDirectAnswer(String question, List<QueryHistoryMessage> history) {
        return streamAnswer(
                "You are a helpful assistant. Use the conversation history when it is relevant. Keep answers direct and useful.",
                history,
                question);
    }

    private Flux<String> streamAnswer(String systemMessage, String userMessage) {
        return streamAnswer(systemMessage, List.of(), userMessage);
    }

    private Flux<String> streamAnswer(
            String systemMessage, List<QueryHistoryMessage> history, String userMessage) {
        List<OllamaMessage> messages = new ArrayList<>();
        messages.add(new OllamaMessage("system", systemMessage));
        if (history != null) {
            history.stream()
                    .filter(message -> message.content() != null && !message.content().isBlank())
                    .filter(
                            message ->
                                    "user".equals(message.role())
                                            || "assistant".equals(message.role()))
                    .map(message -> new OllamaMessage(message.role(), message.content()))
                    .forEach(messages::add);
        }
        messages.add(new OllamaMessage("user", userMessage));

        OllamaChatRequest request =
                new OllamaChatRequest(
                        properties.model(),
                        messages,
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
