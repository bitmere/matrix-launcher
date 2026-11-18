# 2.1.0 post-publication device check — 2026-11-04

TCL C755, Android 14. Published APK was downloaded and its SHA256 matched the installed artifact. 148 JVM tests passed, lint zero errors/one existing warning; final dock/header and reload checks passed before publication.

## Reboot

A real ADB reboot was performed while Matrix was foreground. No Home key or launch intent was sent during recovery. Fallback Home and Google TV appeared first; Matrix was observed foreground at **40.8 seconds**. This is a polling observation, not exact boot-to-interactive timing. The first Matrix dump was still loading; a later settled dump confirmed **24 apps, 7 rows, Home connected, recovery enabled**. Selected accessibility services remained unchanged.

## Memory and background

Settled Android memory status was **normal**, reporting 968,259 KiB free including reclaimable caches. Matrix PSS was 67,458 KiB (~65.9 MiB), versus RSS 171,620 KiB (includes shared mappings). Large resident/cached app sizes alone do not justify disabling them. No apps were disabled, uninstalled or force-stopped; stock Home, Google services, voice input and TV functions remain available.

With Android Settings foreground, Matrix rendered **zero frames over 10 seconds** and its process CPU counters advanced by zero ticks. Its only listed running service was the selected HomeKeyService. This is a short sample, not a long-term battery/network audit. Visible-Home glow still costs rendering work. Activity-scoped refresh/observers and visible lifecycle tasks stop in the background; Home capture intentionally remains bound.

## ART and animations

All three animation scales were 0.5 and remain unchanged. These affect animation duration, not the work needed to boot/load an app.

ART was already active; after installation the package had `verify` compilation. Requested app-only `cmd package compile -m speed -f com.matrix.launcher`; command succeeded and TCL selected **speed-profile** (`reason=cmdline`). No global runtime settings changed. The TV was reboot-tested before this compilation; no compiled cold-boot speed claim is made.

Same warm 24-move scenario: before compilation 19/921 janky frames, p95/p99 46/48ms; after compilation 1/937, p95/p99 46/46ms. The process was not restarted between samples, and single-run/wallpaper/boot-settling effects plus inconsistent vendor counters prevent attributing improvement to compilation. p95 was unchanged. Home jank remains unresolved.

Sources: [Android memory management](https://developer.android.com/topic/performance/memory-management), [ART compilation](https://source.android.com/docs/core/runtime/configure), [animation settings](https://developer.android.com/studio/debug/dev-options).

Evidence is ignored under `artifacts/2.1-*` and `artifacts/profiling/2.1-postboot-*`. TV returned to Matrix Home after checks.

## Final device choices

After the measurements above, Google Assistant (`com.google.android.katniss`) and Google Play Games (`com.google.android.play.games`) were disabled for user 0 at the owner's request. Data was retained. Assistant processes stopped; Matrix D-pad/Back, injected Home, TV Settings and the Inputs menu were checked. This change was not separately reboot-tested. Earlier memory/ART samples predate these changes.

MagiConnect, YouTube Music and Web Video Caster remain enabled. TCL update/input services, stock Home, casting and the active Ambient screensaver were retained. No app was uninstalled. To reverse an app disable after re-enabling debugging: `adb shell pm enable --user 0 <package>`.

2.1.0 is the final published release for this work. No development service or ongoing automation is required. Accounts, preferences, recovery and the selected Home service remain intact. The owner requested debugging be turned off at handoff; it is not needed for ordinary launcher operation. Future development requires manually re-enabling TV debugging and reconnecting. Known Home smoothness and coverage limitations remain documented above.
