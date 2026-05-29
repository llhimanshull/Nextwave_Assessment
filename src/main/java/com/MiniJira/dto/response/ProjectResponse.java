package com.minijira.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ProjectResponse {
    private UUID id;
    private UUID organizationId;
    private String name;
    private String description;
    private UUID createdById;
    private LocalDateTime createdAt;
}
