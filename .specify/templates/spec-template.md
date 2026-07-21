# Feature Specification: [FEATURE NAME]

**Feature Branch**: `[###-feature-name]`
**Created**: [DATE]
**Status**: Draft
**Input**: User description: "$ARGUMENTS"

## Purpose and Scope *(mandatory)*

**Purpose**: [Business value and intended outcome]
**In Scope**: [Explicit capabilities]
**Out of Scope**: [Explicit exclusions, including unapproved Phase 2 integrations]

## Actors and Authorization *(mandatory)*

| Actor or role | Action | Functional permission | Organizational scope | Decision or registration responsibility |
|---|---|---|---|---|
| [Actor] | [Action] | [Role or permission] | [Unit constraint] | [Decides, registers, or not applicable] |

State whether the operation uses a canonical PIIP role. Distinguish who decides from who records a
formal decision. Identify `NEEDS CLARIFICATION` when authorization or organizational scope
materially changes behaviour.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - [Brief Title] (Priority: P1)

[Describe the independently valuable user journey]

**Why this priority**: [Value]
**Independent Test**: [End-to-end observable result]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [outcome]
2. **Given** [initial state], **When** [exception], **Then** [safe outcome]

---

[Add prioritized stories as needed. Every story requires acceptance scenarios.]

## Business Rules and State Impact *(mandatory)*

- **BR-001**: [Testable business rule and its authoritative owner when known]
- **BR-002**: [Transition, document, code, permission, or scope rule]

| Affected entity or state | Current condition | Change | Evidence or document | Exception |
|---|---|---|---|---|
| [Entity or state] | [Condition] | [Change] | [Required evidence] | [Handling] |

Use only canonical portfolio catalogs and transitions. A non-canonical value or a material
unknown MUST be written as `NEEDS CLARIFICATION`, never inferred.

For portfolio features, identify the affected official fields and their approved obligation,
editability, privacy, and ownership matrix. State whether an initiative and project are separate
linked records, and document any direct-project exception.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST [specific, testable capability].
- **FR-002**: System MUST [authorization, organizational scope, or state constraint].
- **FR-003**: System MUST [audit, document, or privacy behaviour when applicable].
- **FR-004**: System MUST [preserve initiative-project traceability and official-field rules
  when applicable].

### Privacy, Security, and Audit Requirements

- **PSA-001**: [Personal or sensitive data classification and access restriction]
- **PSA-002**: [Backend authorization and token requirement]
- **PSA-003**: [Audit event, evidence, and immutable fields]
- **PSA-004**: [Public fields and document metadata; no public content or download in Phase 1]

### Integration and Data Change Requirements

- **IDC-001**: [Oracle scripts, procedure, API, Keycloak effect, or `Not applicable`]
- **IDC-002**: [Consumers, compatibility effect, and compensation when a contract changes]

## Edge Cases *(mandatory)*

- [Unauthorized or out-of-scope actor]
- [Invalid state transition, missing evidence, or concurrent update]
- [Direct project without approved exception, evidence, or organizational scope]
- [Partial failure, rollback, or recoverable compensation]
- [Document validation, antimalware, or privacy-restricted outcome when applicable]

## Key Entities *(include when the feature involves data)*

- **[Entity]**: [Business meaning, identity, lifecycle, and relationships]

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: [Measurable business or user outcome]
- **SC-002**: [Measurable security, accessibility, or correctness outcome]

## Assumptions and Clarifications

- [Explicitly safe assumption that does not change business behaviour]
- `NEEDS CLARIFICATION`: [Material unresolved decision and responsible approver]

## Constitution Conformance *(mandatory)*

- State how the specification avoids inferring any business rule, actor, state, permission,
  catalog, or integration.
- Identify module, service, DTO, transaction, and rule-authority boundaries.
- Describe security, privacy, organizational RBAC, and audit impact.
- Confirm preservation of required documents, transitions, correlatives, and canonical catalogs.
- Identify official fields, the initiative-project relationship, direct-project exceptions, and
  decision versus registration roles when applicable.
- Describe public-consultation and prototype-approval constraints when applicable.
- Identify testing obligations proportional to risk.
