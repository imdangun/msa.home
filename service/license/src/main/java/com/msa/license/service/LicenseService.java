package com.msa.license.service;

import com.msa.license.domain.License;
import com.msa.license.domain.LicenseDto;
import com.msa.license.domain.LicenseMapper;
import com.msa.license.repository.LicenseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LicenseService {
    private final LicenseRepository licenseRepository;
    private final LicenseMapper licenseMapper;

    private License findLicenseById(Long licenseId) {
        return licenseRepository.findById(licenseId)
                .orElseThrow(() -> new EntityNotFoundException("라이선스 " + licenseId +"가 없습니다."));
    }

    public List<LicenseDto> getLicenses() {
        return licenseMapper.toDtoList(licenseRepository.findAll());
    }

    public LicenseDto getLicense(Long licenseId) {
        return licenseMapper.toDto(findLicenseById(licenseId));
    }

    @Transactional
    public LicenseDto createLicense(LicenseDto licenseDto) {
        return licenseMapper.toDto(licenseRepository.save(licenseMapper.toEntity(licenseDto)));
    }

    @Transactional
    public LicenseDto updateLicense(Long licenseId, LicenseDto licenseDto) {
        License license = findLicenseById(licenseId);
        license.setLicenseName(licenseDto.getLicenseName());
        return licenseMapper.toDto(license);
    }

    @Transactional
    public void deleteLicense(Long licenseId) {
        if(!licenseRepository.existsById(licenseId))
            throw new EntityNotFoundException("라이선스 " + licenseId + "가 없습니다.");
        licenseRepository.deleteById(licenseId);
    }

}