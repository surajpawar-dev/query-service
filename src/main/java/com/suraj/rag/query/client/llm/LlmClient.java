package com.suraj.rag.query.client.llm;

import com.suraj.rag.query.dto.QueryHistoryMessage;
import java.util.List;
import reactor.core.publisher.Flux;

public interface LlmClient {

    Flux<String> streamGroundedAnswer(String prompt);

    Flux<String> streamDirectAnswer(String question, List<QueryHistoryMessage> history);
}
