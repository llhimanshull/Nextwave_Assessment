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

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody TaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @GetMapping
    public ResponseEntity<org.springframework.data.domain.Page<TaskResponse>> getTasks(
            @RequestParam(required = false) com.minijira.enums.TaskStatus status,
            @RequestParam(required = false) com.minijira.enums.Priority priority,
            @RequestParam(required = false) java.util.UUID assigneeId,
            @org.springframework.data.web.PageableDefault(size = 20) org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasks(status, priority, assigneeId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @taskSecurity.isTaskAssignee(authentication, #id)")
    public ResponseEntity<TaskResponse> getTask(@PathVariable java.util.UUID id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @taskSecurity.isTaskAssignee(authentication, #id)")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable java.util.UUID id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deleteTask(@PathVariable java.util.UUID id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
