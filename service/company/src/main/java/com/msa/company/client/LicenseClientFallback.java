package com.msa.company.client;

import com.msa.company.domain.LicenseDto;
import org.springframework.stereotype.Component;

@Component
public class LicenseClientFallback implements LicenseClient {
    @Override
    public LicenseDto getLicense(Long licenseId, Long delay) {
        LicenseDto license = new LicenseDto();
        license.setLicenseId(licenseId);
        license.setLicenseName("라이선스 서비스 응답이 없습니다.");
        license.setCreatedDate(null);

        return license;
    }
}