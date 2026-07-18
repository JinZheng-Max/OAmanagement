package com.wut.practicum.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wut.practicum.security.GatewayJwtService;
import io.jsonwebtoken.JwtException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GatewaySecurityFilter implements WebFilter {
    private static final String SESSION_PREFIX = "oa:auth:session:";
    private final GatewayJwtService jwtService;
    private final ReactiveStringRedisTemplate redis;
    private final ObjectMapper mapper;

    public GatewaySecurityFilter(GatewayJwtService jwtService, ReactiveStringRedisTemplate redis, ObjectMapper mapper) {
        this.jwtService = jwtService;
        this.redis = redis;
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/internal")) return error(exchange, HttpStatus.NOT_FOUND, "资源不存在");
        if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod()) || path.equals("/api/auth/login") || path.equals("/actuator/health")) {
            return chain.filter(exchange);
        }
        String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) return error(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        try {
            GatewayJwtService.GatewayPrincipal principal = jwtService.parse(authorization.substring(7));
            return redis.hasKey(SESSION_PREFIX + principal.jti()).onErrorReturn(false)
                    .flatMap(active -> active ? chain.filter(exchange) : error(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已过期"));
        } catch (JwtException | IllegalArgumentException exception) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String traceId = exchange.getResponse().getHeaders().getFirst(TraceWebFilter.HEADER);
        try {
            byte[] bytes = mapper.writeValueAsBytes(Map.of("code", status.value(), "message", message, "data", "", "traceId", traceId == null ? "" : traceId));
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        } catch (JsonProcessingException exception) {
            byte[] bytes = "{\"code\":500,\"message\":\"serialization error\"}".getBytes(StandardCharsets.UTF_8);
            return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
        }
    }
}
