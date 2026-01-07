package com.tms.backend.dto;

public record UpdateUserDTO(
    String firstName,
    String lastName,
    String country,
    String email,
    Boolean isVerified,
    
     // for signup completion
    String organizationName,
    String organizationSize,
    Long roleId,
    String referralSource,
    Boolean agreedToTerms,
    Boolean isProfileComplete
) {}
