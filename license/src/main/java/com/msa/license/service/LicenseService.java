package com.msa.license.service;

import com.msa.license.domain.License;
import com.msa.license.dto.LicenseRequest;
import com.msa.license.dto.LicenseResponse;
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

    /**
     * 전체 라이선스 조회
     */
    public List<LicenseResponse> getAllLicenses() {
        return licenseRepository.findAll().stream()
                .map(LicenseResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * ID로 라이선스 조회
     */
    public LicenseResponse getLicenseById(Long id) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License not found with id: " + id));
        return LicenseResponse.from(license);
    }

    /**
     * 이름으로 라이선스 조회
     */
    public LicenseResponse getLicenseByName(String licenseName) {
        License license = licenseRepository.findByLicenseName(licenseName)
                .orElseThrow(() -> new IllegalArgumentException("License not found with name: " + licenseName));
        return LicenseResponse.from(license);
    }

    /**
     * 라이선스 생성
     */
    @Transactional
    public LicenseResponse createLicense(LicenseRequest request) {
        // 중복 체크
         /*
        if (licenseRepository.existsByLicenseName(request.getLicenseName())) {
            throw new IllegalArgumentException("License already exists with name: " + request.getLicenseName());
        } */

        // DTO → Entity 변환
        License license = new License();
        license.setLicenseName(request.getLicenseName());
        license.setCreatedDate(java.time.LocalDate.now());

        // 저장
        License savedLicense = licenseRepository.save(license);

        // Entity → DTO 변환
        return LicenseResponse.from(savedLicense);
    }

    /**
     * 라이선스 수정
     */
    @Transactional
    public LicenseResponse updateLicense(Long id, LicenseRequest request) {
        // 존재 확인
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("License not found with id: " + id));

        // 다른 라이선스가 같은 이름을 사용하는지 확인
        licenseRepository.findByLicenseName(request.getLicenseName())
                .ifPresent(existing -> {
                    if (!existing.getLicenseId().equals(id)) {
                        throw new IllegalArgumentException("License name already in use: " + request.getLicenseName());
                    }
                });

        // 수정
        license.setLicenseName(request.getLicenseName());

        // 저장 및 반환
        License updatedLicense = licenseRepository.save(license);
        return LicenseResponse.from(updatedLicense);
    }

    /**
     * 라이선스 삭제
     */
    @Transactional
    public void deleteLicense(Long id) {
        if (!licenseRepository.existsById(id)) {
            throw new IllegalArgumentException("License not found with id: " + id);
        }

        licenseRepository.deleteById(id);
    }

    /**
     * 전체 라이선스 개수
     */
    public long count() {
        return licenseRepository.count();
    }
}