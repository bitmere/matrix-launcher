# Release-path audit — 2026-11-17

> Historical audit of the earlier release pipeline. GitHub Releases now serves both update assets.

Scope: every step in `.github/workflows/release.yml`, every function in
`tools/release_gate.py`, its offline tests, and release signing in `app/build.gradle`.
Only those four files and this report were edited. Version remains 2.7.5 / 154;
no key, approved certificate pin, tag, secret, variable, or remote state was changed.
The supplied checkout has an empty `.git` directory, so original-file snapshots
were used for comparisons; no branch checkout, push, dispatch, or publication occurred.

## Findings and fixes

| Location | Evidence / issue | Applied fix |
| --- | --- | --- |
| `release_gate.py:release_state`, `publish_private` | Tag lookup can return 404 for a draft; original publisher then subscripts `None`. The reported draft ID 448631718 has no assets. | Discover through paginated release listings, match `tag_name`, retain numeric ID throughout upload/publication. No release deletion. Regression reproduces the exact original `NoneType` failure. |
| `release_state`, `validate_release` | A draft URL can use `untagged-*`; a genuinely pending pseudo-tag also needs association with the approved tag. Multiple candidates must not select an arbitrary draft. | Match actual tag regardless of URL; also recognize a pseudo-tag draft with the exact expected title, source, channel and notes, then associate it by ID. Reject ambiguity. |
| `publish_private` | Human-facing `gh release create` output and a subsequent tag lookup are not a reliable identity contract. | Create through REST, validate and capture its ID; reconcile a failed/timed-out creation through discovery without repeating creation or deleting drafts. |
| `api`, `listing`, `validate_release`, `release_assets` | Previously assumed dictionaries, non-null body, booleans, embedded asset arrays and present keys. Lists/assets can be paginated. | Explicit type/schema checks, numeric IDs, paginated release and asset endpoints, duplicate-name checks, and clear failures for incomplete/zero-byte assets. A `starter` asset is not treated as a reusable APK. |
| `api` | GET-only status parser could not support create responses; HTTP failures/timeouts had no bounded retry. | Parse HTTP/1.1, HTTP/2 and HTTP/2.0 plus intermediate headers; accept 200/201, validate JSON, distinguish missing reads from errors. Retry only transient reads; never blindly retry writes. Pin API version and GitHub host. |
| `api` upload path | ID-based uploads require raw bytes and a known request length; `gh --input` can supply a stream of unknown length. | Explicit binary content type and `Content-Length`; JSON mutations use typed JSON input, not CLI string-field coercion. |
| `publish_private`, `check_assets` | New uploads were never downloaded/compared; an existing divergent asset could be discovered after another upload. | Compare all existing required bytes before mutation, reconcile uncertain uploads by listing, then download both stored assets and compare exact bytes before publishing. Revalidate release metadata/ID and publication response. No asset replacement/clobber. |
| `recover` | A checksum-only draft could trigger a different rebuild despite already committing to bytes. | Refuse rebuild when the checksum exists without its APK. Existing APKs remain the recovery source; published missing APKs still fail closed. |
| `verified_manifest`, `publish_private`, `main:mirror` | Bytes could change after verify; rollback check preceded retention/tag recheck steps. | Rehash local APK and bind it to the complete verified manifest and checksum before either publisher; repeat monotonic check immediately before private publication. |
| `verify_apk` | Regex accepted a digest prefix with trailing garbage; repeated scheme reports of the same certificate caused rejection. Badging required one attribute order. | Anchor complete signer certificate lines, retain legacy and V2 formats, compare signer identities, exclude public-key digests, parse badging by attribute name. Wrong signer/package/version and debuggable APKs remain rejected. |
| `verify_apk` | Highest numeric fragments could select preview/incomplete SDK tools, then fail looking for `aapt`. | Select the highest complete stable build-tools installation with both tools. |
| `metadata`, `guard` | Duplicate properties silently won; missing fields and non-object manifests could raise incidental exceptions. Indented comments/spacing differed from properties conventions. | Explicit property validation and comment/spacing support; reject duplicate/missing/malformed values and non-object manifests. Preserve exact tag, channel, notes and monotonic rules. |
| `publish_mirror` | Clone used the repository's default branch although every later operation assumed `main`; Git credential helpers could override the dedicated askpass credential. | Clone `main` explicitly; disable credential helpers for clone/push so the supplied askpass/PAT is used. Validate wanted schema and local APK hash before clone. Preserve one commit, non-forced fast-forward push, remote-head and manifest rechecks. |
| `fetch`, `anonymous`, subprocess helpers | One stale CDN response or transient HTTP failure failed verification immediately; subprocesses could hang until job cancellation. | Anonymous cache-busting requests, no-cache header, bounded read/backoff and propagation retries, exact digest check, subprocess timeouts. No token attached to public requests. |
| `release.yml` validation and release jobs | Gate was copied from controlling main but tests ran from the version tag, which can contain older gate code. | Copy and run controlling gate tests before build; snapshot controlling workflow/signing config for those tests. Both jobs retain source-SHA binding and main/tag checks. |
| `release.yml` fetch/report steps | Implicit fetch ref mapping depends on local configuration; both jobs used the same artifact name, and names also repeated across rerun attempts despite upload-artifact v4's immutability. | Explicit remote-main refspec and distinct per-job/per-attempt report names and per-attempt measured-APK artifact names. Set Bash, locale and noninteractive GitHub CLI environment explicitly. |
| `release.yml` signing restore | Regex stripping of Java properties did not cover escaped keys/continuations; a trailing continuation could consume the appended `storeFile`. | Append authoritative `storeFile` after a terminating blank line; retain strict base64, private permissions, temporary paths and unconditional cleanup. |
| `app/build.gradle` signing setup | Partial properties or absent keystore failed later through opaque signing/configuration errors. | Validate all required properties and keystore existence early with nonsecret messages. Actions-only signing and unsigned keyless builds remain; no debug fallback. |

## Checks retained after review

Dispatch-only entry; main-controlled workflow; tag syntax, ancestry and SHA output binding;
release-only contents-write permission; non-cancelling publication concurrency;
keyless validation; R8/resource shrinking and mapping-file requirement;
full approved fingerprint equality; beta prereleases; reviewed changelog notes;
immutable existing assets; mirror rollback/same-code guards and fast-forward race rejection;
anonymous byte verification; scoped secrets and always-run cleanup.

No keytool, curl or jq output is parsed in this path. Git uses documented plumbing
outputs (commit IDs / file contents) and exit codes, never localized prose.
`apksigner`, `aapt`, HTTP headers and JSON parsing are covered with synthetic fixtures.
No run/tag API shape is assumed: source checks use Git and Actions' declared output.

## Validation

- 55 focused release tests pass; all 60 repository Python tool tests pass.
- Structure audit passes; workflow YAML and all 22 shell blocks parse.
- 12 selected behavioral/configuration regressions were run against preserved original
  files and failed, including the exact untagged-draft `NoneType` crash, null notes,
  reordered badging, transient GET, signer duplication, properties and signing wiring.
  The corresponding tests pass with the patch. New helper/API-contract tests also pass.
- Real Git tests use temporary local bare repositories: one advancing commit, identical
  rerun, non-main default branch, conflicting APK, rollback and concurrent writer.
- No JDK, Android SDK, PowerShell or actionlint is installed here. No Android artifact
  was requested/built/installed; `build.ps1 -Checks`, Gradle and live Actions/signing/PAT
  integration were therefore not executed. Offline success is not live release proof.
- Corrupt, ambiguous, incomplete or externally changed state deliberately stops safely;
  this patch does not delete drafts, incomplete assets, or replace existing bytes.

## Platform references checked

- [GitHub releases REST](https://docs.github.com/en/rest/releases/releases): published tag lookup, draft visibility, pagination and ID-based mutation.
- [GitHub CLI draft lookup implementation](https://github.com/cli/cli/blob/trunk/pkg/cmd/release/shared/fetch.go): separate pending-draft discovery from published tag lookup.
- [GitHub CLI create implementation](https://github.com/cli/cli/blob/trunk/pkg/cmd/release/create/create.go): returned release identity/URL; exit success alone is not evidence of a discoverable tag endpoint. Reuse behavior reported by the user is treated as incident evidence, not a universal CLI promise.
- [Release assets REST](https://docs.github.com/en/rest/releases/assets): raw upload/length, ID downloads, duplicate filenames and failed-upload `starter` state.
- [CLI HTTP implementation](https://github.com/cli/cli/blob/trunk/pkg/cmd/api/http.go): absolute upload URLs and explicit Content-Length handling.
- [Actions context availability](https://docs.github.com/en/actions/reference/workflows-and-actions/contexts): runner context remains inside steps, not job env.
- [Android certificate verification](https://support.google.com/googleplay/android-developer/answer/16641489): signer certificate SHA-256 output. V2 format is covered by the incident's supplied evidence and synthetic fixtures.
