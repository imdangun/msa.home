package com.msa.firm.service;

import com.msa.firm.domain.Firm;
import com.msa.firm.repository.FirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FirmService {

    private final FirmRepository firmRepository;

    public List<Firm> findAll() {
        return firmRepository.findAll();
    }

    public Firm findById(Long firmId) {
        return firmRepository.findById(firmId)
                .orElseThrow(() -> new IllegalArgumentException("Firm not found: " + firmId));
    }

    @Transactional
    public Firm create(Firm firm) {
        return firmRepository.save(firm);
    }

    @Transactional
    public Firm update(Long firmId, Firm firm) {
        Firm existingFirm = findById(firmId);
        existingFirm.setFirmName(firm.getFirmName());
        return firmRepository.save(existingFirm);
    }

    @Transactional
    public void delete(Long firmId) {
        if (!firmRepository.existsById(firmId)) {
            throw new IllegalArgumentException("Firm not found: " + firmId);
        }
        firmRepository.deleteById(firmId);
    }
}