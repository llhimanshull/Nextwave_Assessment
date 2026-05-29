package com.minijira.service;

import com.minijira.dto.request.TaskRequest;
import com.minijira.dto.response.TaskResponse;
import com.minijira.entity.Project;
import com.minijira.entity.Task;
import com.minijira.entity.User;
import com.minijira.exception.ResourceNotFoundException;
import com.minijira.repository.ProjectRepository;
import com.minijira.repository.TaskRepository;
import com.minijira.repository.UserRepository;
import com.minijira.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(TaskRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();

        // Validate project belongs to the same organization
        Project project = projectRepository.findByIdAndOrganizationId(request.getProjectId(), currentOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", request.getProjectId()));

        // Fetch current user (creator)
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        // Validate assignee belongs to the same organization if provided
        User assignee = null;
        if (request.getAssigneeId() != null) {
            assignee = userRepository.findById(request.getAssigneeId())
                    .filter(u -> u.getOrganization().getId().equals(currentOrgId))
                    .orElseThrow(() -> new IllegalArgumentException("Assignee not found or does not belong to your organization"));
        }

        Task task = Task.builder()
                .project(project)
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .status(request.getStatus() != null ? request.getStatus() : com.minijira.enums.TaskStatus.TODO)
                .assignee(assignee)
                .createdBy(creator)
                .dueDate(request.getDueDate())
                .build();

        Task savedTask = taskRepository.save(task);

        return mapToResponse(savedTask);
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProject().getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .priority(task.getPriority())
                .status(task.getStatus())
                .assigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null)
                .createdById(task.getCreatedBy().getId())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}
