package com.tms.backend.taskList;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tms.backend.dto.PagedResponseDTO;
import com.tms.backend.dto.TaskListCreateDTO;
import com.tms.backend.dto.TaskListDTO;
import com.tms.backend.dto.TaskListSummaryDTO;
import com.tms.backend.exception.ResourceNotFoundException;
import com.tms.backend.security.AccessRolesConstants;
import com.tms.backend.user.CustomUserDetails;
import com.tms.backend.user.User;
import com.tms.backend.user.UserService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/task-lists")
public class TaskListController {

    private final TaskListService taskListService;
    private final UserService userService;

    public TaskListController(TaskListService taskListService, UserService userService) {
        this.taskListService = taskListService;
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public ResponseEntity<TaskListDTO> createTaskList(
        @RequestBody TaskListCreateDTO createDTO,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        TaskListDTO created = taskListService.createTaskList(createDTO, userDetails.getUid());
        return ResponseEntity.ok(created);
    }

    // Paginated/filtered/sorted/searchable task-list listing. `filters` mirrors the sidebar's
    // activeFilters shape ({ [filterName]: string[] }, JSON-encoded), same convention as
    // GET /api/jobs and GET /api/projects.
    @GetMapping
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public PagedResponseDTO<TaskListSummaryDTO> getAllTaskLists(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir,
        @RequestParam(required = false) String filters,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        User currentUser = userService.findByUid(userDetails.getUid())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with uid: " + userDetails.getUid()));
        Map<String, List<String>> parsedFilters = parseFilters(filters);
        return PagedResponseDTO.from(taskListService.getAllTaskLists(
            currentUser, page, pageSize, search, sortBy, sortDir, parsedFilters));
    }

    private Map<String, List<String>> parseFilters(String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(filtersJson, new TypeReference<Map<String, List<String>>>() {});
        } catch (JsonProcessingException e) {
            return null;
        }
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

    @GetMapping("/assignee/{assigneeUid}")
    @PreAuthorize(AccessRolesConstants.AUTHENTICATED)
    public List<TaskListSummaryDTO> getTaskListsByAssignee(@PathVariable String assigneeUid) {
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
