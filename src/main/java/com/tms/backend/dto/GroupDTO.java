package com.tms.backend.dto;

import java.util.Set;
import java.util.stream.Collectors;

import com.tms.backend.group.Group;
import com.tms.backend.user.User;

public record GroupDTO(
    Long id,
    String name,
    ReferenceDTO teamLeader,
    Set<ReferenceDTO> teamMembers,
    Set<ReferenceDTO> teamProjects,
    boolean isGroupActive
) {
    private static String displayName(User user) {
        return user.isActive()
            ? (user.getFirstName() + " " + user.getLastName())
            : user.getLastName() + " (deleted user)";
    }

    public static GroupDTO fromEntity(Group group) {
        return new GroupDTO(
            group.getId(),
            group.getName(),
            new ReferenceDTO(group.getTeamLeader().getId(), displayName(group.getTeamLeader())),
            group.getTeamMembers().stream()
                .map(u -> new ReferenceDTO(u.getId(), displayName(u)))
                .collect(Collectors.toSet()),
            group.getTeamProjects().stream()
                .map(p -> new ReferenceDTO(p.getId(), p.getName()))
                .collect(Collectors.toSet()),
            group.isGroupActive()
        );
    }
}
