# Delivery roadmap

The current terminal objective is the localized, publication-ready Android app defined in [PRODUCT_SPEC.md](docs/PRODUCT_SPEC.md), with verified distributable artifacts. Ordinary phase transitions are checkpoints of the same goal.

The existing native Android/authorized wireless ADB implementation remains the primary path. Localization/publication does not expand TV, firmware or VPN-provider support, or change the operation contract. Investigate a demonstrated implementation problem with a minimal discriminating experiment; do not build a competing transport or change product scope without a pertinent mandate.

## Existing baseline

The original implementation completed toolchain, integration and local qualification work. The owner subsequently reported successful real operational paths, including from the app, on the designated setup. That is **user-reported** evidence, not a newly observed thirteen-case acceptance receipt and not physical validation of the bilingual APK. Historical raw working material is private; public results belong in [PUBLICATION.md](docs/PUBLICATION.md).

## Localization and publication phase

| Stage | Required result | Exit evidence |
| --- | --- | --- |
| L1 | First functional change: complete EN default and IT resources; only primary system language `it` selects Italian; no per-app override | Resource/placeholder checks, locale-selection tests, relevant UI and lifecycle regressions |
| L2 | One private `development/` area; portable English public documents, real screenshot and GPL-3.0-or-later original code; preserved third-party material | Current-tree and history review, links, source/notice inventory and documented unavoidable tooling exceptions |
| L3 | New version with original application ID and signing identity; focused qualification | Release build, lint, operation/framing/state tests, bilingual UI checks, certificate comparison, clean-checkout rebuild without private signing material |
| L4 | Matching source, signed APK, checksums and short English release notes delivered through a new tag/release when access permits | Commit/tag correspondence; actual release assets and links; downloaded APK hash/signature verification; unchanged repository visibility |

[BUILD.md](docs/BUILD.md) owns build/distribution instructions. [PUBLICATION.md](docs/PUBLICATION.md) owns current results and limitations. JSON caches only route that evidence.

All stages must be addressed before `MILESTONE_COMPLETE`. If remote publication is technically unavailable, finish the verified local release bundle and provide the exact missing access or action; do not call a local bundle or draft a published release. Unresolved private history exposure prevents declaring the repository publication-ready, even if the current tree is clean. Do not change visibility. The separately authorized 2026-10-24 targeted history cleanup is recorded below; it does not authorize future rewrites.

Preserve the user-reported physical evidence without inventing detail. Recheck the risks affected by changes; emulator/synthetic tests do not certify live tunnel behavior. Any genuinely unavoidable human dependency follows `AGENTS.md` after all independent work is finished.

## Targeted history cleanup — 2026-10-24

The specific mandate satisfied `L2_HISTORY_PUBLICATION_APPROVAL`. `L2_REACHABLE_HISTORY` is proven by a new remote clone: current branch/tag ancestry is sanitized, release source inputs and the original APK are preserved, and the release source/checksum/receipt assets are aligned. The shared Gradle cache default is a subsequent change on `main`, outside `v1.1.0`.

`L2_GITHUB_RESIDUAL_REMOVAL` remains open: authenticated access to old commits still exposes identified private context. A support request is prepared privately and has not been sent. No pull requests, forks, Actions runs/artifacts, Pages or wiki were present in the checked inventory. L2 and the overall publication-readiness objective are not complete; state remains `HUMAN_REQUIRED` for this concrete residual, not for renewed rewrite approval. See [PUBLICATION.md](docs/PUBLICATION.md) for evidence and limits.
