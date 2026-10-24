# Development roadmap

The current product scope is defined in `README.md`. Maintenance should improve reliability and usability within that scope. Supporting another TV model, firmware version or VPN provider requires a deliberate scope decision and relevant validation.

## Maintenance priorities

- Keep pairing, discovery, state reads and recovery understandable and reliable.
- Preserve the complete ON/OFF behavior in `docs/COMMAND_CONTRACT.md`, including confirmation, readback, restart and verification.
- Keep English and Italian messages consistent across progress, errors, help and accessibility.
- Keep setup, build instructions and release downloads accurate and easy to find.

These are maintenance boundaries, not promises of unimplemented features.

## Checks for a new app release

| Area | Required result |
| --- | --- |
| Behavior | Focused checks for the code changed, including operation and recovery risks where affected. |
| Interface | Relevant language, lifecycle and accessibility checks. |
| Build | A release build, lint and applicable tests; a build without private signing material remains possible. |
| Signing | Preserved application ID and signing identity for updates, with an increased version code. |
| Distribution | Matching source, required notices, checksums and concise release notes accompany the APK. |
| Documentation | Accurate scope and limitations; working setup, recovery and build links. |

Use `docs/BUILD.md` for commands and packaging instructions. A documentation-only change needs document and link checks, not a new app build or an invented physical test campaign.

Apply the terminal outcomes in `AGENTS.md` to the current request. Keep completed operational history in Git instead of adding dated progress narratives here.
