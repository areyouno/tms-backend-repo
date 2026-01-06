package com.tms.backend.dto;

import java.util.List;

public record ProjectTmAssignmentRequest(
    List<ProjectTmAssignmentDTO> tmAssignments
) {}
