package com.tms.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginDTO(
    String email,
    String firstName,
    Boolean isVerified,
    Boolean isProfileComplete,
    String token
) {}
