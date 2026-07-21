# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Approved feature specification from `/specs/[###-feature-name]/spec.md`

## Summary

[Primary requirement, approved business decision, and implementation approach]

## Technical Context

**Backend**: Java 21, Spring Boot 3.x, Maven, Spring Data JPA, Spring Security Resource Server

**Frontend**: Angular 22, Angular Material, strict TypeScript, WCAG 2.1 AA

**Storage**: Oracle 19c+ in schema `KALLPA_PIIP`; manual versioned SQL under `database/`

**Identity**: Keycloak 26-compatible OIDC Authorization Code Flow with PKCE

**API**: REST OpenAPI 3.0 under `/api/v1`

**Testing**: JUnit 5, Mockito when appropriate, Oracle Testcontainers, Vitest, Playwright

**Project Type**: Modular monolith with Angular web frontend

**Performance Goals**: [Approved measurable goals or NEEDS CLARIFICATION]

**Constraints**: No Flyway/Liquibase; no external connectors or synchronization in Phase 1;
no shared database execution without explicit human authorization.

**Scale/Scope**: [Approved scope, users, and affected functional modules]

## Constitution Check

*GATE: Pass before research and re-check after design. A material ambiguity blocks planning.*

- [ ] Specification is approved or records each material unknown as `NEEDS CLARIFICATION`.
- [ ] Purpose, actors, business rules, affected states, exceptional cases, and acceptance
  criteria are traceable.
- [ ] Module owner, service contract, DTO boundaries, transaction owner, and inter-module
  interactions avoid direct foreign repository or table access.
- [ ] Business-rule authority is identified as application service or stored procedure with no
  duplication between Java and PL/SQL.
- [ ] Authorization combines Oracle-derived functional permission and organizational scope;
  the backend is the authority.
- [ ] Privacy classification, audit events, document evidence, and public exposure are defined.
- [ ] Canonical catalogs, transitions, document requirements, and code invariants are preserved
  or the specification records an approved amendment.
- [ ] Official-field obligation, editability, privacy, and ownership rules are approved for every
  affected lifecycle stage.
- [ ] Initiative and project remain separate linked records; direct-project exceptions identify
  their authority, evidence, origin, and organizational scope.
- [ ] Decision authority and operational registration roles are distinct and enforced where
  formal decisions apply.
- [ ] Public consultation exposes only approved public fields and document metadata, without
  document content or download during Phase 1.
- [ ] Affected interfaces have approved PIIP prototypes with approval evidence.
- [ ] Database scripts have versioned paths, execution order, and compensation or rollback.
- [ ] Tests cover constitutional risk: state, RBAC, transaction, audit, documents, architecture,
  procedures, API, accessibility, and frontend journey when applicable.
- [ ] No Phase 1-prohibited integration, generic `model/`, `client/`, or `integration/`
  directory is introduced.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

### Source Code (repository root)

```text
backend/
└── src/
    ├── main/java/pe/gob/midagri/piip/
    │   └── <modulo>/
    │       ├── controller/
    │       ├── service/impl/
    │       ├── repository/
    │       ├── dto/
    │       ├── entity/
    │       ├── exception/
    │       ├── mapper/
    │       └── event/
    └── test/java/pe/gob/midagri/piip/

frontend/
└── src/app/
    ├── core/
    ├── shared/
    └── features/

database/
├── ddl/
├── procedures/
├── functions/
├── packages/
├── indexes/
├── views/
├── seeds/
└── CHANGELOG.md
```

**Structure Decision**: [Affected modules, exact paths, and the reason their boundaries hold]

## Design Decisions

### Rule and Transaction Ownership

| Decision | Owner | Authority | Transaction and compensation |
|---|---|---|---|
| [State, permission, document, code, or scope rule] | [Module] | [Service or Oracle object] | [Boundary and recovery] |

### Persistence and Contracts

- **SQL change**: [Versioned scripts, dependent consumers, execution order, rollback or compensation]
- **API contract**: [Endpoint, DTO, authorization, errors, and OpenAPI impact]
- **Keycloak effect**: [None or approved Admin API operation, idempotency, compensation, audit]
- **Privacy and audit**: [Classification, records, access restrictions, and events]

## Complexity Tracking

> Fill only for a constitution exception approved in the feature specification.

| Exception | Approval reference | Why required | Simpler alternative rejected because |
|---|---|---|---|
| [Exception] | [Specification section] | [Need] | [Reason] |
