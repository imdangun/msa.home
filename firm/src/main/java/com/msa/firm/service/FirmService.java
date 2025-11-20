package com.msa.firm.service;

import com.msa.firm.domain.Firm;
import com.msa.firm.repository.FirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class FirmService {
    @Autowired
    private FirmRepository firmRepository;

    public List<Firm> findAll() {
        return firmRepository.findAll();
    }

    public Firm findById(Long id) {
        return firmRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Firm not found: " + id));
    }

    @Transactional
    public Firm create(Firm firm) {
        return firmRepository.save(firm);
    }

    @Transactional
    public Firm update(Long id, Firm firm) {
        Firm existingFirm = findById(id);
        existingFirm.setFirmName(firm.getFirmName());
        // foundedDate는 수정 불가 (생성 시 자동 설정)
        return firmRepository.save(existingFirm);
    }

    @Transactional
    public void delete(Long id) {
        firmRepository.deleteById(id);
    }
}