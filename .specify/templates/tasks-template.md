---
description: "Task list template for PIIP feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Approved `spec.md`, `plan.md`, and design artifacts from `/specs/[###-feature-name]/`

**Prerequisites**: plan.md and spec.md are required. Use research.md, data-model.md,
quickstart.md, and contracts/ when present.

**Tests**: Tests are mandatory for changed business behaviour and constitutional risks. Include
JUnit 5, Oracle Testcontainers, architecture tests, Vitest, and Playwright tasks as applicable.

**Organization**: Group tasks by user story after shared foundations. Include exact paths.

## Format: `ID parallel-marker story-marker description`

- `parallel-marker` is `[P]` only for tasks with different files and no incomplete dependency.
- `story-marker` is `[US1]`, `[US2]`, and so on for user-story tasks only.
- Every task begins with a Markdown checkbox and a sequential `T` identifier.

## Path Conventions

- **Backend**: `backend/src/main/java/pe/gob/midagri/piip/<modulo>/...`
- **Backend tests**: `backend/src/test/java/pe/gob/midagri/piip/<modulo>/...`
- **Frontend**: `frontend/src/app/core/`, `frontend/src/app/shared/`, or
  `frontend/src/app/features/<modulo>/`
- **Database**: `database/ddl/`, `database/procedures/`, `database/functions/`,
  `database/packages/`, `database/indexes/`, `database/views/`, or `database/seeds/`
- Do not use `model/`, `client/`, or `integration/` directories.

## Phase 1: Setup and Architecture

**Purpose**: Establish only the approved module, contracts, and tooling.

- [ ] T001 Create the approved module paths in `backend/src/main/java/pe/gob/midagri/piip/[modulo]/`
- [ ] T002 Configure the required Java, Spring Boot, Angular, Oracle, and Keycloak dependencies
- [ ] T003 Add architecture tests preventing controller-to-repository access, entity leakage from services, and prohibited directories
- [ ] T004 Create versioned Oracle scripts and update `database/CHANGELOG.md` with execution order and compensation
- [ ] T005 Configure OpenAPI `/api/v1`, security resource-server validation, and audit foundations as required

## Phase 2: Foundational Controls

**Purpose**: Complete blockers before any user story.

- [ ] T006 Implement module service contracts and DTO boundaries in the approved module paths
- [ ] T007 Implement Oracle-derived authorization and organizational scope checks where affected
- [ ] T008 Implement immutable audit event recording for affected operations
- [ ] T009 Implement transaction, stored-procedure, and compensation boundaries where affected
- [ ] T010 Implement document validation, SHA-256, antimalware status, or `DocumentStorage` contract where affected
- [ ] T011 Add unit, integration, architecture, and Oracle Testcontainers coverage for foundational controls

**Checkpoint**: Foundations enforce constitutional constraints before story work begins.

## Phase 3: User Story 1 - [Title] (Priority: P1)

**Goal**: [Business capability]
**Independent Test**: [Observable end-to-end result]

### Tests for User Story 1

- [ ] T012 [P] [US1] Add JUnit state, rule, authorization, and audit tests in `backend/src/test/java/pe/gob/midagri/piip/[modulo]/`
- [ ] T013 [P] [US1] Add Oracle Testcontainers procedure or persistence tests in `backend/src/test/java/pe/gob/midagri/piip/[modulo]/`
- [ ] T014 [P] [US1] Add Vitest feature tests in `frontend/src/app/features/[modulo]/`
- [ ] T015 [P] [US1] Add Playwright critical-journey test in `frontend/e2e/[feature].spec.ts`

### Implementation for User Story 1

- [ ] T016 [US1] Create DTOs, entities, mapper, repository, service, and controller in `backend/src/main/java/pe/gob/midagri/piip/[modulo]/`
- [ ] T017 [US1] Implement the authoritative rule in the approved service or Oracle object without duplication
- [ ] T018 [US1] Implement the Angular feature in `frontend/src/app/features/[modulo]/`
- [ ] T019 [US1] Document the API contract and privacy or audit behaviour in `specs/[###-feature-name]/contracts/`

**Checkpoint**: User Story 1 is independently functional, authorized, audited, and tested.

## Phase N: Additional User Stories

Repeat the User Story 1 structure in priority order. Each story MUST include proportionate
backend, persistence, frontend, accessibility, authorization, audit, and end-to-end tests.

## Final Phase: Cross-Cutting Validation

- [ ] TXXX Run backend, Oracle Testcontainers, frontend, and Playwright suites
- [ ] TXXX Verify at least 80 percent business-code coverage and explicit coverage of critical invariants
- [ ] TXXX Verify WCAG 2.1 AA requirements for affected Angular screens
- [ ] TXXX Verify OpenAPI, SQL changelog, rollback or compensation, and consumer compatibility
- [ ] TXXX Re-run the Constitution Check and document its result in `specs/[###-feature-name]/plan.md`

## Dependencies and Execution Order

- Setup and Foundational phases block user-story work.
- Within a story, test tasks precede implementation and all authority, transaction, RBAC, and
  audit work precedes exposure through a controller or visual action.
- Database scripts and their integration tests precede dependent service completion.
- A story is complete only when its acceptance criteria, required tests, and constitutional
  controls pass.

## Notes

- `[P]` means independent files and dependencies only; it does not waive review or tests.
- Do not execute shared-database scripts or external Keycloak actions without explicit human
  authorization.
- Do not add Phase 2 connectors, PIDE synchronization, or simulated external adapters.
