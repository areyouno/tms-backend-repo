package com.tms.backend.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerifyRequest(
    @Email  @NotBlank String email,
    @Size(min = 6, max = 6) String code){}
