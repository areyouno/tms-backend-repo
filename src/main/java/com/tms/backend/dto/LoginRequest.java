package com.tms.backend.dto;

public record LoginRequest(
    String identifier,
    String password
) {}
