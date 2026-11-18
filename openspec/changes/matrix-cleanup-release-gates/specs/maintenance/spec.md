# Maintenance capability — delta spec

## ADDED Requirements

### Requirement: Maintenance changes preserve supported behavior
M1 SHALL preserve runtime code/resources, optional integrations, Android-free
core, Matrix UI roles, D-pad behavior, low-memory support, preference keys,
application ID, service bridges and legacy Home alias. Already-merged PR 13
update-flow files SHALL remain untouched. Provenance and MIT/font/icon
attribution SHALL remain intact.

#### Scenario: An apparent cleanup candidate has no deletion evidence
- GIVEN a runtime declaration or resource appears unused
- WHEN M1 maintenance is applied without confirmed consumer/safety evidence
- THEN it remains unchanged and no functionality is removed.

### Requirement: Validation claims distinguish source review from execution
Maintenance documentation SHALL list current module-qualified test/lint
commands and label historical validation reports as historical. Current
measured results SHALL remain pending until the overseer records actual
runs. Source/JVM checks SHALL NOT be presented as TV, remote, account or
live-service integration acceptance.

#### Scenario: The sandbox cannot execute Gradle or device checks
- GIVEN source and specification checks complete in the sandbox
- WHEN the validation status is reported
- THEN Gradle, lint, signing and device results remain explicitly unverified
  until their respective authorized environments execute them.

## MODIFIED Requirements

### Requirement: Current documentation follows the checked-in baseline
Current guidance SHALL describe Matrix 2.7.4/code 153, classify AI tools
against implementations and tests, keep genuinely pending work, retain
historical release facts and use resolving relative links. Keyless preview
and testing paths SHALL remain useful; distributable release instructions
SHALL direct users to signed GitHub Actions artifacts.

#### Scenario: A future-work entry is already implemented
- GIVEN tool implementations, provider loops and tests substantiate completion
- WHEN the TODO documentation is reconciled
- THEN that completed work is reclassified while pending coverage and
  integration work remain visible.

#### Scenario: Historical measurements describe version 2.0.0
- GIVEN a validation link points to the old 2.0.0 report
- WHEN a reader follows current validation guidance
- THEN it is labelled historical and separate current commands/results
  identify what still requires overseer execution.

### Requirement: Signing guidance reflects the fixed rotation context
Documentation SHALL describe release signing as GitHub Actions-only with
existing secrets, the next release's clean-install/settings-loss consequence
and artifact retrieval. It SHALL NOT request passwords, promise local signed
release builds or suggest key recovery, regeneration or debug release signing.

#### Scenario: A contributor has no release keystore
- GIVEN signing keys and random passwords exist only in GitHub secrets
- WHEN a contributor follows the documented local workflow
- THEN keyless preview/tests remain available and signed release artifacts
  are obtained from the approved GitHub Actions run.

## REMOVED Requirements

None. Historical documentation and supported tool functionality remain;
only stale current guidance is corrected.
