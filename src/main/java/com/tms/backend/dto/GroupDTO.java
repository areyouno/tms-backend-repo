package com.tms.backend.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.tms.backend.group.Group;

public record GroupDTO(
    Long id,
    String name,
    ReferenceDTO teamLeader,
    Set<ReferenceDTO> teamMembers,
    Set<ReferenceDTO> teamProjects,
    boolean isGroupActive
) {
    public static GroupDTO fromEntity(Group group) {
        return new GroupDTO(
            group.getId(),
            group.getName(),
            new ReferenceDTO(group.getTeamLeader().getId(), group.getTeamLeader().getFirstName() + " " + group.getTeamLeader().getLastName()),
            group.getTeamMembers().stream()
                .map(u -> new ReferenceDTO(u.getId(), u.getFirstName() + " " + u.getLastName()))
                .collect(Collectors.toSet()),
            group.getTeamProjects().stream()
                .map(p -> new ReferenceDTO(p.getId(), p.getName()))
                .collect(Collectors.toSet()),
            group.isGroupActive()
        );
    }
}
