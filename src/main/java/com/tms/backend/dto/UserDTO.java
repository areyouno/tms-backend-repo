package com.tms.backend.dto;

import java.util.Set;

public record UserDTO(
    Long id,
    String uid,
    String email,
    String firstName,
    String lastName,
    String country,
    boolean isVerified,
    boolean isProfileComplete,
    String referralSource,
    String organizationName,
    String organizationSize,
    String username,
    boolean isActive,
    ReferenceDTO role,
    Set<ReferenceDTO> groups
) {}
