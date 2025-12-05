package com.msa.license.controller;

import com.msa.license.domain.LicenseDto;
import com.msa.license.service.LicenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {
    private final LicenseService licenseService;

    @GetMapping
    public ResponseEntity<List<LicenseDto>> getLicenses() {
        List<LicenseDto> licenses = licenseService.getLicenses();
        return ResponseEntity.ok(licenses);
    }

    @GetMapping("/{licenseId}")
    public ResponseEntity<LicenseDto> getLicense(
            @PathVariable Long licenseId,
            @RequestParam(required=false, defaultValue="0") Long delay) {
        try {
            Thread.sleep(delay);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LicenseDto license = licenseService.getLicense(licenseId);
        return ResponseEntity.ok(license);
    }

    @PostMapping
    public ResponseEntity<LicenseDto> createLicense(@RequestBody LicenseDto licenseDto) {
        LicenseDto created = licenseService.createLicense(licenseDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
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