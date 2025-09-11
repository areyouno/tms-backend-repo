package com.tms.backend.translationMemoryDetail;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tms.backend.translationMemory.TranslationMemory;
import com.tms.backend.translationMemory.TranslationMemoryRepository;

@Service
public class TranslationMemoryDetailService {
    private final TranslationMemoryDetailRepository detailRepo;
    private final TranslationMemoryRepository tmRepo;

    public TranslationMemoryDetailService(TranslationMemoryDetailRepository detailRepo,
                                          TranslationMemoryRepository tmRepo) {
        this.detailRepo = detailRepo;
        this.tmRepo = tmRepo;
    }

    public List<TranslationMemoryDetail> getDetailsByTm(Long tmId) {
        TranslationMemory tm = tmRepo.findById(tmId)
            .orElseThrow(() -> new IllegalArgumentException("TM not found with id " + tmId));
        return tm.getDetails();
    }

    public void deleteDetail(Long detailId) {
        detailRepo.deleteById(detailId);
    }
    
}
