package com.msa.license.service;

import com.msa.license.client.FirmClient;
import com.msa.license.client.dto.FirmDto;
import com.msa.license.domain.License;
import com.msa.license.dto.LicenseWithFirmDto;
import com.msa.license.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final FirmClient firmClient;

    public LicenseWithFirmDto getLicenseWithFirm(Long licenseId) {
        License license = licenseRepository.findById(licenseId)
                .orElseThrow(() -> new RuntimeException("License not found: " + licenseId));

        FirmDto firm = firmClient.getFirmById(license.getFirmId());

        return convertToDto(license, firm);
    }

    public List<LicenseWithFirmDto> getAllLicensesWithFirm() {
        List<License> licenses = licenseRepository.findAll();

        return licenses.stream()
                .map(license -> {
                    FirmDto firm = firmClient.getFirmById(license.getFirmId());
                    return convertToDto(license, firm);
                })
                .collect(Collectors.toList());
    }

    public List<LicenseWithFirmDto> getLicensesByFirm(Long firmId) {
        FirmDto firm = firmClient.getFirmById(firmId);
        List<License> licenses = licenseRepository.findByFirmId(firmId);

        return licenses.stream()
                .map(license -> convertToDto(license, firm))
                .collect(Collectors.toList());
    }

    public License getLicense(Long licenseId) {
        return licenseRepository.findById(licenseId)
                .orElseThrow(() -> new RuntimeException("License not found: " + licenseId));
    }

    public List<License> getAllLicenses() {
        return licenseRepository.findAll();
    }

    @Transactional
    public License createLicense(License license) {
        firmClient.getFirmById(license.getFirmId());
        return licenseRepository.save(license);
    }

    @Transactional
    public License updateLicense(Long licenseId, License updatedLicense) {
        License license = getLicense(licenseId);
        license.setLicenseName(updatedLicense.getLicenseName());
        license.setFirmId(updatedLicense.getFirmId());

        firmClient.getFirmById(license.getFirmId());

        return licenseRepository.save(license);
    }

    @Transactional
    public void deleteLicense(Long licenseId) {
        License license = getLicense(licenseId);
        licenseRepository.delete(license);
    }

    public List<FirmDto> getAllFirms() {
        return firmClient.getAllFirms();
    }

    private LicenseWithFirmDto convertToDto(License license, FirmDto firm) {
        LicenseWithFirmDto dto = new LicenseWithFirmDto();
        dto.setLicenseId(license.getLicenseId());
        dto.setLicenseName(license.getLicenseName());
        dto.setCreatedDate(license.getCreatedDate());
        dto.setFirmId(license.getFirmId());
        dto.setFirm(firm);
        return dto;
    }
}