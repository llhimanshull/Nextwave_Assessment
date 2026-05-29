package com.minijira.security;

import com.minijira.entity.Task;
import com.minijira.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("taskSecurity")
@RequiredArgsConstructor
public class TaskSecurity {

    private final TaskRepository taskRepository;

    public boolean isTaskAssignee(Authentication authentication, UUID taskId) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return false;
        }

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        
        // Find task and check if it belongs to user's org and user is assignee
        return taskRepository.findById(taskId)
                .map(task -> task.getProject().getOrganization().getId().equals(user.getOrganizationId()) &&
                             task.getAssignee() != null && 
                             task.getAssignee().getId().equals(user.getId()))
                .orElse(false); // Return false on not found so @PreAuthorize throws 403, not 404
    }
}
