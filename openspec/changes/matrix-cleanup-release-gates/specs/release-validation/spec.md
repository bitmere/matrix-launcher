# Release validation capability — delta spec

> Historical publication requirements were superseded by the bitmere-releases change.

## ADDED Requirements

### Requirement: CI has a fail-closed aggregate gate
CI SHALL run module-qualified app/core tests, app lint, debug packaging and
existing Matrix structure/tool checks. An always-run `quality-gate` SHALL
require every validation dependency result to equal `success`; it SHALL
reject failure, cancellation, skip and missing results. Failure reports
SHALL include useful test/lint evidence without signing material.

#### Scenario: A required check does not succeed
- GIVEN any required validation job fails, is cancelled or is skipped
- WHEN the final quality gate runs
- THEN it fails even if another required job succeeds.

#### Scenario: Documentation-only pull request
- GIVEN a pull request changes only documentation or workflow files
- WHEN CI evaluates its trigger
- THEN required validation still runs without path-based exclusions.

### Requirement: Release input and artifact verification precedes publication
Release execution SHALL use separately approved manual dispatch from
main-controlled workflow code. It SHALL validate an existing unchanged tag,
main ancestry, exact version/tag and release channel, positive versionCode,
reviewed changelog, full unit suites and debug/release lint before publishing.
Only an optimized, non-debuggable APK with the expected application ID,
version/code and full approved new certificate SHA-256 SHALL be accepted.
A certificate prefix or newly observed unapproved certificate is insufficient.

#### Scenario: An input or signer does not match
- GIVEN a tag, version, main ancestry or full certificate check fails
- WHEN release validation evaluates the candidate
- THEN publication stops without substituting debug signing or another key.

### Requirement: Publication is serialized, monotonic and resumable
A fixed release concurrency group SHALL serialize publication without
cancelling an active release. The workflow SHALL reject older version codes
and divergent same-version bytes or metadata. It SHALL preserve exact
verified signed bytes, reuse the matching private release asset on retry,
retain prior versioned APKs and verify anonymous public downloads. Mirror
updates SHALL be fast-forward and preserve the existing five-field manifest.

#### Scenario: The mirror fails after the private release succeeds
- GIVEN the private release already contains the verified matching APK
- WHEN the same release is retried
- THEN it reuses and verifies that asset, completes the missing publication
  and confirms the anonymous mirror download has the same SHA-256.

#### Scenario: An older dispatch follows a newer publication
- GIVEN the mirror advertises a higher versionCode
- WHEN an older release attempts publication
- THEN the workflow fails without rolling the mirror back.

### Requirement: Signing and authentication material is temporary
Release signing SHALL occur only in Actions using existing GitHub secrets.
Temporary keystore/properties and authentication files SHALL be private and
removed on success, failure or cancellation. Secrets SHALL NOT appear in
logs, reports or uploaded artifact bundles.

#### Scenario: Signing or publication fails
- GIVEN temporary signing or authentication material has been restored
- WHEN a later workflow step fails
- THEN unconditional cleanup removes it and safe reports remain available.

## MODIFIED Requirements

### Requirement: The existing manual release contract is retained
The release workflow SHALL retain the Matrix mirror destination, versioned
APK naming, five-field manifest, application ID and signing identity. Release
notes SHALL warn that the new `bd4e6bf1…` signer cannot replace the retired
`15198ed1…` installation without a clean install and settings loss. Stable
publication SHALL require an explicit user request; betas remain prereleases.

#### Scenario: The next signed release is installed
- GIVEN a TV still runs an APK signed by the retired certificate
- WHEN the user follows the next release's installation guidance
- THEN the guidance requires clean installation, discloses settings loss and
  does not promise an in-place upgrade across signers.

## REMOVED Requirements

None. This change removes no release, updater or installation capability.
