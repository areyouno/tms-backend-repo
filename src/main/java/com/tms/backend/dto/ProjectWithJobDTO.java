package com.tms.backend.dto;

import java.util.List;

public record ProjectWithJobDTO(
    ProjectDTO project,
    List<JobDTO> jobs
) {}
