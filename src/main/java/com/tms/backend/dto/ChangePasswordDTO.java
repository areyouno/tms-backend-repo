package com.tms.backend.dto;

public record ChangePasswordDTO(
    String currentPassword,
    String newPassword
) {}
