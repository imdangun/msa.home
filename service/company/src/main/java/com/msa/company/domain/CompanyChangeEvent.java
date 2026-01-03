package com.msa.company.domain;

import java.time.LocalDateTime;

public record CompanyChangeEvent (
    String action,
    Long companyId,
    String correlationId,
    LocalDateTime timestamp
) {
    public CompanyChangeEvent {
        if(companyId == null || companyId <= 0)
            throw new IllegalArgumentException("Invalid comapny Id");
        if(timestamp == null)
            timestamp = LocalDateTime.now();
    }
}