package com.tms.backend.dto;

import java.time.ZoneId;
import java.util.Set;

public record CreateUserDTO(
    String firstName,
    String lastName,
    String email,
    String username,
    Long roleId,
    boolean isActive,

    ZoneId timeZone,
    String note,

    //optional fields
    String sourceLang,
    Set<String> targetLanguages
) {}
