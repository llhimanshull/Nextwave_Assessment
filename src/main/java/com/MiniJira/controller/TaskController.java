package com.minijira.controller;

import com.minijira.dto.request.TaskRequest;
import com.minijira.dto.response.TaskResponse;
import com.minijira.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Endpoints for managing tasks")
@SecurityRequirement(name = "Bearer Authentication")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a task", description = "Admin or Manager only. Assignee must be in the same organization.")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @GetMapping
    @Operation(summary = "Get tasks", description = "Paginated and filterable. Members only see their own tasks.")
    public ResponseEntity<org.springframework.data.domain.Page<TaskResponse>> getTasks(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) com.minijira.enums.TaskStatus status,
            @RequestParam(required = false) com.minijira.enums.Priority priority,
            @RequestParam(required = false) UUID assigneeId,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasks(projectId, status, priority, assigneeId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@taskSecurity.isTaskAssignee(authentication, #id) or hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get a task by ID", description = "Members can only view their own tasks.")
    public ResponseEntity<TaskResponse> getTask(@PathVariable UUID id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("@taskSecurity.isTaskAssignee(authentication, #id) or hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a task", description = "Members can only update their own tasks. Status transitions are validated.")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a task", description = "Admin or Manager only.")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
