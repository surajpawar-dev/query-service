package com.suraj.rag.query.dto;

import java.util.List;

public record QueryStreamEvent(String type, String content, List<SourceChunk> sources) {

    public static QueryStreamEvent token(String token) {
        return new QueryStreamEvent("token", token, null);
    }

    public static QueryStreamEvent sources(List<SourceChunk> sources) {
        return new QueryStreamEvent("sources", null, sources);
    }

    public static QueryStreamEvent done() {
        return new QueryStreamEvent("done", null, null);
    }
}
