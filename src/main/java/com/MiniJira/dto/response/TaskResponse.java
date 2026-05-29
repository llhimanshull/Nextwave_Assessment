package com.minijira.dto.response;

import com.minijira.enums.Priority;
import com.minijira.enums.TaskStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TaskResponse implements java.io.Serializable {
    private UUID id;
    private UUID projectId;
    private String title;
    private String description;
    private Priority priority;
    private TaskStatus status;
    private UUID assigneeId;
    private UUID createdById;
    private LocalDate dueDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
