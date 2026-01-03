package com.msa.company.service;

import com.msa.company.domain.CompanyChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyEventPublisher {
    private final StreamBridge streamBridge;

    public void publishCompanyChange(String action, Long companyId) {
        String correlationId = UUID.randomUUID().toString();

        CompanyChangeEvent event = new CompanyChangeEvent(
                action,
                companyId,
                correlationId,
                LocalDateTime.now());

        log.info("Company change event 발행: action={}, companyId={}, correlationId={}",
                action, companyId, correlationId);

        boolean sent = streamBridge.send(
                "companyChangeSupplier-out-0",
                MessageBuilder
                        .withPayload(event)
                        .setHeader("action", action)
                        .setHeader("correlationId", correlationId)
                        .build());

        if (sent)
            log.info("Event 발행 성공: {}", correlationId);
        else
            log.error("Event 발행 실패: {}", correlationId);
    }
}