package com.minijira.service;

import com.minijira.dto.request.RoleUpdateRequest;
import com.minijira.dto.response.UserResponse;
import com.minijira.entity.User;
import com.minijira.enums.Role;
import com.minijira.exception.ResourceNotFoundException;
import com.minijira.repository.UserRepository;
import com.minijira.security.CustomUserDetails;
import com.minijira.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse updateRole(UUID targetUserId, RoleUpdateRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("User not authenticated");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        // 1. Must be in the same organization
        if (!targetUser.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new AccessDeniedException("You do not have permission to manage this user");
        }

        String currentRole = currentUser.getAuthorities().iterator().next().getAuthority(); // E.g. "ROLE_ADMIN" or "ROLE_MANAGER"
        Role newRole = request.getRole();

        // 2. Evaluate permission logic based on roles
        if (currentRole.equals("ROLE_MANAGER")) {
            // Manager can only promote to MANAGER
            if (newRole == Role.ADMIN) {
                throw new AccessDeniedException("Managers cannot promote users to ADMIN");
            }
            // Manager can only promote a MEMBER
            if (targetUser.getRole() == Role.ADMIN) {
                throw new AccessDeniedException("Managers cannot change the role of an ADMIN");
            }
        } else if (!currentRole.equals("ROLE_ADMIN")) {
            // Just in case a MEMBER somehow gets here
            throw new AccessDeniedException("Members cannot change roles");
        }
        // If ADMIN, they can promote anyone to ADMIN or MANAGER.

        // Apply update
        targetUser.setRole(newRole);
        User savedUser = userRepository.save(targetUser);

        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .organizationId(savedUser.getOrganization().getId())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }
}
