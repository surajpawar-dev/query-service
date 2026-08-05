package com.suraj.rag.query.client.llm;

import reactor.core.publisher.Flux;

public interface LlmClient {

    Flux<String> streamAnswer(String prompt);
}
