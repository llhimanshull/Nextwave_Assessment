package com.minijira.controller;

import com.minijira.dto.request.RoleUpdateRequest;
import com.minijira.dto.response.UserResponse;
import com.minijira.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Endpoints for managing users")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get all users", description = "Admin or Manager can fetch all users in their organization.")
    public ResponseEntity<java.util.List<UserResponse>> getUsers() {
        return ResponseEntity.ok(userService.getUsersByOrganization());
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Promote a user", description = "Admin can change roles of users.")
    public ResponseEntity<UserResponse> updateRole(
            @PathVariable UUID userId,
            @Valid @RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(userService.updateRole(userId, request));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user details", description = "Users can update their own details. Admins can update any user's details.")
    public ResponseEntity<UserResponse> updateUserDetails(
            @PathVariable UUID userId,
            @Valid @RequestBody com.minijira.dto.request.UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateUserDetails(userId, request));
    }
}
