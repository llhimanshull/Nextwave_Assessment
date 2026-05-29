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
import com.minijira.security.CustomUserDetails;
import com.minijira.security.SecurityUtils;
import com.minijira.validation.TaskStatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskStatusTransitionValidator transitionValidator;
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(value = "tasks", key = "#request.assigneeId != null ? #request.assigneeId.toString() : 'UNASSIGNED'")
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
                    .orElseThrow(() -> new com.minijira.exception.BusinessException(
                            com.minijira.exception.ErrorCode.VALIDATION_ERROR.name(),
                            "Assignee not found or does not belong to your organization",
                            org.springframework.http.HttpStatus.BAD_REQUEST));
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

    @Transactional(readOnly = true)
    @Cacheable(value = "tasks", key = "#assigneeId != null ? #assigneeId.toString() : 'UNASSIGNED'")
    public org.springframework.data.domain.Page<TaskResponse> getTasks(
            com.minijira.enums.TaskStatus status,
            com.minijira.enums.Priority priority,
            UUID assigneeId,
            org.springframework.data.domain.Pageable pageable) {

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        com.minijira.security.CustomUserDetails currentUser = SecurityUtils.getCurrentUser();

        // If user is MEMBER, force assigneeId to themselves
        boolean isMember = currentUser != null && currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER"));
        
        UUID finalAssigneeId = isMember ? currentUserId : assigneeId;

        org.springframework.data.jpa.domain.Specification<Task> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            // Must belong to the user's organization
            predicates.add(cb.equal(root.get("project").get("organization").get("id"), currentOrgId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (finalAssigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), finalAssigneeId));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return taskRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        // The @PreAuthorize on controller checks if it's the member's task, 
        // but we still need to ensure ADMIN/MANAGER can only see tasks in their org.
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        if (!task.getProject().getOrganization().getId().equals(currentOrgId)) {
            throw new org.springframework.security.access.AccessDeniedException("Task not found in your organization");
        }

        return mapToResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(UUID id, TaskRequest request) {
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        // Ensure task belongs to user's org
        if (!task.getProject().getOrganization().getId().equals(currentOrgId)) {
            throw new org.springframework.security.access.AccessDeniedException("Task not found in your organization");
        }

        // Validate assignee if changed
        User assignee = task.getAssignee();
        UUID oldAssigneeId = assignee != null ? assignee.getId() : null;
        
        if (request.getAssigneeId() != null) {
            if (assignee == null || !assignee.getId().equals(request.getAssigneeId())) {
                assignee = userRepository.findById(request.getAssigneeId())
                        .filter(u -> u.getOrganization().getId().equals(currentOrgId))
                        .orElseThrow(() -> new com.minijira.exception.BusinessException(
                                com.minijira.exception.ErrorCode.VALIDATION_ERROR.name(),
                                "Assignee not found or does not belong to your organization",
                                org.springframework.http.HttpStatus.BAD_REQUEST));
            }
        } else {
            assignee = null;
        }

        if (request.getStatus() != null && request.getStatus() != task.getStatus()) {
            // Validate the transition logic
            transitionValidator.validateTransition(task.getStatus(), request.getStatus());

            // Specific Rule: BLOCKED reachable from TODO, IN_PROGRESS, IN_REVIEW — only ASSIGNEE or MANAGER can transition
            if (request.getStatus() == com.minijira.enums.TaskStatus.BLOCKED) {
                CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
                boolean isManager = currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
                boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(currentUser.getId());
                
                if (!isManager && !isAssignee) {
                    throw new org.springframework.security.access.AccessDeniedException("Only the assignee or a MANAGER can transition a task to BLOCKED");
                }
            }
            task.setStatus(request.getStatus());
        }

        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        task.setPriority(request.getPriority());
        task.setAssignee(assignee);
        task.setDueDate(request.getDueDate());

        // Cache eviction for both old and new assignee
        org.springframework.cache.Cache cache = cacheManager.getCache("tasks");
        if (cache != null) {
            String oldAssigneeKey = oldAssigneeId != null ? oldAssigneeId.toString() : "UNASSIGNED";
            String newAssigneeKey = request.getAssigneeId() != null ? request.getAssigneeId().toString() : "UNASSIGNED";
            
            cache.evict(oldAssigneeKey);
            if (!oldAssigneeKey.equals(newAssigneeKey)) {
                cache.evict(newAssigneeKey);
            }
        }

        return mapToResponse(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(UUID id) {
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        // Ensure task belongs to user's org
        if (!task.getProject().getOrganization().getId().equals(currentOrgId)) {
            throw new org.springframework.security.access.AccessDeniedException("Task not found in your organization");
        }

        UUID assigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        taskRepository.delete(task);

        org.springframework.cache.Cache cache = cacheManager.getCache("tasks");
        if (cache != null) {
            cache.evict(assigneeId != null ? assigneeId.toString() : "UNASSIGNED");
        }
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
