package com.minijira.validation;

import com.minijira.enums.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class TaskStatusTransitionValidator {

    private final Map<TaskStatus, Set<TaskStatus>> validTransitions = Map.of(
            TaskStatus.TODO, Set.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED),
            TaskStatus.IN_PROGRESS, Set.of(TaskStatus.IN_REVIEW, TaskStatus.BLOCKED),
            TaskStatus.IN_REVIEW, Set.of(TaskStatus.DONE, TaskStatus.BLOCKED),
            TaskStatus.BLOCKED, Set.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS),
            TaskStatus.DONE, Set.of() // Terminal state
    );

    public void validateTransition(TaskStatus currentStatus, TaskStatus newStatus) {
        if (currentStatus == newStatus) {
            return; // No transition
        }
        
        Set<TaskStatus> allowed = validTransitions.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new com.minijira.exception.BusinessException(
                    com.minijira.exception.ErrorCode.INVALID_TRANSITION.name(),
                    String.format("Invalid status transition from %s to %s", currentStatus, newStatus),
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }
}
