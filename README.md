# Gas Station Management System (EDS Management)

## 📌 Architecture Proposal & Technical Specifications

This repository contains the full-stack architecture for the **Gas Station Management System**, built with a modular approach ensuring maintainability, domain decoupling, scalability, and adherence to clean architecture principles.

---

## 🏗️ Technology Stack

| Layer | Technology | Architecture Decision |
|---|---|---|
| **Backend** | Java 21, Spring Boot 3.2.x, Spring Modulith, Spring Data JPA, PostgreSQL, MapStruct, Swagger/OpenAPI | **Modular Monolith with Event Bus** |
| **Frontend** | Angular 17+, TypeScript, RxJS, PWA (Progressive Web App) | **PWA + Atomic Design + Feature Modules** |

---

## 🏛️ Backend Architecture: Modular Monolith with Event Bus

The backend is structured as a **Modular Monolith** using **Spring Modulith**. Business domains interact primarily through **asynchronous / in-process Domain Events** (Event Bus), maintaining high cohesion and loose coupling without the operational complexity of microservices.

### Domain Modules
- **`sales` (Ventas)**: Fuel and convenience sales processing, pump readings, transactions.
- **`incentives` (Incentivos)**: Performance incentives, commissions, and reward calculations.
- **`workers` (Trabajadores)**: Employee management, roles, and profiles.
- **`inventory` (Inventario)**: Fuel tanks, product stock, replenishments, and low-level alerts.
- **`shifts` (Turnos)**: Shift assignments, cash reconciliation, pump handover logs.
- **`security` (Seguridad)**: Authentication, JWT management, authorization, RBAC.
- **`statistics` (Estadísticas)**: Real-time metrics, KPI calculation, station throughput.
- **`reports` (Reportes)**: Exportable financial, operational, and audit reports.

### Module Internal Structure
Each module follows a hexagonal / clean layer structure:
```
backend/src/main/java/com/edu/unicauca/gasstation/backend/<module>/
├── <Module>EventExample.java          # Domain Events for event bus communication
├── api/                              # REST Controllers & OpenAPI documentation
│   └── <Module>ControllerExample.java
├── domain/                           # Pure business domain layer
│   ├── models/                       # Entities / Domain models (POJOs / records)
│   │   └── <Module>ModelExample.java
│   └── services/                     # Domain business interfaces
│       └── <Module>ServiceExample.java
├── infrastructure/                   # Framework and persistence implementations
│   ├── mappers/                      # MapStruct DTO/Entity mappers
│   │   └── <Module>MapperExample.java
│   └── persistence/                  # Spring Data JPA repositories
│       └── <Module>RepositoryExample.java
└── exception/                        # Module-specific domain exceptions
    └── <Module>NotFoundExceptionExample.java
```

---

## 📱 Frontend Architecture: Angular PWA

The frontend is an Angular Progressive Web App (PWA) designed for offline readiness, fast mobile execution for station operators, and a unified design system.

### Frontend Module Layout
```
frontend/src/
├── atomic-design/                    # Design system components
│   ├── atoms/                        # Primitive UI elements (buttons, inputs, icons)
│   ├── molecules/                    # Combinations of atoms (search bars, form fields)
│   ├── organisms/                    # Complex UI units (navigation bars, stat cards)
│   └── pages/                        # View templates assembling organisms
├── core/                             # Singleton services & HTTP pipeline
│   ├── guards/                       # Route guards (auth, role protection)
│   ├── interceptors/                 # HTTP interceptors (JWT bearer tokens, error handling)
│   └── services/                     # Application-wide core API services
├── features/                         # Independent functional business modules
│   ├── incentives/                   # Incentives views & state
│   ├── inventory/                    # Stock & tank monitoring
│   ├── reports/                      # Reports generation
│   ├── sales/                        # Point-of-sale & pump dispatch
│   ├── security/                     # Login & user management
│   ├── shifts/                       # Shift handovers & schedules
│   ├── statistics/                   # Analytics dashboard
│   └── workers/                      # Staff administration
└── shared/                           # Reusable presentation and utility items
    ├── components/                   # Generic cross-cutting components (modals, spinners)
    ├── helpers/                      # Pure helper functions & formatters
    ├── pipes/                        # Custom Angular transformation pipes
    └── models/                       # Shared data contracts
        ├── dto/                      # Data Transfer Objects
        ├── interfaces/               # Entity interfaces
        └── types/                    # Union and utility types
```

---

## 📐 Coding Conventions & Typing

- **Code Language**: **English** (variables, methods, class names, comments, and Swagger docs).
- **Naming Casing**:
  - `lowerCamelCase` for properties, variables, methods, and functions.
  - `PascalCase` for classes, interfaces, types, components, and services.
  - `SCREAMING_SNAKE_CASE` for constants and enum keys.
- **Documentation**:
  - All REST endpoints must include Swagger / OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`).
  - Swagger UI is accessible at `/swagger-ui.html`.

---

## 🌳 Git Flow & Branching Rules

### Main Branch
- `main`: Production-ready, stable codebase.

### Branch Naming Convention
Every branch must follow this pattern:
- Frontend features: `feature/front/<action-or-description>` (e.g., `feature/front/sales-pos-interface`)
- Backend features: `feature/back/<action-or-description>` (e.g., `feature/back/shifts-handover-api`)
- Fixes: `fix/front/<description>` or `fix/back/<description>`

### Pull Request & Merge Policy
1. **Never commit directly to `main`**: All changes must go through a **Pull Request (PR)**.
2. **Mandatory review**: PR must pass review and automated checks before merging.
3. **Delete branch after merge**: The feature branch must be automatically or manually deleted once merged.

---

## 📝 Commit Conventions

Based on the [Platzi Git Commit Guide](https://platzi.com/blog/guia-de-commits-en-git/):

- **Commit Language**: **English**
- **Structure**:
  ```git
  <type>(<scope>): <short general title in lowerCamelCase or imperative>

  - Detailed bullet point describing change 1
  - Detailed bullet point describing change 2
  ```

### Allowed Types
- `🎉 tada`: Start a project.
- `🚀 release`: Deploying things.
- `🔖new version`: Label / release version.
- `✨ feat`: New feature.
- `♻️ refactor`: Refactor code.
- `🐛 fix`: Bug fix.
- `🩹 patch`: Simple solution for a non-critical problem.
- `⚡️perf`: Improve performance.
- `🎨 style`: Improve code structure/formatting (whitespace, formatting, missing commas, etc.).
- `💄 ui`: Add or update the user interface and style files.
- `🚸 ux`: Improve user experience / usability.
- `🔥 remove`: Delete code or files.
- `🚧 construction`: Work in progress.
- `➕ add`: Add a dependency.
- `⏫ upgrade`: Update dependency.
- `⏬ downgrade`: Downgrade dependency.
- `⏪ revert`: Revert changes.
- `♿️ accessibility`: Improve accessibility.
- `🔀 merge`: Merge branches.
- `✅ test`: Add or update tests.
- `🔧 config`: Add or update configuration files.
- `🔨 script`: Add or update development scripts.
- `✏️ typos`: Correct typographical errors.
- `💬 test`: Add or update text and literals.
- `🚚 resources`: Move or rename resources (files).
- `🍱 assets`: Add or update assets (images).
- `🙈 gitignore`: Add or update a .gitignore file.
- `📱 responsive`: Work on responsive design.
- `⚗️ experiment`: Conduct experiments.
- `📝 docs`: Add or update documentation.
- `💡 comments`: Add or update comments in the source code.
- `🔍️ seo`: Improve SEO.
- `🌱 seeds`: Add or update seed files (seed files in APIs).
- `🤖 chore`: Other changes that do not modify src or test files.
- `👷 ci`: Changes to CI configuration files and scripts.
- `🔒️ security`: Resolve security issues.
- `🔐 secrets`: Add or update secrets (environment variables).
- `🚨 linter`: Fix compiler/linter warnings.
- `🌐 internationalization`: Internationalization and localization.
- `👽️ alien`: Update code due to changes in the external API..
- `🗃️ db`: Make changes related to the database.
- `🛡️ types`: Add or update types.
- `💫 animations`: Add or update animations and transitions.
- `🛂 auth`: Work on code related to authorization, roles, and permissions.
- `⚰️ dead`: Remove dead code.
- `👔 business`: Add or update business logic.
- `🦺 validation`: Add or update validation-related code..
- `💥 BREAKING CHANGE`: A commit that includes a `BREAKING CHANGE:` footer, or adds a `!` after the type/scope, introduces major changes to the API (corresponding to a MAJOR Semantic Versioning release). A `BREAKING CHANGE` can be part of a commit of any type.
