package com.msa.company.controller;

import com.msa.company.domain.CompanyDto;
import com.msa.company.domain.CompanyWithLicensesDto;
import com.msa.company.service.CompanyService;
import jakarta.ws.rs.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/company")
@RequiredArgsConstructor
public class CompanyController {
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<CompanyDto>> getAllCompanies() {
        return ResponseEntity.ok(companyService.getAllCompanies());
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<CompanyDto> getCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(companyService.getCompany(companyId));
    }

    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(@RequestBody CompanyDto companyDto) {
        CompanyDto created = companyService.createCompany(companyDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<CompanyDto> updateCompany(
            @PathVariable Long companyId,
            @RequestBody CompanyDto companyDto) {
        return ResponseEntity.ok(companyService.updateCompany(companyId, companyDto));
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable Long companyId) {
        companyService.deleteCompany(companyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{companyId}/licenses")
    public ResponseEntity<CompanyWithLicensesDto> getCompanyWithLicenses(@PathVariable Long companyId) {
        return ResponseEntity.ok(companyService.getCompanyWithLicenses(companyId));
    }

    @PostMapping("/{companyId}/license/{licenseId}")
    public ResponseEntity<CompanyDto> addLicense(
            @PathVariable Long companyId,
            @PathVariable Long licenseId) {
        return ResponseEntity.ok(companyService.addLicenseToCompany(companyId, licenseId));
    }

    @DeleteMapping("/{companyId}/license/{licenseId}")
    public ResponseEntity<CompanyDto> removeLicense(
            @PathVariable Long companyId,
            @PathVariable Long licenseId) {
        return ResponseEntity.ok(companyService.removeLicenseFromCompany(companyId, licenseId));
    }
}