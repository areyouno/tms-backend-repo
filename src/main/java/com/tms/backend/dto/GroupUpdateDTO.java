package com.tms.backend.dto;

import java.util.Set;

public record GroupUpdateDTO(
    String name,
    Long teamLeaderId,
    Set<Long> teamMemberIds,
    Set<Long> teamProjectIds,
    Boolean isGroupActive
) {}
