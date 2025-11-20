package com.msa.license.dto;

import com.msa.license.client.dto.FirmDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseWithFirmDto {
    private Long licenseId;
    private String licenseName;
    private LocalDate createdDate;
    private Long firmId;
    private FirmDto firm;
}