# Matrix toolchain migration

Date: 2026-11-28. Checkout: `matrix-liquid-glass-phase1`, base `765a88f3` (2.7.16).
Build configuration edits only; no Gradle invocation, compilation, installation, commit, push, or PR. All gates remain with the coordinator. Existing spike edits were preserved.

## Versions landed in configuration

| Component | Before | After |
| --- | --- | --- |
| Android Gradle Plugin | 8.13.2 | 9.1.1 |
| Gradle distribution | 8.13 | 9.3.1 (binary ZIP) |
| compileSdk | 36 | 37 |
| Kotlin / Compose compiler plugin | 2.2.20 | 2.4.21 |
| Compose BOM declaration | 2026.03.00 | 2026.03.00 (unchanged) |
| AndroidX Compose selected by Haze | 1.12.1 in existing spike failure | 1.12.1 expected; coordinator must confirm |
| JDK / Java target | 17 | 17 (unchanged) |
| minSdk / targetSdk | 26 / 34 | 26 / 34 (unchanged) |

AGP 9.1.1 supports API 37.0 and requires Gradle 9.3.1, JDK 17 and Build Tools 36.0.0. No identified requirement forces a minSdk or targetSdk change. If a gate reveals one, stop and report it instead of changing either value. [Official AGP compatibility table](https://developer.android.com/build/releases/agp-9-1-0-release-notes).

The binary distribution SHA-256 is `b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06`, independently verified against the 9.3.1 entry in the [official Gradle checksum reference](https://gradle.org/release-checksums/), and matches Trove. Direct checksum endpoint retrieval was unavailable through the web tool (binary content type) and shell network; verification used the official HTML reference. Wrapper scripts and JAR were not regenerated because Gradle must not run this turn.

## Kotlin decision and dependency resolution

Choose Kotlin 2.4.21 to match Haze 2.1.0's producer toolchain and Compose compiler, rather than retain 2.2.20 without proof that it can consume the newer Kotlin metadata. AGP itself only requires KGP 2.2.10 or higher; it does not require 2.4.21. Haze's [2.1.0 changelog](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/CHANGELOG.md) records Kotlin 2.4.21 and Compose Multiplatform 1.12.2. Its [tagged version catalog](https://raw.githubusercontent.com/chrisbanes/haze/2.1.0/gradle/libs.versions.toml) records Jetpack Compose 1.12.1. These producer versions are not proof of an exact minimum consumer Kotlin version; matching them avoids an unverified older-compiler choice.

Remove `org.jetbrains.kotlin.android` from root and app plugins. With AGP 9.x built-in Kotlin enabled, applying that plugin causes a configuration error. Keep built-in Kotlin and the new DSL enabled by default. The root Compose plugin at 2.4.21 selects the matching KGP automatically; no unused Kotlin JVM plugin or legacy opt-out is needed for Matrix's Java-only core. This follows the [official built-in Kotlin migration and version-selection instructions](https://developer.android.com/build/migrate-to-built-in-kotlin). Keep the existing JVM toolchain 17 configuration.

Haze's changelog documents the move to standard compile SDK 37. The existing `spike-build-failure.log` independently records both Haze Android artifacts requiring API 37 and Compose 1.12.1 requiring AGP 9.1.0+. Haze's own catalog uses AGP 9.5.0 to build upstream; that is not itself a consumer minimum. The prior artifact metadata diagnostics support the requested consumer migration to 9.1.1; rerun metadata checks to confirm the entire graph.

The BOM is an ordinary `platform`, not `enforcedPlatform`. Its 1.10.5 constraints do not cap higher transitive requirements: Haze already selected AndroidX Compose 1.12.1 in the spike. Expect those affected Compose modules to remain 1.12.1 after this migration, overriding the BOM's lower recommendations. This is not a claim that every Compose-family artifact has the same version, nor a newly measured dependency graph. Compose Multiplatform 1.12.2 and AndroidX Compose 1.12.1 are separate coordinates. The prior log also shows lifecycle-runtime-compose-android 2.11.0 and AndroidX core 1.19.1; coordinator should inspect those overrides too. No dependency declarations or BOM were changed this turn.

## Complete list of files changed this turn

1. `build.gradle`: AGP 9.1.1, Compose compiler plugin 2.4.21, remove external Android Kotlin plugin declaration.
2. `app/build.gradle`: remove Android Kotlin plugin application; compileSdk 37. The two Haze dependencies were already present and remain untouched.
3. `gradle/wrapper/gradle-wrapper.properties`: Gradle 9.3.1 URL and verified binary ZIP checksum.
4. `.github/workflows/ci.yml`: configure Android command-line tools and install API 37 / Build Tools before Gradle.
5. `.github/workflows/release.yml`: same SDK setup in both independent jobs; update platform directory check.
6. `openspec/changes/matrix-liquid-glass-phase0/migration.md`: this report.

`gradle.properties` is unchanged: inspection found no demonstrated incompatible setting requiring removal. No deprecation suppression or Kotlin compatibility escape hatch was added. The existing `android.suppressUnsupportedOptionWarnings` setting/comment is retained rather than treated as an SDK compatibility mechanism; inspect resulting warnings in the coordinator gates.

## Workflow details

CI build and both release jobs now run `android-actions/setup-android@v3` with `packages: platform-tools`, followed by `sdkmanager 'platforms;android-37.0' 'build-tools;36.0.0'`, before Gradle setup/invocation. Each release job gets its own SDK installation because runners are independent.

The release validation directory check is `$ANDROID_HOME/platforms/android-37.0`, matching the installed package and Trove's actual check. This intentionally refines the prompt's shorthand `android-37`; checking that literal directory would not match the requested `android-37.0` package. Signing, release recovery, publication controls, JDK setup and gate commands are unchanged. Trove was read only; no dependency on that checkout was introduced.

## Coordinator review and gates

- Review the five build/workflow diffs before compiling, per AGENTS.md. `git diff` against HEAD also includes pre-existing Haze source/dependency edits; those are not migration edits.
- Confirm wrapper bootstrap selects Gradle 9.3.1 and verifies the pinned checksum; verify API 37.0 and Build Tools 36.0.0 installation on the runners.
- Confirm built-in KGP and Compose compiler resolve to 2.4.21, JVM target remains 17, and the existing Groovy toolchain block configures correctly under the new DSL.
- Capture dependency insight for debug/release compile/runtime classpaths: Kotlin stdlib/compiler, Compose UI/foundation/runtime/animation, lifecycle and AndroidX core. Confirm the expected Compose 1.12.1 overrides and successful AAR metadata validation.
- Run the prescribed structure/tool checks, focused JVM tests, full checks/lint and debug build; then approved optimized release gates. No tests or builds were run here; only source inspection and `git diff --check` (passed).
- Inspect Gradle 9 removed APIs/deprecations and configuration-cache behavior. No incompatible custom build API was identified statically; runtime validation is outstanding.
- AGP 9.1 enables R8 repackaging into the default package unless existing rules control it. Inspect optimized output and existing reflection/manifest/service keep behavior in the release gates; no speculative ProGuard changes were made. See the official AGP release notes above.
- Keep minSdk 26, targetSdk 34, app ID, preference keys, legacy bridges and alias unchanged. Version properties remain 2.7.16/base values because no artifact was installed here; any future installation must follow the repository's versionCode rule.

App sources, tests, resources, Haze attribution, OpenSpec drafts and all other files are outside this migration and were not edited. Build success and on-device compatibility are not asserted.
