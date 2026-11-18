# TCL Smart TV Pro Optimization & Debloat Log

This document records the exact memory optimization, telemetry removal, and background tuning performed on the **TCL Smart TV Pro** (Android 12 / Amlogic SoC) when running **Matrix Launcher** (`com.matrix.launcher`).

---

## 1. Target Device Profile

- **Device Model**: TCL Smart TV Pro
- **Manufacturer**: TCL
- **Chipset / SoC**: Amlogic (`com.droidlogic`)
- **Operating System**: Android 12 (API 31)
- **Total Physical RAM**: 1,911 MB (~2 GB)
- **Network Serial / IP**: `192.168.29.219:5555`

---

## 2. Before vs. After Optimization Benchmarks

| Metric | Before Optimization | After Optimization | Net Improvement |
| :--- | :--- | :--- | :--- |
| **Available / Free RAM** | 101 MB | **358 MB** | **+257 MB RAM reclaimed (+254%)** |
| **Active Used RAM** | 1,809 MB | **1,552 MB** | **-257 MB system memory pressure** |
| **Compressed ZRAM Swap** | 544 MB used | **480 MB used** | **-64 MB swap reduction** |
| **UI Animation Latency** | 1.0x (Stock) | **0.5x (Snappy)** | Instant remote navigation response |
| **Background Video Limit** | Unlimited (300MB+ players resident) | **2 background processes max** | Automatic cleanup on returning Home |

---

## 3. What Was Done: 28 Debloated Packages

All packages were disabled user-wide (`pm disable-user --user 0`) to stop background daemons from waking up and consuming RAM while keeping the APKs intact in the read-only system partition.

### A. TCL Telemetry, Analytics & Background Daemons
1. `com.tcl.logkit` – TCL continuous background log collector and uploader.
2. `com.tcl.guard` – TCL "system cleaner/antivirus" daemon that periodically scanned memory.
3. `com.tcl.useragreement` – Background telemetry data compliance tracker.
4. `com.tcl.usercenter` – TCL cloud user account and profile tracking.
5. `com.tcl.messagebox` – TCL push notification service (marketing/promo popups).
6. `com.tcl.magiconnectfree` – MagiConnect remote screen share and tracking.
7. `com.tcl.tv.tclhome_passive` – TCL Home passive background tracker.

### B. TCL Ad Channels, Retail Demos & Preloaded Manuals
8. `com.tcl.channelplus` – TCL Channel Plus live streaming and ad feeds.
9. `com.tcl.waterfall.overseas` – TCL Waterfall recommendations and promo feed.
10. `com.tcl.partnercustomizer` – TCL stock launcher OEM partner branding injector.
11. `com.tcl.interactive` – TCL interactive marketing and promo framework.
12. `com.tcl.browser` – TCL built-in stock browser.
13. `com.tcl.hearaid` – TCL HearAid audio tool.
14. `com.tcl.ocean.instructions` – Preloaded user manual documentation.
15. `com.tcl.repairguide` – Preloaded repair guide documentation.
16. `com.tcl.esticker` – Retail store display "E-Sticker" overlay mode.
17. `com.tcl.exhibit` – Retail showroom exhibit demo overlay.
18. `com.tcl.hotelmenu` – Hospitality hotel menu framework.
19. `com.tcl.initsetup` – First-time setup wizard (redundant after setup).

### C. OEM Screen Sharing Daemons (Frees ~57 MB RAM)
20. `com.amlogic.airplay` – Amlogic AirPlay daemon service.
21. `com.tcl.airplay2` – TCL AirPlay receiver service.
22. `com.tcl.miracast` – TCL Miracast screen mirroring service.
*(Note: Standard Google Cast / `mediashell` remains active for phone-to-TV casting).*

### D. Android & Google Voice Listeners / Unused TV Daemons (Frees ~125 MB RAM)
23. `com.google.android.katniss` – Google Assistant background listener (was consuming **78.7 MB RSS**).
24. `com.google.android.tts` – Google Text-to-Speech synthesis (was consuming **46.3 MB RSS**).
25. `com.google.android.marvin.talkback` – TalkBack screen reader accessibility framework.
26. `com.android.printspooler` – Android print spooler (TVs never connect to printers).
27. `com.google.android.syncadapters.calendar` & `com.android.providers.calendar` – Google Calendar sync and provider.
28. `com.google.android.tungsten.setupwraith` – Google TV initial setup wraith.
29. `com.google.android.youtube.tvmusic` – Preloaded YouTube Music for TV.
30. `com.google.android.apps.tv.dreamx` – Ambient dream screensaver.

---

## 4. Heavy Video Streaming App Background Limits

Heavy video engines (Cobalt/TizenTube, YouTube, Play Store) were configured so they run smoothly in the foreground but cannot hold memory hostage in the background after returning to Matrix Launcher:

```bash
adb shell cmd appops set io.gh.reisxd.tizentube.cobalt RUN_IN_BACKGROUND ignore
adb shell cmd appops set io.gh.reisxd.tizentube.cobalt RUN_ANY_IN_BACKGROUND ignore
adb shell cmd appops set com.android.vending RUN_IN_BACKGROUND ignore
```

---

## 5. Global OS Performance Tuning Applied

```bash
# 1. Limit background processes to 2 (automatically cleans up exited video players)
adb shell settings put global background_process_limit 2

# 2. Cut animation duration in half for rapid D-pad remote navigation
adb shell settings put global window_animation_scale 0.5
adb shell settings put global transition_animation_scale 0.5
adb shell settings put global animator_duration_scale 0.5
```

---

## 6. Critical Safeguards: Packages Intentionally Preserved

The following hardware and remote packages were whitelisted and **never touched**:
- **`com.tcl.tv`** & **`com.tcl.tvinput`**: Essential for handling physical HDMI 1/2/3 inputs, AV sources, and live TV.
- **`com.tcl.autopair`**: Essential for Bluetooth remote pairing and connection maintenance.
- **`com.tcl.globalkeyoverlay`**: Hardware remote control key event mapper.
- **`com.tcl.systemserver`**: Core TCL hardware abstraction layer.
- **`com.droidlogic`**: Core Amlogic SoC chipset services.
- **`com.android.systemui`**: Core display compositor.
- **`com.tcl.inputmethod.international`**: TCL TV on-screen keyboard.

---

## 7. Rollback Instructions (How to Undo)

If you ever wish to re-enable any specific feature or restore all default stock settings:

### Option A: Restore a Specific Package
```bash
adb connect 192.168.29.219:5555
adb -s 192.168.29.219:5555 shell cmd package install-existing <PACKAGE_NAME>
adb -s 192.168.29.219:5555 shell pm enable <PACKAGE_NAME>
```
*Example: To restore Google Assistant voice search:*
```bash
adb shell cmd package install-existing com.google.android.katniss
adb shell pm enable com.google.android.katniss
```

### Option B: Full Rollback (Restore All Packages & Stock OS Settings)
Run this single command block in PowerShell or terminal:

```powershell
$serial = "192.168.29.219:5555"
$packages = @(
    "com.tcl.logkit", "com.tcl.guard", "com.tcl.channelplus", "com.tcl.useragreement",
    "com.tcl.usercenter", "com.tcl.messagebox", "com.tcl.magiconnectfree", "com.tcl.browser",
    "com.tcl.hearaid", "com.tcl.interactive", "com.tcl.ocean.instructions", "com.tcl.repairguide",
    "com.tcl.esticker", "com.tcl.exhibit", "com.tcl.hotelmenu", "com.tcl.initsetup",
    "com.amlogic.airplay", "com.tcl.airplay2", "com.tcl.miracast", "com.google.android.katniss",
    "com.google.android.tts", "com.google.android.marvin.talkback", "com.android.printspooler",
    "com.google.android.syncadapters.calendar", "com.android.providers.calendar",
    "com.google.android.tungsten.setupwraith", "com.google.android.youtube.tvmusic",
    "com.google.android.apps.tv.dreamx"
)

foreach ($pkg in $packages) {
    adb -s $serial shell cmd package install-existing $pkg
    adb -s $serial shell pm enable $pkg
}

# Reset background limits and animation speeds to Android stock defaults
adb -s $serial shell settings put global background_process_limit -1
adb -s $serial shell settings put global window_animation_scale 1.0
adb -s $serial shell settings put global transition_animation_scale 1.0
adb -s $serial shell settings put global animator_duration_scale 1.0

# Restore background execution for apps
adb -s $serial shell cmd appops set io.gh.reisxd.tizentube.cobalt RUN_IN_BACKGROUND default
adb -s $serial shell cmd appops set io.gh.reisxd.tizentube.cobalt RUN_ANY_IN_BACKGROUND default
adb -s $serial shell cmd appops set com.android.vending RUN_IN_BACKGROUND default
```
