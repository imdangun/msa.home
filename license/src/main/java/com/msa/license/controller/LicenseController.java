package com.msa.license.controller;

import com.msa.license.dto.LicenseRequest;
import com.msa.license.dto.LicenseResponse;
import com.msa.license.service.LicenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;
    private final MessageSource msg;

    /**
     * 전체 라이선스 조회
     * GET /api/license
     */
    @GetMapping
    public ResponseEntity<List<LicenseResponse>> getAllLicenses() {
        List<LicenseResponse> licenses = licenseService.getAllLicenses();
        return ResponseEntity.ok(licenses);
    }

    /**
     * ID로 라이선스 조회
     * GET /api/license/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<LicenseResponse> getLicenseById(@PathVariable Long id) {
        LicenseResponse license = licenseService.getLicenseById(id);
        return ResponseEntity.ok(license);
    }

    /**
     * 이름으로 라이선스 조회
     * GET /api/license/name/{name}
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<LicenseResponse> getLicenseByName(@PathVariable String name) {
        LicenseResponse license = licenseService.getLicenseByName(name);
        return ResponseEntity.ok(license);
    }

    /**
     * 라이선스 생성
     * POST /api/license
     */
    @PostMapping
    public ResponseEntity<LicenseResponse> createLicense(@Valid @RequestBody LicenseRequest request) {
        LicenseResponse license = licenseService.createLicense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(license);
    }

    /**
     * 라이선스 수정
     * PUT /api/license/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<LicenseResponse> updateLicense(
            @PathVariable Long id,
            @Valid @RequestBody LicenseRequest request) {
        LicenseResponse license = licenseService.updateLicense(id, request);
        return ResponseEntity.ok(license);
    }

    /**
     * 라이선스 삭제
     * DELETE /api/license/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLicense(@PathVariable Long id) {
        licenseService.deleteLicense(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 전체 개수 조회
     * GET /api/license/count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> count() {
        long count = licenseService.count();
        return ResponseEntity.ok(count);
    }

    @GetMapping("/hello")
    public ResponseEntity<String> hello(
            @RequestHeader(value="Accept-Language", required=false) Locale locale) {
        System.out.println(msg.getMessage("license.msg.welcome", null, locale));
        return ResponseEntity.ok(msg.getMessage("license.msg.welcome", null, locale));
    }


    @GetMapping("/hateoas/{id}")
    public ResponseEntity<LicenseResponse> getLicense(@PathVariable Long id) {
        LicenseResponse license = licenseService.getLicenseById(id);

        // self 링크
        license.add(linkTo(methodOn(LicenseController.class).getLicense(id)).withSelfRel());

        // update 링크
        license.add(linkTo(methodOn(LicenseController.class).updateLicense(id, null)).withRel("update"));

        // delete 링크
        license.add(linkTo(methodOn(LicenseController.class).deleteLicense(id)).withRel("delete"));

        // all-licenses 링크
        license.add(linkTo(methodOn(LicenseController.class).getAllLicenses()).withRel("all-licenses"));

        return ResponseEntity.ok(license);
    }
}