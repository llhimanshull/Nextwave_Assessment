package com.minijira.service;

import com.minijira.dto.request.ProjectRequest;
import com.minijira.dto.response.ProjectResponse;
import com.minijira.entity.Organization;
import com.minijira.entity.Project;
import com.minijira.entity.User;
import com.minijira.exception.ResourceNotFoundException;
import com.minijira.repository.OrganizationRepository;
import com.minijira.repository.ProjectRepository;
import com.minijira.repository.UserRepository;
import com.minijira.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    @Transactional
    public ProjectResponse createProject(ProjectRequest request) {
        UUID currentUserId = SecurityUtils.getCurrentUserId();
        UUID currentOrgId = SecurityUtils.getCurrentOrganizationId();

        // 1. Fetch organization of the current user
        Organization organization = organizationRepository.findById(currentOrgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", currentOrgId));

        // 2. Fetch the current user (creator)
        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        // 3. Create project (tied to the same organization)
        Project project = Project.builder()
                .organization(organization)
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .build();

        Project savedProject = projectRepository.save(project);

        return ProjectResponse.builder()
                .id(savedProject.getId())
                .organizationId(savedProject.getOrganization().getId())
                .name(savedProject.getName())
                .description(savedProject.getDescription())
                .createdById(savedProject.getCreatedBy().getId())
                .createdAt(savedProject.getCreatedAt())
                .build();
    }
}
