# Release validation

## ADDED Requirements

### Requirement: Publish exact verified assets

The workflow SHALL bind the approved tag to a commit on main and recover an existing APK for the tag before building. The gate SHALL verify package, version, non-debuggable flags and exact signer equality before writing the lowercase SHA-256 sidecar with two spaces, the APK filename and a terminal LF.

#### Scenario: Available or partially uploaded release

- GIVEN an existing draft or published release for the tag
- WHEN a release run resumes
- THEN it reuses existing APK bytes, fails on ambiguous tags, incomplete uploads or divergent assets, and uploads only missing assets on a draft.

### Requirement: Verify publication over the public API

The publisher SHALL name the release `Matrix Launcher TAG`, append the version code as the last line of the reviewed notes, compare both stored assets before publishing, and refuse rollback below a published stable version code. An anonymous check SHALL verify the release by tag, including the title, status, channel, version trailer, asset sizes, exact bytes and APK SHA-256.

#### Scenario: A newer stable release is already published

- GIVEN a published stable release with a higher Android version code
- WHEN publishing the candidate
- THEN the gate refuses publication.
