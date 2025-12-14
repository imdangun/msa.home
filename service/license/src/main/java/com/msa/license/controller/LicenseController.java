package com.msa.license.controller;

import com.msa.license.domain.LicenseDto;
import com.msa.license.service.LicenseService;
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
@RequestMapping("/license")
@RequiredArgsConstructor
public class LicenseController {
    private final LicenseService licenseService;

    @GetMapping
    public ResponseEntity<List<LicenseDto>> getLicenses(
            @AuthenticationPrincipal Jwt jwt) {
        log.info("User: {}", jwt.getSubject());
        return ResponseEntity.ok(licenseService.getLicenses());
    }

    @GetMapping("/{licenseId}")
    public ResponseEntity<LicenseDto> getLicense(
            @PathVariable Long licenseId,
            @RequestParam(required=false, defaultValue="0") Long delay,
            @RequestHeader(value="Correlation-Id", required=false) String correlationId,
            @AuthenticationPrincipal Jwt jwt) {
        log.info("User: {}", jwt.getSubject());
        log.info("🔗 License Correlation-Id: {}", correlationId);

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