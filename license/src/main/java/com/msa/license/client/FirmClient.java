package com.msa.license.client;

import com.msa.license.client.dto.FirmDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Firm Service API Client
 * Base URL: /firms (복수형)
 */
@FeignClient(name = "firm")
public interface FirmClient {

    /**
     * 모든 Firm 목록 조회
     * 서버 경로: GET /firms
     */
    @GetMapping("/firms")
    List<FirmDto> getAllFirms();

    /**
     * Firm 단건 조회
     * 서버 경로: GET /firms/{id}
     */
    @GetMapping("/firms/{id}")
    FirmDto getFirmById(@PathVariable("id") Long firmId);

    /**
     * Firm 생성
     * 서버 경로: POST /firms
     */
    @PostMapping("/firms")
    FirmDto createFirm(@RequestBody FirmDto firm);

    /**
     * Firm 수정
     * 서버 경로: PUT /firms/{id}
     */
    @PutMapping("/firms/{id}")
    FirmDto updateFirm(@PathVariable("id") Long firmId, @RequestBody FirmDto firm);

    /**
     * Firm 삭제
     * 서버 경로: DELETE /firms/{id}
     */
    @DeleteMapping("/firms/{id}")
    void deleteFirm(@PathVariable("id") Long firmId);
}