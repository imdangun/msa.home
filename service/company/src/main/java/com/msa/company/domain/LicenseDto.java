package com.msa.company.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class LicenseDto {
    private Long licenseId;
    private String licenseName;
    private LocalDate createdDate;
}