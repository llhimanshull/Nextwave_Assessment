-- ============================================================
-- V3__insert_sample_data.sql
-- INSERTS SAMPLE DATA FOR REVIEWERS TO TEST THE APPLICATION
-- AUTOMATICALLY ON STARTUP VIA FLYWAY
-- ============================================================

-- Use a hardcoded UUID for the organization so it's predictable for reviewers
INSERT INTO organizations (id, name, created_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'NextWave Assessment Org', now())
ON CONFLICT (id) DO NOTHING;

-- Insert sample users with roles (Password is 'password123' for all)
-- Hash: $2a$12$Z0s37B8O3G5lK5uQvUf8/.N79jR3rA0gX2wR1E/pW5ZtF0.u6mEWe
INSERT INTO users (id, organization_id, name, email, password_hash, role, created_at)
VALUES 
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'Alice Admin', 'admin@nextwave.com', '$2a$12$Z0s37B8O3G5lK5uQvUf8/.N79jR3rA0gX2wR1E/pW5ZtF0.u6mEWe', 'ADMIN', now()),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'Bob Manager', 'manager@nextwave.com', '$2a$12$Z0s37B8O3G5lK5uQvUf8/.N79jR3rA0gX2wR1E/pW5ZtF0.u6mEWe', 'MANAGER', now()),
    ('44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'Charlie Member', 'member@nextwave.com', '$2a$12$Z0s37B8O3G5lK5uQvUf8/.N79jR3rA0gX2wR1E/pW5ZtF0.u6mEWe', 'MEMBER', now())
ON CONFLICT (email) DO NOTHING;

-- Insert a sample project so the reviewer can immediately test task creation
INSERT INTO projects (id, organization_id, name, description, created_by, created_at)
VALUES ('55555555-5555-5555-5555-555555555555', '11111111-1111-1111-1111-111111111111', 'Initial Project', 'A sample project for testing tasks', '22222222-2222-2222-2222-222222222222', now())
ON CONFLICT (id) DO NOTHING;
