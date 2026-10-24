# Contributor and agent guidance

## Authority and document ownership

Follow platform instructions, the current user mandate and existing authorizations. Use only authorized resources and preserve other people's work.

Each concept has one owner: `START.md` covers development entry and resume; this file covers governance; `ROADMAP.md` covers development scope and delivery checks; `CORROBORATIONS.md` routes primary technical references. `README.md` owns product scope and setup, `docs/COMMAND_CONTRACT.md` owns recovery and operation semantics, and `docs/BUILD.md` owns build and distribution instructions.

The four canonical files are read-only for agents after bootstrap unless the current conversation explicitly delegates a relevant change. Keep changes within that delegation. JSON indexes are optional, non-authoritative caches; Git, deliverables and evidence take precedence.

## Working method

Use the smallest sufficient, reversible and verifiable solution. Load focused context, reuse valid evidence and communicate material changes. Add documents, abstractions, gates and tests only when they solve a concrete need.

The AI PM may delegate distinct tasks to Investigator/Executor and independent Reviewer roles. Subagents inherit the same scope and limits. Integrate and verify delegated work; avoid mechanically repeating checks.

Tie factual claims to their source and scope. Distinguish implementation review, synthetic tests, emulator checks and physical-device validation. Use `OBSERVED`, `INFERRED`, `PROVEN` or `ASSUMED` when useful in working evidence. Preserve concise decisions and results, not reasoning diaries.

## Autonomy and human dependencies

Work autonomously on code, documentation within its applicable delegation, local dependencies, builds, emulators, tests, commits and ordinary pushes in this repository. Existing authorization permits new non-colliding release tags and GitHub releases. Preserve repository visibility and existing signing keys.

Changing visibility, rewriting shared history, replacing release artifacts, collecting private device data or operating a real device requires applicable authorization. A past authorization for one completed action is not standing permission for another.

Ask for input only when a necessary decision, permission, physical action, credential or access is missing. Complete independent work first. Explain the specific constraint, proposed action, expected result, risk, recovery and evidence needed. Routine milestones and subagent creation do not require approval.

## Safety and verification

Verify the target before mutations. If identity, permission or results are uncertain, stop writes and explain the uncertainty. Never claim a successful operation from incomplete evidence.

Keep credentials, pairing codes, private keys, signing configuration, tokens and unnecessary personal identifiers out of Git and logs. Store private working material under ignored `development/`. Never upload backups or device data without authorization. Prefer project-local tools; host changes need a concrete reason, applicable authorization and recovery path. Do not bypass access controls.

Test contracts and material risks. Never invent test results, screenshots, benchmarks or compatibility. Repeat checks only for affected behavior, remaining risk or a required release check. Pin dependencies, retain license material and distinguish minimum supported versions from actually tested combinations.

## Documentation and delivery

Public documentation and release notes are in English; conversational updates may be in Italian. Use first-person singular if an authorial voice is needed. Keep user instructions direct, current and self-contained. Keep agent workflow, audit logs, historical cleanup details and private evidence out of the user documentation. Maintain working links and indexes without adding unnecessary navigation layers.

Terminal outcomes are:

- `MILESTONE_COMPLETE`: the requested outcome is delivered and sufficiently verified.
- `HUMAN_REQUIRED`: a specific necessary human dependency remains after independent work is complete.
- `BLOCKED`: no useful authorized step remains; explain the attempts and minimum unblock.

An interruption is not completion. Leave reconstructible state and identify any unfinished work.
