# Android package identity

## ADDED Requirements

### Requirement: Use the Matrix release and preview package IDs

The release application ID SHALL be `com.matrix.launcher`. The preview build SHALL append `.preview`. The code namespace SHALL remain `com.matrix.launcher`. Package-aware tools SHALL use the application ID of the intended variant.

#### Scenario: First bitmere installation

- GIVEN an installation of an earlier Matrix build
- WHEN the user moves to the first bitmere build
- THEN the user installs it manually into the new application sandbox before using the in-app updater.
