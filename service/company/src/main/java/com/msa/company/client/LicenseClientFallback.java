package com.msa.company.client;

import com.msa.company.domain.LicenseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LicenseClientFallback implements LicenseClient {
    @Override
    public LicenseDto getLicense(Long licenseId, Long delay) {
        log.error("🔴 Fallback, license {}", licenseId);
        LicenseDto license = new LicenseDto();
        license.setLicenseId(licenseId);
        license.setLicenseName("라이선스 서비스 응답이 없습니다.");
        license.setCreatedDate(null);

        return license;
    }
}