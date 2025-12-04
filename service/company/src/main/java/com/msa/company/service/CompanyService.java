package com.msa.company.service;

import com.msa.company.client.LicenseClient;
import com.msa.company.domain.*;
import com.msa.company.repository.CompanyRepository;
import jakarta.persistence.EntityNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyMapper companyMapper;
    private final LicenseClient licenseClient;

    private Company findCompanyById(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("조직 " + companyId + "가 없습니다."));
    }

    public List<CompanyDto> getAllCompanies() {
        return companyMapper.toDtoList(companyRepository.findAll());
    }

    public CompanyDto getCompany(Long companyId) {
        return companyMapper.toDto(findCompanyById(companyId));
    }

    @Transactional
    public CompanyDto createCompany(CompanyDto companyDto) {
        return companyMapper.toDto(companyRepository.save(companyMapper.toEntity(companyDto)));
    }

    @Transactional
    public CompanyDto updateCompany(Long companyId, CompanyDto companyDto) {
        Company company = findCompanyById(companyId);
        company.setCompanyName(companyDto.getCompanyName());
        return companyMapper.toDto(company);
    }

    @Transactional
    public void deleteCompany(Long companyId) {
        companyRepository.delete(findCompanyById(companyId));
    }

    @Bulkhead(name="license", fallbackMethod="getCompanyWithLicensesFallback")
    public CompanyWithLicensesDto getCompanyWithLicenses(Long companyId, Long delay) {
        log.info("▶ LICENSE: {}", Thread.currentThread().getName());
        Company company = findCompanyById(companyId);

        List<LicenseDto> licenses = company.getLicenseIds().stream()
                .map(licenseId -> licenseClient.getLicense(licenseId, delay))
                .collect(Collectors.toList());

        CompanyWithLicensesDto dto = new CompanyWithLicensesDto();
        dto.setCompanyId(company.getCompanyId());
        dto.setCompanyName(company.getCompanyName());
        dto.setLicenses(licenses);

        return dto;
    }

    private CompanyWithLicensesDto getCompanyWithLicensesFallback(
            Long companyId, Long delay, Exception e) {
        log.warn("● FALLBACK: {}, {}", Thread.currentThread().getName(), e.getClass().getSimpleName());
        Company company = findCompanyById(companyId);

        CompanyWithLicensesDto dto = new CompanyWithLicensesDto();
        dto.setCompanyId(company.getCompanyId());
        dto.setCompanyName(company.getCompanyName() + " (라이선스 서비스 제한)");
        dto.setLicenses(List.of());

        return dto;
    }

    @Transactional
    public CompanyDto addLicenseToCompany(Long companyId, Long licenseId) {
        Company company = findCompanyById(companyId);
        company.getLicenseIds().add(licenseId);
        return companyMapper.toDto(company);
    }

    @Transactional
    public CompanyDto removeLicenseFromCompany(Long companyId, Long licenseId) {
        Company company = findCompanyById(companyId);
        company.getLicenseIds().remove(licenseId);
        return companyMapper.toDto(company);
    }
}