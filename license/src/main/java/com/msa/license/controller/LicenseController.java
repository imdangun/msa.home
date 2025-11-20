package com.msa.license.controller;

import com.msa.license.client.dto.FirmDto;
import com.msa.license.domain.License;
import com.msa.license.dto.LicenseWithFirmDto;
import com.msa.license.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/licenses")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping("/{licenseId}/with-firm")
    public ResponseEntity<LicenseWithFirmDto> getLicenseWithFirm(@PathVariable Long licenseId) {
        return ResponseEntity.ok(licenseService.getLicenseWithFirm(licenseId));
    }

    @GetMapping("/with-firm")
    public ResponseEntity<List<LicenseWithFirmDto>> getAllLicensesWithFirm() {
        return ResponseEntity.ok(licenseService.getAllLicensesWithFirm());
    }

    @GetMapping("/by-firm/{firmId}")
    public ResponseEntity<List<LicenseWithFirmDto>> getLicensesByFirm(@PathVariable Long firmId) {
        return ResponseEntity.ok(licenseService.getLicensesByFirm(firmId));
    }

    @GetMapping("/test/firms")
    public ResponseEntity<List<FirmDto>> getAllFirms() {
        return ResponseEntity.ok(licenseService.getAllFirms());
    }

    @GetMapping("/{licenseId}")
    public ResponseEntity<License> getLicense(@PathVariable Long licenseId) {
        return ResponseEntity.ok(licenseService.getLicense(licenseId));
    }

    @GetMapping
    public ResponseEntity<List<License>> getAllLicenses() {
        return ResponseEntity.ok(licenseService.getAllLicenses());
    }

    @PostMapping
    public ResponseEntity<License> createLicense(@RequestBody License license) {
        License created = licenseService.createLicense(license);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{licenseId}")
    public ResponseEntity<License> updateLicense(@PathVariable Long licenseId, @RequestBody License license) {
        return ResponseEntity.ok(licenseService.updateLicense(licenseId, license));
    }

    @DeleteMapping("/{licenseId}")
    public ResponseEntity<Void> deleteLicense(@PathVariable Long licenseId) {
        licenseService.deleteLicense(licenseId);
        return ResponseEntity.noContent().build();
    }
}