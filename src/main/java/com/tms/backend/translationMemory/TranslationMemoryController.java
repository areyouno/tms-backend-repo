package com.tms.backend.translationMemory;

import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.TranslationMemoryDTO;
import com.tms.backend.user.CustomUserDetails;

@RestController
@RequestMapping("/tm")
public class TranslationMemoryController {
    private final TranslationMemoryService tmService;

    public TranslationMemoryController(TranslationMemoryService tmService){
        this.tmService = tmService;
    }
    
    @PostMapping("/create")
    public TranslationMemoryDTO saveTM(@RequestBody TranslationMemoryDTO request, Authentication authentication){
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String uid = userDetails.getUid();

        return tmService.save(
                request.name(),
                uid,
                LocalDateTime.now(),
                request.sourceLang(),
                request.targetLang()
        );
    }
}
