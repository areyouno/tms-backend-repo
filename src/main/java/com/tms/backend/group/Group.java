package com.tms.backend.group;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tms.backend.project.Project;
import com.tms.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "groups_mgmt")
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_leader_id", nullable = false)
    @JsonIgnore
    private User teamLeader;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_members",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnore
    private Set<User> teamMembers = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "group_projects",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "project_id")
    )
    @JsonIgnore
    private Set<Project> teamProjects = new HashSet<>();

    private boolean isGroupActive = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public User getTeamLeader() { return teamLeader; }
    public void setTeamLeader(User teamLeader) { this.teamLeader = teamLeader; }

    public Set<User> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(Set<User> teamMembers) { this.teamMembers = teamMembers; }

    public Set<Project> getTeamProjects() { return teamProjects; }
    public void setTeamProjects(Set<Project> teamProjects) { this.teamProjects = teamProjects; }

    public boolean isGroupActive() { return isGroupActive; }
    public void setGroupActive(boolean isGroupActive) { this.isGroupActive = isGroupActive; }
}
