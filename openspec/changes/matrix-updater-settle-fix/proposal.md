# Proposal — matrix-updater-settle-fix

## Why
A stale saved PackageInstaller session makes every update retry fail while
settling the previous install, even though the downloaded APK is valid.

## Scope
- Add a JVM-testable session store and existence-aware settlement.
- Clear settled session preferences synchronously.
- Share settlement with both confirmation-failure callbacks.
- Log settlement failures with the saved ID and exception.
- Add focused plain JVM regression tests.

Android documents null for missing sessions. AOSP also returns null for
visibility-filtered sessions; visible non-owned sessions can return information.
Lookup failures are not evidence that a session is gone.
Sources: [API](https://developer.android.com/reference/android/content/pm/PackageInstaller#getSessionInfo(int)),
[AOSP](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/pm/PackageInstallerService.java).

## Non-goals
No PR, merge, or release; no change to the install flow for live sessions;
no new permissions or dependencies, UI changes, or version bump.
