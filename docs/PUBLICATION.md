# Version 1.1.0 verification and publication

This record distinguishes local evidence from the owner's historical physical tests. Version 1.1.0 uses application ID `it.sorguido.chromecastkillswitch`, version code `2`, minSdk `26` and targetSdk `36`. The exact release tag and asset checksums identify the delivered revision; private raw logs remain under ignored `development/`.

## Evidence scope

**USER_REPORTED:** the owner reports successful real operational paths, including from the previous app, on Chromecast with Google TV HD (`boreal`, Android 14/API 34, user 0, Proton VPN) and Motorola edge 40 neo / Android 15. No detailed thirteen-case PASS receipt is inferred. This is not physical validation of the new bilingual APK.

**PROVEN locally:** both Android resource sets have the same 138 keys and matching format placeholders. Retained ViewModel messages use unresolved resource IDs; domain/transport enums map to resources without translating diagnostic phrases. The operation engine, command allowlist, identity binding and private store behavior remain unchanged by localization.

**PROVEN locally:** the release signing certificate matches the original 1.0.0 APK: SHA-256 `2936676a220e4a9682171fbf0c9b91552acd8fc4f120318c6a0aebe6e6722ecc`. The original key was moved intact into the ignored private area. Application ID is unchanged and version code increased from 1 to 2.

**PROVEN locally:** 24 unit tests passed (18 operation/state contracts, 5 framing/endpoint/identity parsing, 1 primary-language matrix). Release lint reports 0 errors and 7 reviewed warnings, including the documented upstream TLS trust limitation and legacy/vendor advisories.

**PROVEN on the dedicated Android 15/API 35 x86_64 emulator:** the instrumentation runner completed 21 cases without failures: 20 executed successfully and the opt-in real ADB pairing case was skipped because it was not requested. Coverage includes private identity persistence/Conscrypt loading, pending-store recovery, confirmations/cancel/busy exclusion, offline recovery, bilingual pairing validation and retained error/progress/dialog translation. Actual system-language cases were `it-IT`, `it-CH`, `en-US`, `fr-FR`, `fr-FR,it-IT`, `de-DE` and `es-ES`, each with a conflicting per-app preference. Italian was selected only for the first two. Activity recreation retained the ViewModel, pairing and pending-operation record; live language changes switched resources. Tests use synthetic identities and dispatch counters; no physical Chromecast mutation or phone-language change occurred. System language, font scale and temporary emulator test settings were restored.

**OBSERVED visual inspection:** real emulator captures of synthetic scenarios show readable Italian/English home screens in light/dark themes, English failure/recovery guidance and both languages' scrolling confirmations at 200% font size. Main confirmation/cancel actions remain reachable. Test screenshots are private and do not replace the owner's README screenshot. After the suite, four Italian recovery references were clarified to name the existing “Rileggi lo stato” action; this text-only delta is covered by resource/lint checks and independent semantic review.

**Independent review:** no blocking production regression was identified in primary-system language selection, resource-ID state, dialog language, preserved operations/identity/storage, signing or private layout. Review corrected the dialog test configuration and the obsolete Italian recovery-button wording, and verified safety-message equivalence. Older Android locale fallback (API 26–32), all allowed phones/ABIs, native dependency rebuilds and the new APK's physical TV operations remain unverified. No synthetic test certifies tunnel or leak behavior.

**PROVEN clean rebuild:** a separate clone of the pre-sanitization implementation revision ran `scripts/build.sh --unsigned --offline --no-build-cache`, using only the documented SDK and existing dependency cache. It contained no signing configuration or original key. All 124 tasks executed; tests, lint and unsigned release assembly succeeded. ZIP entry comparison with the signed working build found identical code/resources and only the expected VCS revision metadata difference. Later release-record edits do not change build inputs.

**PROVEN emulator update:** the original signed 1.0.0 APK was installed and launched, then updated in place with signed 1.1.0 using `adb install -r`. Android accepted the update and the English release screen launched correctly. Package metadata retained the original installation time and recorded the newer update time. No physical pairing is claimed by this emulator check.

The delivered revision is tag `v1.1.0`; [GitHub release assets](https://github.com/sorguido/chromecast-killswitch-app/releases/tag/v1.1.0) and `SHA256SUMS` identify the signed APK, matching application source, runtime-source supplement and notices. The release's verification receipt records final artifact and downloaded-asset checks without requiring a self-referential source checksum in this file.

## Source, licenses and private material

The tagged application-source archive includes wrapper, dependency locks/checksums, scripts, LibADB source, exact SPAKE2 Java/C archives, notices and licenses. The separate runtime-source supplement is collected by [the checksum-pinned script](../scripts/collect-runtime-sources.py) from the exact release runtime dependency graph, including Conscrypt's pinned BoringSSL source and the AndroidX Graphics Path native module. Its native code was not independently rebuilt; normal app builds use the locked upstream binaries. Recombination with a modified SPAKE2 library is documented separately and remains an optional recipe.

Original code uses GPL version 3 **or any later version**, with the narrow [section 7 permission](../LICENSE-ADDITIONAL-PERMISSION) needed for Conscrypt/BoringSSL. Third-party licenses and required OpenSSL/SSLeay acknowledgments remain intact. The source supplement is supplied alongside the binary rather than replaced by a generic upstream link.

Private signing configuration, original key, SDK/cache tools, build outputs, previous deliverables, raw tests and personal operational documents are under `development/`. No material from that directory belongs in the tagged source archive. `.gitignore` additionally covers direct Gradle/IDE caches, the SDK locator, misplaced secrets/binaries and native crash files; those are deliberate tooling/safety exceptions.

**OBSERVED:** the initial historical audit found personal operational documents and private-page references, without identifying owner credentials or private signing keys. A specific subsequent mandate authorized targeted history cleanup and release realignment. The repository remains **PRIVATE**. The current branch/tag ancestry is sanitized; residual old-commit access on GitHub still prevents claiming complete publication readiness.

The v1.1.0 tag preserves the owner's original Italian screenshot. The current README uses the English screenshot subsequently supplied by the owner, with its image content preserved. These screenshots illustrate observed interface states, not complete validation of every release path.

## Published release and history-cleanup verification

**PROVEN:** [v1.1.0](https://github.com/sorguido/chromecast-killswitch-app/releases/tag/v1.1.0) remains published, not draft or prerelease, with repository visibility PRIVATE. The sanitized tag points to `7177a18bc81c74fb062fbcfedca4756ff0fcedd6`, the rewritten equivalent of the original release commit, not the later `main` head. All ten historical commits and their author/committer attribution were preserved.

**PROVEN:** every tracked file and file mode at the release revision is identical before and after rewriting. This includes application code, resources, dependencies, build configurations, scripts and the Gradle Wrapper. The original APK was not rebuilt, re-signed or replaced. Its embedded Git SHA identifies the build before sanitization; it was **not** compiled from the new sanitized SHA.

APK SHA-256: `4e1583fc7421ba2ed508966ec8c567a336c0f0be5d6fa775a3a001885bf13e43`. Its signature was reverified after download and retains the original certificate stated above. Application ID, versionName and versionCode remain unchanged.

**PROVEN locally and from a new remote clone:** four personal operational documents were removed from all historical versions, and private-page references and personal ownership wording were replaced only where required. Exact identified page IDs/URLs are absent from reachable objects and inspected nested archives. Git integrity checks passed; the current README and English screenshot remained byte-identical. A focused independent reviewer confirmed the rewrite, tag mapping, tree equivalence, artifacts and fresh remote verification. Signature scans are bounded checks, not proof against every unknown secret. Upstream test-key fixtures in dependency sources and build paths inside unchanged upstream native libraries were verified as third-party material, not owner credentials or local paths.

**PROVEN remotely:** the regenerated application-source archive matches all 97 tagged files and contains no `development/` material. GitHub's generated tag archive was also compared with the tagged tree. The source archive, `SHA256SUMS`, `RELEASE_VERIFICATION.txt` and English release notes were updated. The APK, runtime-source supplement and notices retain both their original bytes and GitHub asset IDs. All six release assets were downloaded after the update and matched the prepared hashes; README, screenshot and release notes were checked through the authenticated API. Historical documents inside the tag describe the original qualification context; this current record and the release receipt explain the rewritten provenance.

**PROVEN locally:** verified private bundles, original assets/checksums, commit maps and signing backups were retained under ignored `development/`. The backed-up keystore opens with the backed-up configuration and has the original certificate. The operating checkout was aligned using the verified fresh clone after exact tree equality and clean-worktree checks, without merging old history or changing private signing material, caches or build artifacts. Historical backup checkouts have default remote pushes disabled.

## Shared Gradle cache on main

The subsequent `main` change makes `scripts/build.sh` default to `$HOME/.gradle`, respecting a nonempty `GRADLE_USER_HOME` override. An unset or empty value uses the default. Android SDK selection, unsigned/signing behavior, test/lint tasks and outputs under `development/build/` are unchanged. Wrapper files remain intact. No cache, SDK, emulator, key or previous artifact was moved or deleted, and no build was run for this one-line behavior change.

**PROVEN by focused synthetic checks:** `bash -n` passed; wrapper interception verified unset, empty and explicit cache values against absent, explicit and project-local SDK settings, exact Gradle arguments, unsigned mode and the missing-signing-configuration guard. These are shell behavior checks, not another application qualification campaign. The `v1.1.0` script retains its original cache default, so release input equivalence is preserved.

## Remaining GitHub dependency

**OBSERVED after the controlled push:** authenticated GitHub API requests still resolve nine superseded commits, and a file at an old SHA still contains an identified private-page reference. These objects are absent from current branch/tag ancestry but are not proven erased from GitHub storage or caches. The repository inventory showed no pull requests, forks, Actions runs/artifacts, Pages or wiki. No claim is made about unobservable copies in other people's clones.

A concrete support request is prepared privately and has **not** been sent. GitHub determines whether this private-context case qualifies for removal; support cannot be assumed to purge all copies. The minimum remaining action is for the owner to submit the prepared request through [GitHub Support](https://support.github.com/) and then recheck the identified old-SHA access. The repository must remain private while this publication blocker is unresolved. GitHub's [removal guidance](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository) distinguishes rewritten refs from cached views and other clones.

`L2_HISTORY_PUBLICATION_APPROVAL` and `L2_REACHABLE_HISTORY` are satisfied. `L2_GITHUB_RESIDUAL_REMOVAL` remains open, so the overall state is `HUMAN_REQUIRED`, not `MILESTONE_COMPLETE`. No renewed authorization for the completed rewrite is needed.
