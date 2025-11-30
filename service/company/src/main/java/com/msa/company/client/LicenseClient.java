package com.msa.company.client;

import com.msa.company.domain.LicenseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="license") // eureka에 등록된 servie name
public interface LicenseClient {
    @GetMapping("/license/{licenseId}") // <license> controller URL
    LicenseDto getLicense(@PathVariable Long licenseId);
}