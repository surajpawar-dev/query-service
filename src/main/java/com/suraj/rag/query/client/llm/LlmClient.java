package com.suraj.rag.query.client.llm;

import reactor.core.publisher.Flux;

public interface LlmClient {

    Flux<String> streamGroundedAnswer(String prompt);

    Flux<String> streamDirectAnswer(String question);
}
