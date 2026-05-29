package com.minijira.dto.request;

import com.minijira.enums.Priority;
import com.minijira.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class TaskRequest {

    @NotNull(message = "Project ID is required")
    private UUID projectId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Priority is required")
    private Priority priority;

    // Status is optional on creation, defaults to TODO in entity
    private TaskStatus status;

    private UUID assigneeId;

    @jakarta.validation.constraints.Future(message = "Due date must be in the future")
    private LocalDate dueDate;
}
