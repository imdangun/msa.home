package com.msa.license.dto;

import com.msa.license.domain.License;
import lombok.Builder;
import lombok.Getter;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;

@Getter
@Builder
public class LicenseResponse  extends RepresentationModel<LicenseResponse> {

    private Long licenseId;
    private String licenseName;
    private LocalDate createdDate;

    /**
     * Entity → DTO 변환
     */
    public static LicenseResponse from(License license) {
        return LicenseResponse.builder()
                .licenseId(license.getLicenseId())
                .licenseName(license.getLicenseName())
                .createdDate(license.getCreatedDate())
                .build();
    }
}