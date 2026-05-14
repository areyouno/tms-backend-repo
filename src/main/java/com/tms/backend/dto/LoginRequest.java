package com.tms.backend.dto;

public record LoginRequest(
    String email,
    String password
) {}
