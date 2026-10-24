# Development entry point

For installation and daily use, read `README.md`. This file is for contributors and automated maintenance.

Read this file, `AGENTS.md`, `ROADMAP.md` and `CORROBORATIONS.md`, then consult the JSON indexes only if they help locate relevant work. Git and actual deliverables take precedence over cached state.

Before editing:

1. Inspect the repository, remote, branch, HEAD, local changes and recent commits. Preserve other people's work.
2. Identify the requested outcome and the smallest change that achieves it.
3. Load the relevant documentation, implementation and tests. Avoid unrelated historical working material.
4. Check current authorizations and dependencies before requesting further input.
5. Implement, review and verify the affected behavior, then report the result and any concrete limitation.

`README.md` owns product scope and setup. `docs/COMMAND_CONTRACT.md` owns troubleshooting, recovery and operation semantics. `docs/BUILD.md` owns build and distribution instructions. Keep each fact in its appropriate document and link only when the reader needs the detail.

Keep private signing material, local builds and raw operational records under ignored `development/`. A fresh clone must remain understandable and buildable without personal pages or private records.

Before stopping, keep the documentation and optional JSON caches consistent with the delivered change. Record only a useful next action when work remains; do not turn public documentation into an activity log.
