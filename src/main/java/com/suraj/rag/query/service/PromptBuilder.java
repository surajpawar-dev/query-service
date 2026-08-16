package com.suraj.rag.query.service;

import com.suraj.rag.query.config.LlmProperties;
import com.suraj.rag.query.dto.QueryHistoryMessage;
import com.suraj.rag.query.dto.QueryRequest;
import com.suraj.rag.query.dto.SourceChunk;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    private final LlmProperties properties;

    public PromptBuilder(LlmProperties properties) {
        this.properties = properties;
    }

    public String build(QueryRequest request, List<SourceChunk> sources) {
        String history = formatHistory(request.history());
        StringBuilder context = new StringBuilder();
        int remainingCharacters = properties.maxContextCharacters();

        for (int i = 0; i < sources.size() && remainingCharacters > 0; i++) {
            SourceChunk source = sources.get(i);
            String block =
                    """
                    [Source %d]
                    documentId: %s
                    chunkId: %s
                    chunkOrder: %s
                    content:
                    %s

                    """
                            .formatted(
                                    i + 1,
                                    source.documentId(),
                                    source.chunkId(),
                                    source.chunkOrder(),
                                    source.content());
            String boundedBlock =
                    block.length() <= remainingCharacters
                            ? block
                            : block.substring(0, remainingCharacters);
            context.append(boundedBlock);
            remainingCharacters -= boundedBlock.length();
        }

        return """
                Use the context below to answer the question.

                Rules:
                - Answer only from the context.
                - If the context is not enough, say you do not have enough information.
                - Keep the answer direct and useful.
                - Mention source numbers when they support the answer.

                Context:
                %s

                Conversation history:
                %s

                Question:
                %s
                """
                .formatted(context, history, request.question());
    }

    private String formatHistory(List<QueryHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return "No previous messages.";
        }

        StringBuilder builder = new StringBuilder();
        for (QueryHistoryMessage message : history) {
            if (message.content() == null || message.content().isBlank()) {
                continue;
            }
            builder.append(message.role()).append(": ").append(message.content()).append("\n");
        }
        return builder.isEmpty() ? "No previous messages." : builder.toString();
    }
}
