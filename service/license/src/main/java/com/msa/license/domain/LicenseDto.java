package com.msa.license.domain;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Getter
@Setter
public class LicenseDto {
    private Long licenseId;
    private String licenseName;
    private LocalDate createdDate;
}
