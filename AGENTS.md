# Governance

## Authority and ownership

Follow platform instructions, the current user mandate and existing authorizations. Public project instructions are self-contained; no personal workspace is required.

Each concept has one owner: `START.md` owns bootstrap/resume; this file owns governance; `ROADMAP.md` owns phases and gates; `CORROBORATIONS.md` routes external sources. Product requirements belong to `docs/PRODUCT_SPEC.md`; technical behavior belongs to `docs/COMMAND_CONTRACT.md`. Other artifacts refer to these without creating competing copies.

The four canonical files are **read-only for agents after initial bootstrap**, except under explicit, specific delegation in the current conversation. A general development mandate is insufficient. The October 2026 localization/publication mandate delegates only the new phase, public English documentation, public/private paths and alignment with that mandate and available evidence. For changes beyond an existing delegation, propose:

```text
HUMAN_REQUIRED
GATE=CANONICAL_FILE_CHANGE_APPROVAL
FILES=<file>
REASON=<need>
PROPOSED_CHANGE=<delta>
IMPACT=<effect>
```

JSON caches are non-authoritative. Git, deliverables and evidence take precedence; correct conflicting caches. Unprotected artifacts may evolve within the mandate without extra gates.

## Method and coordination

Use the smallest sufficient, reversible and verifiable solution. Load focused context, reuse valid evidence and communicate changes. Do not add documents, abstractions, gates or tests merely for completeness.

The AI PM coordinates Investigator/Executor and independent Reviewer roles when separation improves the work. Subagents are authorized within the project scope and must not expand it. Assign distinct files or worktrees; integrate and verify delegated work. Independent review addresses concrete risks rather than repeating every check mechanically.

Classify claims as `OBSERVED`, `INFERRED`, `PROVEN` or `ASSUMED`, with source and scope. User-reported physical results remain attributed to the user. For material root causes record `EVIDENCE_FOR`, `EVIDENCE_AGAINST`, `ALTERNATIVE_EXPLANATIONS`, `CONFIDENCE` and `MINIMAL_DISCRIMINATING_EXPERIMENT`. Preserve decisions and results, not reasoning diaries.

## Autonomy and human dependencies

Work autonomously on code, unprotected documents, project-local dependencies, builds, emulators, tests, commits and ordinary pushes within the designated repository. Preserve others' work and do not rewrite shared history. Check current authorization before asking again.

The accepted publication mandate also authorizes new non-colliding tags and GitHub releases in this repository using available access. It does **not** authorize changing repository visibility, overwriting releases/tags, force pushes, or collecting private device data.

A human gate is only for a genuinely necessary decision, access or action: physical pairing, an unauthorized live operation, scope expansion, undelegated canonical changes, destructive changes, missing credentials or sensitive publication not already authorized. Complete independent work first and prepare a concrete result. Milestones and subagent creation do not require approval.

A necessary request identifies the constraint and includes:

```text
PURPOSE=
EXPECTED_RESULT=
COMMANDS=<exact commands or UI actions>
RISK=
ROLLBACK=
EVIDENCE_TO_RETURN=
```

## Safety and quality

Use only authorized resources and devices. Verify the recipient before mutations. If identity, permission or results are uncertain, stop writes and state the uncertainty; do not claim success. Before changing real systems, prepare pertinent state inspection and recovery without exporting unnecessary data.

Prefer project-isolated tools and dependencies. Host changes require documented necessity, applicable authorization and rollback. Do not bypass access controls or permissions.

Keep credentials, pairing codes, private keys, signing keystores, tokens and unnecessary personal addresses/identifiers out of Git and logs. Keep private local material under `development/`, sanitize public evidence, and do not upload backups or real data externally without authorization. Preserve existing signing keys. A private file moved out of the current tree may remain in history: inspect publication-bound history and report unresolved exposure without rewriting it automatically.

Tests cover contracts and material risks. Distinguish synthetic checks from real validation. Never fabricate results, screenshots, benchmarks or compatibility. Repeat checks only for relevant changes, residual risk or required gates. Pin reproducible versions, verify licenses and distinguish declared, tested and unverified compatibility. Language-only changes require focused regression checks, not an invented new physical acceptance campaign.

## Hygiene and terminal conditions

Public documentation and release notes are in English; user-facing status reports may be in Italian. Identifiers, paths, APIs and states remain in English. Use first-person singular when an authorial voice is needed. Commit only necessary material, maintain links/indexes, retain third-party license texts and preserve repository visibility.

- `MILESTONE_COMPLETE`: the roadmap's terminal outcome is proved, not an intermediate checkpoint.
- `HUMAN_REQUIRED`: a specific human dependency remains after all autonomous work is complete, with an actionable request prepared.
- `BLOCKED`: no useful step remains within scope; document attempts, evidence and minimum unblock.

A pause, interruption or platform limit is not success. Save reconstructible state and leave unmet requirements open.
