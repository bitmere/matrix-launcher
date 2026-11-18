# TV gotchas

TV-specific traps for Matrix Launcher, with the measurement or incident
that proved each one. Add a new trap here (and in the matching skill)
whenever one bites — a trap that lives only in someone's head will bite
again.

## Numbers first

Every "feels slow" becomes a bench result before it becomes a change.
Run `tools/bench.py` (see the tv-bench skill): the fixed D-pad tour,
3-run medians, the gates. D-06 (row cache windows) and D-22
(prefs-revision recomposition) are decided by measurement, not by
guessing. A change is proven only when the same tour, same gates,
3-run medians show it.

## Gradle daemon loopback: other_tcp / IPv4

- The Gradle daemon's loopback was blocked twice: first by the VM's
  `other_tcp=Deny`, then by an IPv4/IPv6 mismatch.
- Fix that works: force Java to IPv4-only with
  `JDK_JAVA_OPTIONS="-Djava.net.preferIPv4Stack=true"`, and set
  `other_tcp` to Ask in the web app (the Android app has no such
  screen).
- Toolchain (verified 2026-11-14): JDK 17 Temurin at
  `~/workspace/android-tooling/jdk-17.0.21+9`, Android SDK at
  `~/Android/sdk`, Gradle 8.13 at
  `~/workspace/android-tooling/gradle-8.13`, `GRADLE_USER_HOME` at
  `~/workspace/android-tooling/gradle-home`, proxy flags
  `-Dhttp.proxyHost=hatch-egress-proxy -Dhttp.proxyPort=3128`.
- Symptom to recognise: the build hangs starting the daemon instead of
  failing with an error. Check the loopback first.

## Debug-key install conflicts

- A build signed with the debug key cannot update a build signed with
  the release key (and vice versa): the install fails with a signature
  conflict. The fix is uninstalling first, which loses settings —
  export a backup (Settings → Backup → Export backup) before you
  uninstall.
- The permanent release key is `~/.matrix/matrix-release.jks`
  (alias `matrix`, RSA 2048, valid to 2054-04-01); its password is in
  `~/workspace/android-tooling/release-keystore/password.txt`, with a
  backup copy there too. Never commit the keystore or its password.
  Keep the same release key once chosen: changing it breaks updates
  for existing installs.
- Release builds run as root in this environment, so `user.home` is
  `/root` — the signing wiring checks `$HOME` too. A rebuild that
  comes out unsigned after a key change is the `user.home` mismatch
  until proven otherwise; verify with `apksigner`.

## Play Protect sideload blocking

- Play Protect blocks sideloaded APK installs: choose "Install
  anyway" on the block screen. If the install never starts, the
  installer app needs "Install unknown apps" allowed (TV Settings →
  Apps → Special app access → Install unknown apps).
- Disabling Play Protect scanning is a fallback, not the first step.

## Key mapper / Home helper setup

- The launcher takes over Home through the Home helper / key mapper
  path: the selected Home helper must stay Matrix, and the key filter
  must stay on, or the remote's Home key stops reaching the launcher.
- Matrix preserves the user's selected Home helper and accounts /
  preferences unless a takeover is explicitly authorised. Do not
  change the helper during a cleanup.
- The input switcher, gamepad passthrough, and key services are
  behind explicit settings for the same reason: a "helpful" reset
  that turns them off looks like a broken remote.

## gradlew executable bit

- CI failed on `./gradlew: Permission denied` because `gradlew` was
  mode 100644. The fix is a mode-only commit (`gradlew` → 100755).
  After any checkout, patch, or rebase that touches `gradlew`, check
  the bit before pushing: `git ls-files -s gradlew`.

## workflow_dispatch runs report head_branch = main

- A release workflow run started with `workflow_dispatch` reports
  `head_branch == main`, not the tag or branch you dispatched from.
  Any watcher that filters runs by `head_branch == <tag>` will miss
  the run. Filter by the run's event / display title instead, or do
  not filter on `head_branch` at all for dispatched runs.

## Home never stops drawing (fixed)

- The focused card's glass rim used to revolve forever on a 4 s loop,
  and on a TV something is always focused — so Home drew frames at
  rest. The fix: the rotation sweeps once (0→360 over ~1.5 s) when
  focus lands and then holds still (see `rememberMatrixFocusRotation`
  in `MatrixButton.kt`). The card stays highlighted (border + 1.015
  scale + backdrop); only the motion stops.
- Verify with the bench idle window: ≤ 1 idle frame/s. The only other
  infinite animations are in `MatrixSearchOrb.kt` and
  `MatrixSearchScreen.kt` (search-only, not Home); the clock ticks
  once a minute; wallpaper rotation happens on Home load, not on a
  timer.

## Safe mode exists — use it before uninstalling

- If Home is blank or crashing after a bad build, safe mode starts
  Home with wallpaper, AI discovery, and custom rows off:
  `adb shell am start -n
  com.matrix.launcher/com.gathin.tvhome.ComposeHomeActivity
  --ez safe_mode true`. Three consecutive startup crashes enter it
  on their own. Exit from Settings → "Exit safe mode".
- Uninstalling loses settings. Safe mode first, `pm clear` second,
  uninstall last — and export a backup before any of them when you
  can still reach Settings.
