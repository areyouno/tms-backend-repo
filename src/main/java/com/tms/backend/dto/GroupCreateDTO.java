package com.tms.backend.dto;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GroupCreateDTO(
    @NotBlank String name,
    @NotNull Long teamLeaderId,
    Set<Long> teamMemberIds,
    Set<Long> teamProjectIds,
    Boolean isGroupActive
) {}
