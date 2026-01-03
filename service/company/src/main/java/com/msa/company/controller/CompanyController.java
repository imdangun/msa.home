package com.msa.company.controller;

import com.msa.company.domain.CompanyDto;
import com.msa.company.domain.CompanyWithLicensesDto;
import com.msa.company.domain.LicenseDto;
import com.msa.company.service.CompanyEventPublisher;
import com.msa.company.service.CompanyService;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;
    private final CompanyEventPublisher eventPublisher;

    @GetMapping
    public ResponseEntity<List<CompanyDto>> getAllCompanies(
            @AuthenticationPrincipal Jwt jwt) {
        log.info("User: {}", jwt.getSubject());
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyDto> getCompany(
            @PathVariable Long companyId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("User: {}", jwt.getSubject());
        return ResponseEntity.ok(companyService.getCompany(companyId));
    }

    @GetMapping("/{companyId}/licenses")
    public ResponseEntity<CompanyWithLicensesDto> getCompanyWithLicenses(
            @PathVariable Long companyId,
            @RequestParam(required=false, defaultValue="0") Long delay,
            @RequestHeader(value="Correlation-Id", required=false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("User: {}", jwt.getSubject());
        log.info("🔗 Company Correlation-Id: {}", correlationId);
        return ResponseEntity.ok(companyService.getCompanyWithLicenses(companyId, delay));
    }

    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(@RequestBody CompanyDto companyDto) {
        CompanyDto company = companyService.createCompany(companyDto);
        eventPublisher.publishCompanyChange("CREATE", company.getCompanyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(company);
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<CompanyDto> updateCompany(
            @PathVariable Long companyId,
            @RequestBody CompanyDto companyDto) {
        CompanyDto company = companyService.updateCompany(companyId, companyDto);
        eventPublisher.publishCompanyChange("UPDATE", companyId);
        return ResponseEntity.ok(company);
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long companyId) {
        companyService.deleteCompany(companyId);
        eventPublisher.publishCompanyChange("DELETE", companyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{companyId}/license/{licenseId}")
    public ResponseEntity<CompanyDto> addLicense(
            @PathVariable Long companyId,
            @PathVariable Long licenseId) {
        CompanyDto company = companyService.addLicenseToCompany(companyId, licenseId);
        eventPublisher.publishCompanyChange("UPDATE", companyId);
        return ResponseEntity.ok(company);
    }

    @DeleteMapping("/{companyId}/license/{licenseId}")
    public ResponseEntity<CompanyDto> removeLicense(
            @PathVariable Long companyId,
            @PathVariable Long licenseId) {
        CompanyDto company = companyService.removeLicenseFromCompany(companyId, licenseId);
        eventPublisher.publishCompanyChange("UPDATE", companyId);
        return ResponseEntity.ok(company);
    }
}