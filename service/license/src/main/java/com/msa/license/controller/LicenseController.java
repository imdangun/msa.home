package com.msa.license.controller;

import com.msa.license.domain.LicenseDto;
import com.msa.license.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {
    private final LicenseService licenseService;

    @PostMapping
    public ResponseEntity<LicenseDto> createLicense(@RequestBody LicenseDto licenseDto) {
        LicenseDto created = licenseService.createLicense(licenseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<LicenseDto>> getLicenses() {
        List<LicenseDto> licenses = licenseService.getLicenses();
        return ResponseEntity.ok(licenses);
    }


    @GetMapping("/{licenseId}")
    public ResponseEntity<LicenseDto> getLicense(@PathVariable Long licenseId) {
        LicenseDto license = licenseService.getLicense(licenseId);
        return ResponseEntity.ok(license);
    }

    @PutMapping("/{licenseId}")
    public ResponseEntity<LicenseDto> updateLicense(
            @PathVariable Long licenseId,
            @RequestBody LicenseDto licenseDto) {
        LicenseDto updated = licenseService.updateLicense(licenseId, licenseDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{licenseId}")
    public ResponseEntity<Void> deleteLicense(@PathVariable Long licenseId) {
        licenseService.deleteLicense(licenseId);
        return ResponseEntity.noContent().build();
    }
}