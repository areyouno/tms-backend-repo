package com.tms.backend.group;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tms.backend.dto.GroupCreateDTO;
import com.tms.backend.dto.GroupDTO;
import com.tms.backend.dto.GroupUpdateDTO;
import com.tms.backend.project.Project;
import com.tms.backend.project.ProjectRepository;
import com.tms.backend.user.User;
import com.tms.backend.user.UserRepository;

@Service
public class GroupService {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Transactional
    public GroupDTO createGroup(GroupCreateDTO dto) {
        if (groupRepository.existsByName(dto.name())) {
            throw new RuntimeException("Group name already exists: " + dto.name());
        }

        User teamLeader = userRepository.findById(dto.teamLeaderId())
                .orElseThrow(() -> new RuntimeException("Team leader not found with id: " + dto.teamLeaderId()));

        Group group = new Group();
        group.setName(dto.name());
        group.setTeamLeader(teamLeader);
        group.setGroupActive(dto.isGroupActive() != null ? dto.isGroupActive() : true);

        if (dto.teamMemberIds() != null && !dto.teamMemberIds().isEmpty()) {
            Set<User> members = new HashSet<>(userRepository.findAllById(dto.teamMemberIds()));
            group.setTeamMembers(members);
        }

        if (dto.teamProjectIds() != null && !dto.teamProjectIds().isEmpty()) {
            Set<Project> projects = new HashSet<>(projectRepository.findAllById(dto.teamProjectIds()));
            group.setTeamProjects(projects);
        }

        Group saved = groupRepository.save(group);
        return GroupDTO.fromEntity(saved);
    }

    public List<GroupDTO> getAllGroups() {
        return groupRepository.findAllWithDetails().stream()
                .map(GroupDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<GroupDTO> getAllActiveGroups() {
        return groupRepository.findAllActiveWithDetails().stream()
                .map(GroupDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public GroupDTO getGroupById(Long id) {
        Group group = groupRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));
        return GroupDTO.fromEntity(group);
    }

    @Transactional
    public GroupDTO updateGroup(Long id, GroupUpdateDTO dto) {
        Group group = groupRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Group not found with id: " + id));

        if (dto.name() != null) {
            if (!dto.name().equals(group.getName()) && groupRepository.existsByName(dto.name())) {
                throw new RuntimeException("Group name already exists: " + dto.name());
            }
            group.setName(dto.name());
        }

        if (dto.teamLeaderId() != null) {
            User teamLeader = userRepository.findById(dto.teamLeaderId())
                    .orElseThrow(() -> new RuntimeException("Team leader not found with id: " + dto.teamLeaderId()));
            group.setTeamLeader(teamLeader);
        }

        if (dto.teamMemberIds() != null) {
            Set<User> members = new HashSet<>(userRepository.findAllById(dto.teamMemberIds()));
            group.setTeamMembers(members);
        }

        if (dto.teamProjectIds() != null) {
            Set<Project> projects = new HashSet<>(projectRepository.findAllById(dto.teamProjectIds()));
            group.setTeamProjects(projects);
        }

        if (dto.isGroupActive() != null) {
            group.setGroupActive(dto.isGroupActive());
        }

        Group saved = groupRepository.save(group);
        return GroupDTO.fromEntity(saved);
    }

    @Transactional
    public void deleteGroup(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new RuntimeException("Group not found with id: " + id);
        }
        groupRepository.deleteById(id);
    }
}
