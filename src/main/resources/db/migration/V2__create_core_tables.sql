-- ============================================================
-- V2__create_core_tables.sql
-- CREATES THE 5 CORE TABLES FOR MINIJIRA:
--   organizations, users, projects, tasks, refresh_tokens
-- ALSO CREATES 3 INDEXES ON THE tasks TABLE FOR QUERY PERFORMANCE
-- ============================================================


-- ==================== ORGANIZATIONS ====================
-- This is the top-level "tenant" table in a multi-tenant design.
-- Every user, project, and task is scoped to exactly one organization.
-- Why UUID? UUIDs are safe to expose in APIs (no sequential enumeration
-- attacks like with integer IDs), globally unique across services, and
-- make future data sharding or merging straightforward.
-- Why gen_random_uuid()? It's built into PostgreSQL 13+ — no extension
-- (like pgcrypto) needed. It generates cryptographically random v4 UUIDs.
CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    -- created_at uses DEFAULT now() so the DB always sets it — no risk of
    -- the application layer forgetting to populate it.
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);


-- ==================== USERS ====================
-- Users always belong to exactly one organization (organization_id is NOT NULL).
-- This enforces the multi-tenant boundary at the database level, not just app level.
-- Why REFERENCES organizations(id)? Foreign keys give us referential integrity
-- guaranteed by the DB — even if a bug in application code tries to create an
-- orphaned user, the DB rejects it.
-- Why VARCHAR(20) CHECK for role instead of a PostgreSQL ENUM?
-- PostgreSQL ENUMs are painful to modify — adding a new role later requires
-- an ALTER TYPE statement which can lock the table and is hard to roll back.
-- A CHECK constraint on a VARCHAR is trivially changed in a migration by
-- just dropping and re-adding the constraint. No table lock, no drama.
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    -- email is UNIQUE because it's used as the login identifier.
    -- The UNIQUE constraint also implicitly creates a B-tree index on email,
    -- so lookups by email (e.g. login) are fast without a separate index.
    email           VARCHAR(255) NOT NULL UNIQUE,
    -- We store the HASH, never the plain-text password. The application layer
    -- (BCrypt) is responsible for hashing before insert and comparison on login.
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'MEMBER')),
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);


-- ==================== PROJECTS ====================
-- Projects are scoped to an organization and created by a user.
-- created_by is a hard reference (NOT NULL) — every project must have an owner.
-- We intentionally do NOT add updated_at here because projects are relatively
-- static entities (name/description rarely change). Tracking mutations at the
-- project level adds overhead with little benefit for this use case.
CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    name            VARCHAR(255) NOT NULL,
    -- description is nullable TEXT — some projects may have no description yet.
    description     TEXT,
    created_by      UUID         NOT NULL REFERENCES users(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);


-- ==================== TASKS ====================
-- Tasks are the core entity of the system. They belong to a project and
-- can be assigned to a user (nullable — unassigned tasks are valid).
-- Why updated_at HERE but not on projects/organizations?
-- Tasks change state frequently: status moves, reassignments, priority tweaks.
-- updated_at is critical for two reasons:
--   1. Cache invalidation: the Redis cache for "tasks by assignee" must be
--      invalidated when a task is mutated. updated_at is the mutation signal.
--   2. Optimistic concurrency: clients can use updated_at to detect stale reads.
-- Why VARCHAR CHECK for priority and status instead of ENUMs? Same reason as
-- role on users — easier to evolve via migrations without table locks.
-- DEFAULT 'TODO' for status means newly created tasks start in the backlog
-- without the application needing to explicitly set it.
CREATE TABLE tasks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id  UUID         NOT NULL REFERENCES projects(id),
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    priority    VARCHAR(10)  NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    status      VARCHAR(15)  NOT NULL DEFAULT 'TODO'
                    CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE', 'BLOCKED')),
    -- assignee_id is nullable: a task can exist without being assigned to anyone yet.
    assignee_id UUID REFERENCES users(id),
    created_by  UUID         NOT NULL REFERENCES users(id),
    -- due_date is DATE (not TIMESTAMP) — task deadlines are day-level granularity,
    -- not time-level. Using DATE avoids timezone confusion entirely.
    due_date    DATE,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);


-- ==================== REFRESH TOKENS ====================
-- Why store refresh tokens in PostgreSQL instead of Redis?
-- Short answer: refresh tokens are long-lived (7 days) and need ACID guarantees.
-- Redis is in-memory and volatile — a restart or eviction would silently invalidate
-- all active sessions, logging out every user. That's unacceptable.
-- PostgreSQL gives us:
--   1. Durability: tokens survive Redis restarts.
--   2. Auditability: the revoked column lets us soft-delete tokens and keep an
--      audit trail (e.g. "when was this session revoked and why?").
--   3. ACID revocation: revoking a token is a single atomic UPDATE — no risk of
--      partial state if the app crashes mid-operation.
-- Access tokens (short-lived, 15 min) DO live in Redis because they can be
-- safely re-issued and losing them on a Redis restart is acceptable.
-- token is VARCHAR(512) UNIQUE — the application generates a cryptographically
-- random token; UNIQUE ensures no accidental collision can grant two sessions.
CREATE TABLE refresh_tokens (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL REFERENCES users(id),
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMP    NOT NULL,
    -- revoked defaults to false. When a user logs out or we detect token reuse,
    -- we flip this to true (soft delete) rather than hard-deleting. This preserves
    -- the audit trail and prevents replay attacks during the race window.
    revoked     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMP    NOT NULL DEFAULT now()
);


-- ==================== INDEXES ====================
-- We add 3 targeted indexes on tasks — the most frequently queried table.
-- We intentionally skip indexes on other tables for now (YAGNI principle):
-- add indexes when query patterns are proven, not speculatively.

-- INDEX 1: tasks by status
-- Query pattern: "Show me all TODO tasks in this project" / "Show all BLOCKED tasks"
-- This is the most common filter in any task tracker UI. Without this index,
-- Postgres would do a full table scan across all tasks in the system.
CREATE INDEX idx_tasks_status ON tasks (status);

-- INDEX 2: tasks by assignee
-- Query pattern: "Show me all tasks assigned to user X" (the My Tasks view).
-- This is also the Redis cache key — when tasks are cached per assignee,
-- this index backs the DB query that populates the cache on a cache miss.
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);

-- INDEX 3: tasks by due_date
-- Query pattern: "Show overdue tasks" / "Show tasks due this week"
-- Range queries on due_date (e.g. WHERE due_date < now()) benefit greatly
-- from a B-tree index since Postgres can do an index range scan instead of
-- scanning every row and evaluating the condition.
CREATE INDEX idx_tasks_due_date ON tasks (due_date);
