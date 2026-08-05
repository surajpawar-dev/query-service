package com.suraj.rag.query.controller;

import com.suraj.rag.query.common.ApiPaths;
import com.suraj.rag.query.dto.QueryRequest;
import com.suraj.rag.query.dto.QueryStreamEvent;
import com.suraj.rag.query.service.QueryOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(ApiPaths.QUERY_BASE)
public class QueryController {

    private final QueryOrchestrationService queryOrchestrationService;

    public QueryController(QueryOrchestrationService queryOrchestrationService) {
        this.queryOrchestrationService = queryOrchestrationService;
    }

    @PostMapping(value = ApiPaths.QUERY_STREAM, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<QueryStreamEvent>> streamAnswer(
            @Valid @RequestBody QueryRequest request) {
        return queryOrchestrationService.stream(request)
                .map(event -> ServerSentEvent.builder(event).event(event.type()).build());
    }
}
