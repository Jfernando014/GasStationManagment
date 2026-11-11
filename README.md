<div align="center">

# ⛽ Gas Station Management System (EDS Management)

**Enterprise Full-Stack Platform for Service Station Operations, Daily Cash Reconciliation, Inventory Control & Shift Management**

[![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Modulith](https://img.shields.io/badge/Spring_Modulith-1.1.3-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-modulith)
[![Angular](https://img.shields.io/badge/Angular-21_PWA-DD0031?style=flat-square&logo=angular&logoColor=white)](https://angular.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## 📖 Overview

**EDS Management** is a full-stack web application designed to digitize, audit, and automate daily operations at fuel service stations (EDS). Built on a **Spring Modulith** backend and an **Angular 21 Progressive Web App (PWA)** frontend, the platform enforces strict domain boundaries, real-time financial reconciliation, and offline-ready operational workflows—replacing manual spreadsheet processes (`CONSOLIDADO`, `VARIACIÓN`, and `COMPLEMENTO DOMINUS`) with a guided, traceable pipeline.

---

## 🏗️ Technology Stack

| Layer | Technologies | Architectural Pattern |
| :--- | :--- | :--- |
| **Backend** | Java 21 LTS, Spring Boot 3.2.4, Spring Modulith 1.1.3, Spring Security (JWT), Spring Data JPA, MapStruct, Flyway, OpenAPI 3 / Swagger | **Modular Monolith + In-Process Event Bus** |
| **Frontend** | Angular 21, TypeScript, RxJS, Service Worker (PWA), ExcelJS, PptxGenJS, Vitest | **Feature-Driven Modules + Atomic Design** |
| **Database** | PostgreSQL 16 (`postgres:16-alpine`), Flyway Versioned Migrations (`V1`–`V34`) | **Relational Schema with Strictly Scoped Domain Tables** |
| **DevOps & QA** | Docker, Docker Compose (`dev`, `ci`, `prod`), Nginx, Testcontainers, JUnit 5, GitHub Actions | **Containerized Multi-Environment CI/CD** |

---

## 🏛️ System Architecture

### Modular Monolith & Inter-Module Communication

The backend is structured with **Spring Modulith** into 8 self-contained business domains. Modules **never** access each other's persistence repositories or JPA entities directly. Cross-domain collaboration is strictly governed by:
1. **Explicit Application Query Contracts (`*Query`)**: Read-only ports exposed by provider modules to supply domain data without leaking internal persistence models.
2. **Transactional Domain Events (`Spring Modulith Event Publication`)**: Backed by the `event_publication` outbox table (`republish-outstanding-events-on-restart=true`) for guaranteed event delivery across module boundaries.

| Contract / Event | Provider Module | Consumer Module(s) | Purpose |
| :--- | :--- | :--- | :--- |
| **`dayStaffQuery`** | `shifts` | `sales` | Retrieves active shift operators and assignments for a specific journal date. |
| **`fuelPriceQuery`** | `inventory` | `sales` | Supplies the effective fuel gallon prices (`Corriente`, `Extra`, `ACPM`) active on the journal date. |
| **`parameterQuery`** | `shifts` | `sales` | Provides station tolerance thresholds to classify seller cash closures (`OK`, `Warning`, `Discrepancy`). |
| **`sellerCloseQuery`** | `sales` | `reports`, `incentives`, `statistics` | Exposes consolidated cash, electronic payment media, fuel gallons, and product units per seller and day. |
| **`inventoryQuery`** | `inventory` | `sales`, `reports` | Exposes daily tank dip measurements, gallon variations, stock movements, and active inventory alerts. |
| **`JournalClosedEvent`** | `sales` | `inventory`, `incentives`, `reports`, `statistics` | Published when Step 6 locks the daily journal, triggering downstream recalculations and audit snapshots. |

### Domain Modules

| Module | Domain Scope | Core Responsibilities |
| :--- | :--- | :--- |
| **`sales`** | Ventas y Cierre Diario | 6-step daily operational close: shift personnel, seller close, fuel & product summary, cash balance, advances/changes, and final journal lock. |
| **`inventory`** | Inventario y Tanques | Fuel tank dip readings, gauge-table gallon interpolation, fuel receptions, product stock, price schedules, autonomy alerts, and ONAC inspection logs. |
| **`shifts`** | Turnos y Parámetros | Weekly shift matrix generation (`Mañana`, `Tarde`, `Noche`, `Descanso`), rest-day rotation, novelties, holiday surcharges, and station parameters. |
| **`workers`** | Trabajadores | Employee directory, document validation, contract lifecycle, social security (EPS, AFP, ARL), and active/inactive status control. |
| **`incentives`** | Incentivos | Monthly sales targets, distribution profiles, additive sales tracking, and automated employee bonus calculations. |
| **`security`** | Seguridad | JWT authentication, BCrypt password hashing, 5-attempt account lockout policy, session expiration, and Role-Based Access Control (`ADMIN`). |
| **`reports`** | Reportes | Automated generation of `VARIACIÓN` and `COMPLEMENTO DOMINUS` Excel workbooks and executive PowerPoint balance reports. |
| **`statistics`** | Estadísticas | Historical shift sales analytics, August baseline migration, and operational KPIs. |

### Core Business Workflow: The 6-Step Daily Journal Close (`sales`)

```text
[Step 1: Day Staff] ➔ [Step 2: Seller Close] ➔ [Step 3: Sales Summary] ➔ [Step 4: Cash Balance] ➔ [Step 5: Advances & Changes] ➔ [Step 6: Journal Lock]
```

| Step | Screen / Feature | Functional Scope |
| :---: | :--- | :--- |
| **Step 1** | **Personal del día** | Confirms operators on duty (`Mañana`, `Tarde`, `Noche`) from `shifts` and records daily shift novelties or replacements. |
| **Step 2** | **Cierre por vendedor** | Captures bills, coins, electronic cards (`Credibanco`, `G.A.`, `QR`), redemptions, Dominus fuel gallons, and lubricant/additive units per seller. |
| **Step 3** | **Resumen de ventas** | Consolidates total fuel gallons and convenience products sold across all shifts, cross-checked against active fuel prices. |
| **Step 4** | **Consolidado y saldo en caja** | Reconciles physical vault bills/coins, savings fund (`Fondo de ahorro`), bank deposits, and administrative expenses against expected cash. |
| **Step 5** | **Avances y cambios** | Registers cash advances, operator loans, and change fund movements prior to final closing. |
| **Step 6** | **Cierre de jornada** | Validates completion of Steps 1–5, displays inventory variation alerts, transitions the journal to **Read-Only (`CLOSED`)**, and emits `JournalClosedEvent`. |

<details>
<summary><strong>📂 View Internal Directory Structure (Backend & Frontend)</strong></summary>

<br>

**Backend Module Layout (Hexagonal / Clean Layering):**
```text
backend/src/main/java/com/edu/unicauca/gasstation/backend/<module>/
├── api/                              # REST Controllers, Request/Response DTOs & OpenAPI annotations
├── application/                      # Cross-module query ports (*Query) & event listeners
├── domain/                           # Pure business rules, domain models & service interfaces
├── infrastructure/                   # JPA entities, Spring Data repositories & MapStruct mappers
└── exception/                        # Module-specific domain exceptions
```

**Frontend Application Layout (Atomic Design + Feature Modules):**
```text
frontend/src/
├── atomic-design/                    # Reusable UI system (atoms, molecules, organisms, pages)
├── core/                             # Singleton API services, auth guards & JWT HTTP interceptors
├── features/                         # Domain modules (sales, inventory, shifts, workers, incentives, etc.)
└── shared/                           # Cross-cutting components, pipes, helpers & DTO contracts
```

</details>

---

## 🚀 Quick Start & Execution

### Prerequisites
- **Docker** & **Docker Compose** (v2+)
- *Optional (for host execution without Docker)*: **JDK 21 LTS**, **Maven 3.9+**, **Node.js 22+**

### 1. Configure Environment Variables (`.env`)

Create a `.env` file in the root directory of the repository with the following configuration:

```env
# Container user configuration
DOCKER_USER=gasstation_user
DOCKER_GROUP=gasstation_group

# Frontend - Angular PWA Container
FRONTEND_PORT=4200
FRONTEND_PROD_PORT=80

# Backend - Spring Boot API Container
SERVER_PORT=8080
JPA_DDL_AUTO=validate
JPA_SHOW_SQL=true
SWAGGER_PATH=/swagger-ui.html
API_DOCS_PATH=/v3/api-docs

# Database - PostgreSQL 16 Container
POSTGRES_DB_NAME=gasstation_db
POSTGRES_DB_USER=<your_db_user>
POSTGRES_DB_PASSWORD=<your_secure_password>
POSTGRES_LOCAL_PORT=5432
```

### 2. Start the Platform with Docker Compose

**Development Mode (Live Reload + Dev Containers):**
```bash
docker compose up --build
```

**Production Mode (Multi-Stage Nginx + Hardened JRE Runtime):**
```bash
docker compose -f docker-compose.prod.yml up --build -d
```

### 3. Service Endpoints & Security Provisioning

Once the containers report `healthy`, access the platform at:

| Service | URL / Port | Description |
| :--- | :--- | :--- |
| **Frontend Web App (Dev)** | `http://localhost:4200` | Angular 21 PWA application |
| **Frontend Web App (Prod)** | `http://localhost:80` | Nginx production reverse-proxy bundle |
| **Backend REST API** | `http://localhost:8080` | Spring Boot REST server |
| **Swagger UI (OpenAPI 3)** | `http://localhost:8080/swagger-ui.html` | Interactive API documentation & endpoint testing |
| **OpenAPI JSON Spec** | `http://localhost:8080/v3/api-docs` | Raw OpenAPI 3.0 contract |
| **PostgreSQL 16 Database** | `localhost:5432` | Database (`gasstation_db`) managed automatically by Flyway |

> [!NOTE]
> **Initial Account Provisioning & Security Policy:**
> On first startup, `AdminInitializer` verifies whether an administrator exists in the `users` table and seeds the bootstrap account with BCrypt hashing if empty. Authentication enforces JWT session expiration and a **5-attempt temporary lockout policy** (`HU-1`).

### 4. Run Automated Tests

```bash
# Run Frontend unit tests (Vitest inside Docker)
docker compose exec frontend npx ng test --watch=false

# Run Backend unit & Testcontainers integration tests
cd backend && ./mvnw test
```

---

## 📚 Project Documentation Index

Detailed architectural artifacts, functional inventories, and sprint planning documents are available in the [`docs/`](./docs) directory:

- 📄 [**Functional Inventory & Prototypes Map** (`docs/inventario-funcionalidades.md`)](./docs/inventario-funcionalidades.md) — Traceability matrix of all screens (`C-01`–`C-19`) and user stories (`HU-1`–`HU-39`).
- 🗓️ [**Sprint Roadmap & Milestones** (`docs/plan-trabajo-sprints.md`)](./docs/plan-trabajo-sprints.md) — 3-Sprint execution plan, August baseline verification criteria, and acceptance gates.
- 👥 [**Team Workload Distribution** (`docs/reparto-tareas-por-persona.md`)](./docs/reparto-tareas-por-persona.md) — Task allocation (`S1`–`S3`), module ownership, and cross-module integration contracts.

---

## 📐 Engineering & Git Standards

### Coding Conventions
- **Codebase Language**: **100% English** across source code, database tables/columns, REST endpoints, and OpenAPI documentation.
- **Naming Rules**:
  - `lowerCamelCase` for variables, properties, methods, and functions.
  - `PascalCase` for classes, interfaces, records, DTOs, and Angular components.
  - `SCREAMING_SNAKE_CASE` for constants and enum keys.
- **Database Migrations**: Managed strictly via Flyway (`backend/src/main/resources/db/migration/V1__*.sql` through `V34__*.sql`) with `spring.jpa.hibernate.ddl-auto=validate` and `spring.flyway.out-of-order=true`.

### Git Flow & Branching Strategy
- **`main`**: Protected, production-ready branch. Direct commits to `main` are prohibited.
- **Branch Naming Convention**:
  - Frontend features/fixes: `feature/front/<short-description>` or `fix/front/<short-description>`
  - Backend features/fixes: `feature/back/<short-description>` or `fix/back/<short-description>`
- **Pull Request Policy**: Every change must be submitted via Pull Request, pass automated GitHub Actions CI checks (`.github/workflows/ci-pipeline.yml`), receive peer review, and delete the feature branch after merging.

---

## 📝 Commit Convention

Commits follow the **[Platzi Git Commit Guide](https://platzi.com/blog/guia-de-commits-en-git/)** written in **English**:

```git
<emoji> <type>(<scope>): <imperative summary in lower case>

- Detailed bullet point describing specific change 1
- Detailed bullet point describing specific change 2
```

### Core Commit Types

| Type | Description | Type | Description |
| :--- | :--- | :--- | :--- |
| `✨ feat` | New feature implementation | `✅ test` | Add or update unit / integration tests |
| `🐛 fix` | Bug fix | `📝 docs` | Documentation updates |
| `♻️ refactor` | Code refactoring without behavior change | `🔧 config` | Configuration or build file updates |
| `💄 ui` | UI layout, CSS, and visual styling | `👷 ci` | CI/CD pipeline and workflow changes |
| `🗃️ db` | Database migrations, seeds, or schema changes | `🤖 chore` | Maintenance tasks outside `src` or `test` |
| `🛂 auth` | Authentication, security, roles, and permissions | `🔀 merge` | Branch and Pull Request merges |

> 📖 For the complete catalog of specialized commit emojis (performance, accessibility, dependencies, etc.), refer to the **[Official Platzi Git Commit Guide](https://platzi.com/blog/guia-de-commits-en-git/)**.

> [!IMPORTANT]
> **AI Co-Authorship Policy**: To maintain accurate repository analytics and ensure the GitHub contributor graph strictly reflects the human engineering team, `Co-authored-by:` trailers referencing automated assistants (such as Cursor or GitHub Copilot) must **never** be included in commits or Pull Request merges.

---

## 👥 Engineering Team

| Developer | GitHub | Primary Modules & Role |
| :--- | :--- | :--- |
| **Juan Fernando Portilla Collazos** | [@Jfernando014](https://github.com/Jfernando014) | `sales` (Steps 2, 4, 6), `security` & Repository Lead |
| **Santiago Alejandro Hurtado Campo** | [@santihurtado114](https://github.com/santihurtado114) | `sales` (Steps 1, 3, 4, 5) & `workers` |
| **José Alejandro Rodríguez Dueñas** | [@joserodriguezgn](https://github.com/joserodriguezgn) | `inventory` (Stock, Tanks, Prices & ONAC Inspections) |
| **Nicolle Alexandra Montaño Cifuentes** | [@NICOLLEUNI](https://github.com/NICOLLEUNI) | `shifts` (Schedules & Parameters) & `incentives` |
| **Javier Mauricio Solano Páez** | [@javiersolanop777](https://github.com/javiersolanop777) | `reports`, `statistics` & DevOps / Production Infrastructure |
| **José Nicolás Bambague Sánchez** | [@nicolas28B](https://github.com/nicolas28B) | QA, Testing, Historical Data Migration & DevOps CI |
