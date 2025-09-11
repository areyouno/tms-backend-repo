package com.tms.backend.translationMemory;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import com.tms.backend.dto.TranslationMemoryDTO;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

@Service
public class TranslationMemoryService {
    private TranslationMemoryRepository tmRepo;
    private UserRepository userRepo;

    public TranslationMemoryService(TranslationMemoryRepository repo, UserRepository userRepo){
        this.tmRepo = repo;
        this.userRepo = userRepo;
    }


    public TranslationMemoryDTO save(String name, String createdBy,
    LocalDateTime createDate, String sourceLang, String targetLang){
        TranslationMemory tm = new TranslationMemory();
        tm.setName(name);
        tm.setCreateDate(createDate);
        tm.setSourceLang(sourceLang);
        tm.setTargetLang(targetLang);

        tm.setCreatedBy(createdBy);

        User user = userRepo.findByUid(createdBy)   // or email/username
        .orElseThrow(() -> new IllegalArgumentException("User not found"));
        tm.setOwner(user);

        TranslationMemory saved = tmRepo.save(tm);
        return convertToFullDTO(saved);
    }

    private TranslationMemoryDTO convertToFullDTO(TranslationMemory tm){
        String ownerName = null;
        if (tm.getOwner() != null){
            String lastName = tm.getOwner().getLastName();
            String firstName = tm.getOwner().getFirstName();
            ownerName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            if (ownerName.isEmpty()) {
                ownerName = null;
            }
        }

        return new TranslationMemoryDTO(
            tm.getName(),
            tm.getCreatedBy(),
            tm.getCreateDate(),
            ownerName,
            tm.getSourceLang(),
            tm.getTargetLang(),
            tm.getSegments()
        );
    }
}
