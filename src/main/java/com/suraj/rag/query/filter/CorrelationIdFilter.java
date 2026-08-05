package com.suraj.rag.query.filter;

import com.suraj.rag.query.common.HeaderNames;
import com.suraj.rag.query.common.MdcKeys;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = resolveCorrelationId(exchange.getRequest());
        exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, correlationId);
        return chain.filter(exchange)
                .contextWrite(context -> context.put(MdcKeys.CORRELATION_ID, correlationId))
                .doFinally(
                        signal -> {
                            MDC.remove(MdcKeys.CORRELATION_ID);
                        });
    }

    private String resolveCorrelationId(ServerHttpRequest request) {
        String value = request.getHeaders().getFirst(HeaderNames.CORRELATION_ID);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}
