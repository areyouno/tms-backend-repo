package com.tms.backend.dto;

import java.time.LocalDateTime;

public record JobCheckoutStatusDTO(
    String checkoutUserId,
    String checkoutUserName,
    LocalDateTime checkoutAt,
    LocalDateTime fileUpdatedAt,
    LocalDateTime lastSavedAt,
    LocalDateTime expiresAt
)
{}
