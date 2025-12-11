package com.msa.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    private static final String CORRELATION_ID = "Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_ID);

        if (correlationId == null)
            correlationId = UUID.randomUUID().toString();

        log.info("🔗 Correlation-Id: {}", correlationId);

        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(CORRELATION_ID, correlationId)
                .build();

        ServerWebExchange newExchange = exchange.mutate()
                .request(request)
                .build();

        newExchange.getResponse()
                .getHeaders()
                .add(CORRELATION_ID, correlationId);

        return chain.filter(newExchange);
    }

    @Override
    public int getOrder() {
        return -2;
    }
}