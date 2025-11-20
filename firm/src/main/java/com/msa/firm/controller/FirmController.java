package com.msa.firm.controller;

import com.msa.firm.domain.Firm;
import com.msa.firm.service.FirmService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/firm")
public class FirmController {
    @Autowired
    private FirmService firmService;

    @GetMapping
    public ResponseEntity<List<Firm>> getAllFirms() {
        return ResponseEntity.ok(firmService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Firm> getFirm(@PathVariable Long id) {
        return ResponseEntity.ok(firmService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Firm> createFirm(@RequestBody Firm firm) {
        Firm created = firmService.create(firm);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Firm> updateFirm(@PathVariable Long id,
                                           @RequestBody Firm firm) {
        Firm updated = firmService.update(id, firm);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFirm(@PathVariable Long id) {
        firmService.delete(id);
        return ResponseEntity.noContent().build();
    }
}