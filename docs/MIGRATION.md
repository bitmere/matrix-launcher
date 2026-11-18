# Independent repository migration

The preserved Java checkpoint is `55a853a` (`v1.0.1-beta.4`) in personal-home-tv. The initial Compose experiment was `980a6e4` (`v1.0.1-beta.8`). This repository starts an independent Git history; it does not share a worktree/object store or require sibling source folders.

The bitmere build uses `com.matrix.launcher` and its preview uses `com.matrix.launcher.preview`. The first bitmere release requires a manual clean installation. Each package has its own app data sandbox.

All Matrix-facing screens now have Compose implementations: Home/channel/app browsing, favorites and reorder, source/row/card controls, SIMKL account approval and lists, wallpaper configuration, Home-control setup, quick settings and clock/screensaver. Invisible Android routing activities and system services remain platform adapters. Classic-only theme/effect/addon implementations are not carried into this app.

Home capture, input requests and remote settings use Compose routes. The manifest declares HOME eligibility; installation alone does not select it as Home. A new installation keeps all helpers and recovery opt-in. Do not enable this edition's capture while the Java edition's capture remains enabled.

An existing connected SIMKL account has survived updates and its menu was checked on the TCL. Fresh approval completion is covered by navigation policy tests, not repeat reauthorization. Reddit was retired in beta 27; its saved images are retained and its stored credentials removed. Actual provider profile changes still require source-app/device verification. See VALIDATION.md for physical Home/remote checks and the held-volume limitation. Emulator results do not certify these integrations. A release must record exact APK size/hash, tests/lint and device evidence before publication.
