package com.tms.backend.dto;

public record ResetPasswordDTO(
    String token,
    String newPassword
) {}
