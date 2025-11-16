package com.msa.license.repository;

import com.msa.license.domain.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByLicenseName(String licenseName);
    boolean existsByLicenseName(String licenseName);

    // 라이선스 이름에 특정 문자열 포함된 것 조회
    //List<License> findByLicenseNameContaining(String keyword);

    // 생성일 기준 조회
    //List<License> findByCreatedDateAfter(LocalDate date);

    // 기본 CRUD 메서드는 JpaRepository가 자동 제공:
    // - save(License)
    // - findById(Long)
    // - findAll()
    // - deleteById(Long)
    // - count()
}