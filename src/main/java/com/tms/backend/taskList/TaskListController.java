package com.tms.backend.taskList;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.TaskListCreateDTO;
import com.tms.backend.dto.TaskListDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.security.AccessRolesConstants;
import com.tms.backend.user.CustomUserDetails;

@RestController
@RequestMapping("/api/task-lists")
public class TaskListController {

    private final TaskListService taskListService;

    public TaskListController(TaskListService taskListService) {
        this.taskListService = taskListService;
    }

    @PostMapping
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public ResponseEntity<TaskListDTO> createTaskList(
        @RequestBody TaskListCreateDTO createDTO,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskListDTO created = taskListService.createTaskList(createDTO, userDetails.getUid());
        return ResponseEntity.ok(created);
    }

    @GetMapping
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public List<TaskListDTO> getAllTaskLists() {
        return taskListService.getAllTaskLists();
    }

    @GetMapping("/{id}")
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public ResponseEntity<TaskListDTO> getTaskListById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(taskListService.getTaskListById(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/project/{projectId}")
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public List<TaskListDTO> getTaskListsByProject(@PathVariable Long projectId) {
        return taskListService.getTaskListsByProject(projectId);
    }

    @GetMapping("/assignee/{assigneeUid}")
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public List<TaskListDTO> getTaskListsByAssignee(@PathVariable String assigneeUid) {
        return taskListService.getTaskListsByAssignee(assigneeUid);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public ResponseEntity<Void> deleteTaskList(@PathVariable Long id) {
        try {
            taskListService.deleteTaskList(id);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
