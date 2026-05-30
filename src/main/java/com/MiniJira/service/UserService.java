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

        String currentRole = currentUser.getAuthorities().iterator().next().getAuthority();

        if (!currentRole.equals("ROLE_ADMIN")) {
            throw new AccessDeniedException("Only ADMINs can change roles");
        }

        targetUser.setRole(request.getRole());
        User savedUser = userRepository.save(targetUser);

        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUserDetails(UUID targetUserId, com.minijira.dto.request.UserUpdateRequest request) {
        CustomUserDetails currentUser = SecurityUtils.getCurrentUser();
        if (currentUser == null) {
            throw new AccessDeniedException("User not authenticated");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        if (!targetUser.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new AccessDeniedException("You do not have permission to manage this user");
        }

        String currentRole = currentUser.getAuthorities().iterator().next().getAuthority();
        boolean isSelf = targetUser.getId().equals(currentUser.getId());

        if (!isSelf && !currentRole.equals("ROLE_ADMIN")) {
            throw new AccessDeniedException("You can only modify your own details, unless you are an ADMIN");
        }

        // Validate email uniqueness if changed
        if (!targetUser.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new com.minijira.exception.BusinessException(
                        com.minijira.exception.ErrorCode.VALIDATION_ERROR.name(),
                        "Email is already in use",
                        org.springframework.http.HttpStatus.BAD_REQUEST);
            }
        }

        targetUser.setName(request.getName());
        targetUser.setEmail(request.getEmail());

        User savedUser = userRepository.save(targetUser);
        return mapToResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public java.util.List<UserResponse> getUsersByOrganization() {
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();
        return userRepository.findByOrganizationId(currentOrgId).stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .organizationId(user.getOrganization().getId())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
