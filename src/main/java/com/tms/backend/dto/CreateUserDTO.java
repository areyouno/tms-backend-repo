package com.tms.backend.dto;

import java.util.Set;

public record CreateUserDTO(
    String firstName,
    String lastName,
    String email,
    String username,
    Long roleId,
    boolean isActive,

    String organization,
    String country,

    //optional fields
    String sourceLang,
    Set<String> targetLanguages
) {}
