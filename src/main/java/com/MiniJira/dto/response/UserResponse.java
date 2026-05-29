package com.minijira.dto.response;

import com.minijira.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private Role role;
    private UUID organizationId;
    private LocalDateTime createdAt;
}
