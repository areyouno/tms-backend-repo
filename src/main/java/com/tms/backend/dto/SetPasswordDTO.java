package com.tms.backend.dto;

public record SetPasswordDTO(
    String token,
    String newPassword
) {}
