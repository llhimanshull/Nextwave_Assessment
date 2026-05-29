package com.minijira.repository;

import com.minijira.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByIdAndOrganizationId(UUID id, UUID organizationId);
    Page<Project> findAllByOrganizationId(UUID organizationId, Pageable pageable);
    List<Project> findAllByOrganizationId(UUID organizationId);
}
