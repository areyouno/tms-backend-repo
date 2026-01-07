package com.tms.backend.dto;

import java.time.ZoneId;
import java.util.Set;

public record UpdateUserByIdDTO(
    Long userId,
    String firstName,
    String lastName,
    String email,
    String username,
    Long roleId,
    ZoneId timeZone,
    Boolean isActive,
    String note,
    String sourceLang,
    Set<String> targetLanguages
) {}
