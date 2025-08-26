package com.tms.backend.dto;


public record UserDTO(
    String uid,
    String email,
    String firstName,
    boolean isVerified,
    boolean isProfileComplete
) {}
