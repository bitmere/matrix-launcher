# Tasks — matrix-cleanup-release-gates

> Historical publication requirements were superseded by the bitmere-releases change.

1. Reconcile the supplied main snapshot and documentation.
   Files: `version.properties`, current documentation, AI tool/provider
   implementations and their existing tests.
   Verify: current guidance reflects 2.7.4/code 153; completed AI tools are
   classified using implementation/test evidence; dated history remains.

2. Correct validation, provenance and installation guidance.
   Files: `TESTING.md`, `docs/VALIDATION.md`, `docs/README.md`,
   `docs/PROVENANCE.md`, `docs/NEW-TV-INSTALL.md` and stale signing guidance.
   Verify: historical 2.0.0 results are labelled; current module-qualified
   commands are documented; measured results remain for the overseer;
   namespace link resolves; Actions artifacts and clean-install/settings-loss
   warning describe new `bd4e6bf1…` versus retired `15198ed1…` signing.

3. Harden CI without weakening existing checks.
   File: `.github/workflows/ci.yml`.
   Verify: module-qualified tests/lint/debug build, structure/tool tests,
   failure reports without secrets, unchanged PR coverage and always-run
   `quality-gate`; failure, cancellation, skip or missing dependency cannot
   produce a passing gate.

4. Harden the existing manual release workflow.
   Files: `.github/workflows/release.yml`, `tools/release_gate.py` and its
   offline tests; signing adapters in `app/build.gradle` and `build.ps1`.
   Verify: main-controlled dispatch, tag/version/main/channel validation,
   full unit suites and debug/release lint, Actions-only secret restoration,
   full `MATRIX_RELEASE_CERT_SHA256` approved rotation-evidence pin,
   optimized APK metadata/full signer/hash verification, serialized monotonic
   publication, exact-asset retries, anonymous mirror verification and
   unconditional secret cleanup. Preserve existing mirror/naming/schema.

5. Validate the documentation/spec/workflow diff without Gradle.
   Files: this change's four OpenSpec files and all M1 edits.
   Verify: `openspec validate matrix-cleanup-release-gates --strict` when a
   supported CLI is obtainable. Otherwise compare structure with existing
   changes, check every requirement/scenario and report that fallback
   explicitly. Check links, references and whitespace; no runtime/updater,
   version, attribution or installed-identity changes.

6. Overseer validation remains pending; do not mark it passed locally.
   Run focused checks, full app/core suites, lint with zero errors, debug
   packaging, structure/tool tests and release workflow static review.
   Record actual environment and report results rather than historical skill
   counts. Signed artifact verification occurs only in an approved Actions
   run; physical TV, D-pad, account/integration and install acceptance remain
   separate evidence. No push, PR, merge, tag or release is performed here.
