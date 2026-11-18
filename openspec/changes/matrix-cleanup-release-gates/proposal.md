# Change: matrix-cleanup-release-gates

> Historical publication requirements were superseded by the bitmere-releases change.

## Why

The supplied main snapshot is Matrix 2.7.4 (versionCode 153), but some
current documentation describes older releases or completed AI tools as
future work. CI needs a fail-closed aggregate result, and the existing
manual release workflow needs explicit source, signer and publication gates.

## Scope

1. Correct current documentation against checked-in implementations and
   tests; preserve dated history, provenance, MIT, font and icon attribution.
2. Harden `.github/workflows/ci.yml` without reducing existing checks,
   protections or pull-request coverage, including documentation changes.
3. Harden `.github/workflows/release.yml`, its small publication helper and
   offline tests, and existing signing adapters while retaining manual dispatch,
   the Matrix mirror destination, APK naming and five-field updater contract.
4. Record release-validation and maintenance delta requirements here.

## What Changes

- CI uses module-qualified test/lint tasks, existing structure/tool checks,
  safe failure reports and an always-run `quality-gate` that requires every
  dependency result to be exactly `success`.
- Release gates validate tag, main ancestry, version/channel, lint, optimized
  APK metadata, full approved signer fingerprint and exact signed bytes.
- `MATRIX_RELEASE_CERT_SHA256` supplies the full public certificate pin from
  approved rotation evidence; this change does not retrieve or rotate keys.
- Serialized publication rejects rollback or divergent same-version assets,
  supports retrying partial publication and always removes temporary secrets.
- Documentation explains Actions-only signing and the next release's clean
  installation and settings loss after the 2026-11-16 signing-key rotation.

## Impact and preserved behavior

No runtime code or resource deletion is justified. Preserve application ID
`com.matrix.launcher`, preference keys, the four `com.gathin.tvhome`
bridges, legacy Home alias, shared Matrix controls, deterministic D-pad
behavior, optional integrations, low-memory support and Android-free core.

The five PR 13 update-flow files are already on main per the supplied
reconciliation and remain untouched: `ApkInstaller.kt`,
`MatrixSettingsPages.kt`, `UpdateInstallResultReceiver.kt`,
`UpdateFlowTest.kt` and `MatrixReleaseNotesScreen.kt`. There is no updater
commit in M1 and no new cleanup deletion inventory to approve.

Version/tag changes, release execution, mirror changes, Trove work and
repository protection/auto-merge configuration are outside this change.
The new `quality-gate` must be observed before separately authorized branch
protection changes; existing required checks remain required meanwhile.

## Validation

Review the complete diff, local links and references; validate this OpenSpec
change and workflow structure. The local snapshot commit is `6a8619c`
(labelled `pristine main @ 3bbc9ee`); authenticated live-main/PR API
reconciliation is not claimed by this source-only change.

Gradle is prohibited in this sandbox. The overseer must run focused checks,
then `./gradlew --no-daemon :app:testDebugUnitTest :core:test :app:lint
:app:assembleDebug`, plus release-variant lint and the final integrated
gate as appropriate. No test totals, lint results, signing success, device
coverage or publication outcome are inferred from source inspection.
