package com.tms.backend.dto;

import java.util.List;

public record ProjectTbAssignmentRequest(
    List<ProjectTbAssignmentDTO> tbAssignments
) {}
