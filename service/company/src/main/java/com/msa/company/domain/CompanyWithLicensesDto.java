package com.msa.company.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompanyWithLicensesDto {
    private Long companyId;
    private String companyName;
    private List<LicenseDto> licenses;
}