package com.tms.backend.dto;

public record CreateUserDTO(
    String firstName,
    String lastName,
    String email,
    String username,
    Long roleId,
    boolean isActive
) {}
