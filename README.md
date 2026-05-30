<div align="center">
  <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" alt="Spring Boot"/>
  <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
  <img src="https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis"/>
  <img src="https://img.shields.io/badge/Docker-2CA5E0?style=for-the-badge&logo=docker&logoColor=white" alt="Docker"/>
  <img src="https://img.shields.io/badge/JWT-black?style=for-the-badge&logo=JSON%20web%20tokens" alt="JWT"/>
  
  <h1>🚀 MiniJira — Team Task Tracker API</h1>
  
  <p>A production-ready REST API built for modern, multi-tenant task management. Think a minimal Jira or Linear.</p>
</div>

<br/>

## ✨ Features
- 🔐 **Robust Auth:** JWT Access & Refresh Token rotation.
- 🏢 **Multi-Tenancy:** Isolated data per Organization.
- 🛡️ **Role-Based Access Control:** Strict ADMIN, MANAGER, and MEMBER permissions.
- ⚡ **High Performance:** Redis caching with intelligent assignee-based invalidation.
- 🐳 **DevOps Ready:** Multi-stage Docker builds and robust compose setup.
- 📖 **Self-Documenting:** Interactive OpenAPI / Swagger UI.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 |
| **Framework** | Spring Boot 3.4.5 |
| **Database** | PostgreSQL 16 (via Docker) |
| **Migrations** | Flyway |
| **Cache** | Redis 7 (via Docker) |
| **Auth** | JWT (Access + Refresh tokens via JJWT) |
| **API Docs** | SpringDoc OpenAPI / Swagger UI |

---

## 🚀 Running Locally

**Prerequisites:** Docker Desktop, Java 21, Maven

```bash
# 1. Spin up the entire stack (Postgres, Redis, and Spring Boot)
docker compose up -d --build
```

### 🌐 Endpoints
- **Frontend App:** `http://localhost:3000`
- **API Base URL:** `http://localhost:8080/api/v1`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`

### 👥 Sample Users & Roles
- **Admin User**: `admin@nextwave.com` / `password123`
- **Manager User**: `manager@nextwave.com` / `password123`
- **Member User**: `member@nextwave.com` / `password123`

> **Note for Reviewers:**
> - When registering a new account, users are granted the `MEMBER` role by default. To elevate their privileges, log in using the Admin account above and modify their role via the Admin dashboard.
> - For this assessment, the application is restricted to a **single organization**. The database and backend are fully designed for multi-tenancy, and multi-org support can easily be toggled on in the future.
> - The **Frontend React application** is partially connected to demonstrate the core user flows (Authentication, Project Boards, Task Management, Profile, and User Management). 
> - **The entire assessment is fully complete in the backend.** Since not all backend endpoints have an associated frontend view, please use the **Swagger UI** (`http://localhost:8080/swagger-ui.html`) or **OpenAPI JSON** to explore, send requests, and test the remaining endpoints!

---

## 🏗️ Schema Design Decisions

> This section explains **why** the database is structured the way it is — the reasoning a client or reviewer would ask about.

<details>
<summary><b>1. Multi-Tenant Design with <code>organizations</code> as the Root</b></summary>
<br>
Every user, project, and task traces back to a single `organizations` row.  
This is a <b>multi-tenant</b> architecture: one deployed instance of the API serves multiple independent companies (tenants), each fully isolated from each other at the data level.

<b>Why this matters:</b>
- You can onboard a new company by just inserting a row in `organizations` — no new database, no new deployment.
- All queries are automatically scoped by `organization_id`, so Company A can never accidentally see Company B's data.
</details>

<details>
<summary><b>2. UUIDs Instead of Auto-Increment Integer IDs</b></summary>
<br>
All primary keys use `UUID DEFAULT gen_random_uuid()`.

<b>Why not integers (<code>SERIAL</code>)?</b>
- Sequential IDs are easy to guess — attackers can enumerate records (`/tasks/1`, `/tasks/2`...).
- Merging data from two databases causes ID collisions. UUIDs are globally unique.
- Exposing `id=5` leaks business info (e.g. "you're our 5th customer").
</details>

<details>
<summary><b>3. <code>VARCHAR</code> + <code>CHECK</code> Constraints Instead of PostgreSQL ENUMs</b></summary>
<br>
<code>role</code>, <code>status</code>, and <code>priority</code> are all <code>VARCHAR</code> with a <code>CHECK</code> constraint.

<b>Why not ENUMs?</b>  
You cannot easily remove or rename a PostgreSQL ENUM value without complex workarounds. With `VARCHAR` + `CHECK`, adding or removing a value is just a single constraint update migration.
</details>

<details>
<summary><b>4. Refresh Tokens in PostgreSQL, Not Redis</b></summary>
<br>
- <b>Access tokens</b> (JWT, 15 minutes) — stateless, verified by signature.
- <b>Refresh tokens</b> (opaque string, 7 days) — stored in the `refresh_tokens` table in PostgreSQL.

<b>Why PostgreSQL?</b>
Redis is an in-memory cache and volatile. A restart would silently log out every user in the system. PostgreSQL gives us durability, auditability, and ACID atomic revocation.
</details>

<details>
<summary><b>5. Strategic Indexing</b></summary>
<br>
Three B-tree indexes are added on the `tasks` table (the most frequently queried table).
- `idx_tasks_status`: Supports the main board/backlog view.
- `idx_tasks_assignee_id`: Supports the "My Tasks" view (also the Redis cache key).
- `idx_tasks_due_date`: Efficient range queries for overdue tasks.

We followed the **YAGNI principle**. Indexes slow down writes, so we only indexed the most critical paths.
</details>

---

## ⚡ Caching Strategy

MiniJira utilizes **Spring Cache + Redis (Lettuce)** to optimize the `GET /api/v1/tasks` endpoint. Task lists are cached per assignee independently.

### 🔄 Invalidation Rules
- **TTL:** The Redis cache has a global TTL of **5 minutes** to ensure data doesn't remain stale indefinitely.
- **Eviction Triggers:** The cache for a specific user is immediately evicted when a task assigned to them is **created, updated, deleted, or undergoes a status transition**.
- **Assignee Change Nuance:** If a task's assignee is changed (e.g., from User A to User B), the system dynamically invalidates **both** User A's cache (so the task disappears from their list) and User B's cache (so the task appears on their list).

---

## 🔮 Future Improvements

- 📡 **Real-Time Notifications:** Add WebSockets or Server-Sent Events (SSE) for real-time task updates.
- 🔍 **Advanced Search:** Implement ElasticSearch for full-text task searching.
- 📊 **Analytics Endpoint:** Overdue task count per user and average completion time using SQL Window functions.
- 📧 **Email Integration:** Hook up Amazon SES or SendGrid for task assignment notifications.

<br/>
