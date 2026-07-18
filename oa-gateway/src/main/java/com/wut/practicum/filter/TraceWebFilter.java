package com.wut.practicum.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceWebFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(TraceWebFilter.class);
    public static final String HEADER = "X-Trace-Id";
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String supplied = exchange.getRequest().getHeaders().getFirst(HEADER);
        String traceId = supplied != null && supplied.matches("[A-Za-z0-9_-]{8,64}") ? supplied : UUID.randomUUID().toString().replace("-", "");
        long startedAt = System.nanoTime();
        ServerHttpRequest request = exchange.getRequest().mutate().headers(headers -> headers.set(HEADER, traceId)).build();
        exchange.getResponse().getHeaders().set(HEADER, traceId);
        ServerWebExchange tracedExchange = exchange.mutate().request(request).build();
        return chain.filter(tracedExchange).doFinally(signal -> {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            var status = exchange.getResponse().getStatusCode();
            log.info("Gateway request method={} path={} origin={} host={} status={} durationMs={} traceId={}",
                    request.getMethod(), request.getPath().value(), request.getHeaders().getOrigin(), request.getHeaders().getHost(),
                    status == null ? 200 : status.value(), durationMs, traceId);
        });
    }
}
