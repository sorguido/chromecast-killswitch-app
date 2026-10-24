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

**PROVEN clean rebuild:** a separate clone of implementation commit `817b8d5` ran `scripts/build.sh --unsigned --offline --no-build-cache`, using only the documented SDK and existing dependency cache. It contained no signing configuration or original key. All 124 tasks executed; tests, lint and unsigned release assembly succeeded. ZIP entry comparison with the signed working build found identical code/resources and only the expected VCS revision metadata difference. Later release-record edits do not change build inputs.

**PROVEN emulator update:** the original signed 1.0.0 APK was installed and launched, then updated in place with signed 1.1.0 using `adb install -r`. Android accepted the update and the English release screen launched correctly. Package metadata retained the original installation time and recorded the newer update time. No physical pairing is claimed by this emulator check.

The delivered revision is tag `v1.1.0`; [GitHub release assets](https://github.com/sorguido/chromecast-killswitch-app/releases/tag/v1.1.0) and `SHA256SUMS` identify the signed APK, matching application source, runtime-source supplement and notices. The release's verification receipt records final artifact and downloaded-asset checks without requiring a self-referential source checksum in this file.

## Source, licenses and private material

The tagged application-source archive includes wrapper, dependency locks/checksums, scripts, LibADB source, exact SPAKE2 Java/C archives, notices and licenses. The separate runtime-source supplement is collected by [the checksum-pinned script](../scripts/collect-runtime-sources.py) from the exact release runtime dependency graph, including Conscrypt's pinned BoringSSL source and the AndroidX Graphics Path native module. Its native code was not independently rebuilt; normal app builds use the locked upstream binaries. Recombination with a modified SPAKE2 library is documented separately and remains an optional recipe.

Original code uses GPL version 3 **or any later version**, with the narrow [section 7 permission](../LICENSE-ADDITIONAL-PERMISSION) needed for Conscrypt/BoringSSL. Third-party licenses and required OpenSSL/SSLeay acknowledgments remain intact. The source supplement is supplied alongside the binary rather than replaced by a generic upstream link.

Private signing configuration, original key, SDK/cache tools, build outputs, previous deliverables, raw tests and personal operational documents are under `development/`. No material from that directory belongs in the tagged source archive. `.gitignore` additionally covers direct Gradle/IDE caches, the SDK locator, misplaced secrets/binaries and native crash files; those are deliberate tooling/safety exceptions.

**OBSERVED:** the initial audit inspected all six reachable commits and 119 distinct blobs, including the bundled source archives. No credential/private-key values were found by signature scanning and inspection; this is not an absolute guarantee. Historical commits do contain personal operational context and private-page references. The new tree removes those references, but moving files cannot erase Git history.

**PUBLIC VISIBILITY BLOCKER:** this repository remains **PRIVATE**. Its complete history is not yet publication-ready. Before changing visibility, the owner must authorize targeted history sanitization or explicitly accept publication of the identified historical context. Neither history rewriting nor visibility changes are part of this release. The private audit identifies paths and commits without reproducing secret values. A release published in this private repository retains the same access perimeter.

The v1.1.0 tag preserves the owner's original Italian screenshot. The current README uses the English screenshot subsequently supplied by the owner, with its image content preserved. These screenshots illustrate observed interface states, not complete validation of every release path.

## Published release verification

**PROVEN:** [v1.1.0](https://github.com/sorguido/chromecast-killswitch-app/releases/tag/v1.1.0) was published (not draft/prerelease) at commit `0c83c63896c5d65cebe7cbfd8c1c6d78d34529cb`, with visibility still PRIVATE. All four payload assets were downloaded from GitHub and matched the published SHA256SUMS and verified local bytes. The downloaded APK signature matches the original certificate, and its VCS metadata identifies the exact tagged commit. The downloaded source archive matches every tracked blob of that tag and contains no private `development/` content. README and the real screenshot were verified through the authenticated GitHub API.

APK SHA-256: `4e1583fc7421ba2ed508966ec8c567a336c0f0be5d6fa775a3a001885bf13e43`.

The release also includes `RELEASE_VERIFICATION.txt` with the actual checks and payload hashes. This final publication record is a documentation-only follow-up; it does not replace the tagged source or overwrite any asset. The public-history requirement above remains open, so the overall state is `HUMAN_REQUIRED`, not `MILESTONE_COMPLETE`.
