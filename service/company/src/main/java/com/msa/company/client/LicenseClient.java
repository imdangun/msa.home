package com.msa.company.client;

import com.msa.company.domain.LicenseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="license", fallback=LicenseClientFallback.class)
public interface LicenseClient {
    @GetMapping("/license/{licenseId}") // <license> controller URL
    LicenseDto getLicense(@PathVariable Long licenseId);
}