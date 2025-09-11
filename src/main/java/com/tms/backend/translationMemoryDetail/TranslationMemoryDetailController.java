package com.tms.backend.translationMemoryDetail;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tmdetail")
public class TranslationMemoryDetailController {
    private final TranslationMemoryDetailService detailService;

    public TranslationMemoryDetailController(TranslationMemoryDetailService detailService) {
        this.detailService = detailService;
    }

    @GetMapping("/{tmId}")
    public List<TranslationMemoryDetail> getDetailsByTm(@PathVariable Long tmId) {
        return detailService.getDetailsByTm(tmId);

    }

    @DeleteMapping("/{detailId}")
    public void deleteDetail(@PathVariable Long detailId) {
        detailService.deleteDetail(detailId);
    }
}
