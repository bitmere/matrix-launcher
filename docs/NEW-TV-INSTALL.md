# Installing Matrix Launcher on a New Android TV / Google TV Using Any AI CLI

This guide provides step-by-step instructions for installing and configuring **Matrix Launcher** on a brand-new or secondary Android TV / Google TV device using any AI coding assistant CLI (Antigravity, Gemini CLI, Claude Code, Cursor, Codex CLI, GitHub Copilot CLI, Aider) or standard PowerShell/bash terminal.

## First bitmere installation

The first bitmere release (2.7.6) uses application ID `com.matrix.launcher` and signing certificate SHA-256 `a21fc7cb0cf12ab08744e03a84fc45be7fd50aafb7397f054d250ce1a5151b9b`. Install it manually as a clean installation. Earlier installations keep their settings in a separate app data sandbox. Keep a stock or alternate launcher available before removing an older installation. Later builds signed with the same bitmere key can update in place.

Signing keys and random passwords exist only in GitHub Actions secrets. Local builds cannot sign release artifacts; never retrieve/recreate keys, request passwords or substitute a debug/preview APK.

## Get the APK from GitHub Releases

1. Download `Matrix-Launcher.apk` from the [latest GitHub Release](https://github.com/bitmere/matrix-launcher/releases/latest).
2. Extract `Matrix-Launcher.apk` into an ignored local artifacts directory. Verify its SHA-256 against the `SHA-256:` line in the release notes (`sha256sum Matrix-Launcher.apk` and compare the hash, or compare `Get-FileHash -Algorithm SHA256` on Windows). Keep the checksum and exact APK bytes together; never rebuild or modify them for installation.
3. Check the run's signer/package/version verification. It must verify the full approved certificate fingerprint, `com.matrix.launcher`, intended version/code and non-debuggable optimized artifact.
4. Complete the approved clean-install transition first when the old signer is installed. Then use `adb -s <YOUR_TV_IP>:5555 install Matrix-Launcher.apk`; use `-r` only for subsequent compatible-signer updates. Verify the installed APK hash against the downloaded APK. The legacy helper below instead requires its matching local `artifacts/release-manifest.json`; the release checksum file alone is not that manifest.

---

## 1. Supported TV Devices & Hardware
- **Operating System**: Android TV 8.0 (Oreo / API 26) through Android 14+ / Google TV.
- **Tested Hardware**: TCL Google TV, Sony Bravia, Chromecast with Google TV (4K & HD), Nvidia Shield TV, Xiaomi Mi Box / TV, Onn 4K Pro, OnePlus TV, Hisense, Fire TV (with Developer Options / ADB enabled).
- **Architecture**: ARMv7 (`armeabi-v7a`), ARM64 (`arm64-v8a`), x86, x86_64.

---

## 2. One-Time Setup on the New TV

Before using your AI CLI or terminal, enable network debugging on the TV:

1. **Connect TV to Wi-Fi/LAN**: Ensure the TV is on the same local network as your computer.
2. **Enable Developer Options**:
   - On the TV, go to **Settings** $\to$ **System** (or **Device Preferences**) $\to$ **About**.
   - Scroll down to **Android TV OS build** (or **Build**).
   - Press the **OK / Center button** on your TV remote **7 times** until a toast appears: *"You are now a developer!"*.
3. **Enable Network / USB Debugging**:
   - Go back to **Settings** $\to$ **System** $\to$ **Developer options**.
   - Enable **USB debugging**.
   - Enable **Network debugging** (or **Wireless debugging** if shown as a separate toggle).
4. **Note the TV IP Address**:
   - Go to **Settings** $\to$ **Network & Internet** $\to$ select your connected Wi-Fi or Ethernet.
   - Note the assigned IPv4 address (e.g. `192.168.1.150` or `192.168.88.10`). The standard ADB port is `5555`.

---

## 3. Installing via Any AI CLI

You can install Matrix Launcher by typing natural language prompts into your AI CLI, or by executing the project's automated installation script.

### Option A: Using Natural Language in Your AI CLI
Simply open your AI assistant CLI in this repository directory and paste:

> *"Install Matrix Launcher onto my new TV at IP 192.168.1.150:5555 with full speed compilation."*

Or for a different device IP:
> *"Connect to Android TV at <YOUR_TV_IP>:5555 and deploy the latest release APK."*

### Option B: Running the Command Directly
Run the verified PowerShell installer script:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-compose-preview.ps1 -Serial <YOUR_TV_IP>:5555 -Compile
```

*Example for TV at `192.168.1.150`:*
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-compose-preview.ps1 -Serial 192.168.1.150:5555 -Compile
```

---

## 4. The One-Time TV Pairing Authorization

When the AI CLI or installer first connects to your TV, a prompt will appear on your TV screen:

> **"Allow USB debugging?"**  
> Fingerprint: `XX:XX:XX:...`  
> [X] **Always allow from this computer**  
> `[ CANCEL ]`  `[ OK ]`

1. Grab your **TV remote**.
2. Check the box **"Always allow from this computer"**.
3. Select **OK**.

*Note: If the installer times out while waiting for you to click OK, simply run or re-prompt the install command once authorized.*

---

## 5. What the Automated Installer Does

The project's installer (`scripts/install-compose-preview.ps1`) automates the full deployment pipeline:
1. **ADB Handshake**: Connects to `<TV_IP>:5555`.
2. **Release Artifact Verification**: Reads `artifacts/release-manifest.json`, locates its named APK under `artifacts/` and verifies the APK SHA-256. Stage the exact approved Actions APK and a matching manifest before using this helper.
3. **Safe Package Install**: Uses `pm install -r` to reinstall with data preserved only when signing certificates are compatible. It does not bypass the next-release signer transition or automatically uninstall the old app.
4. **ART Speed Compilation (`-Compile`)**:
   - Invokes `cmd package compile -m speed com.matrix.launcher` on the TV.
   - Precompiles all Kotlin and Compose bytecode directly into optimized native machine code (`dex2oat`).
   - Ensures fluid, stutter-free 60fps animations even on low-end TV SoCs.
5. **Warm Launch**: Starts `com.matrix.launcher/com.matrix.launcher.ComposeHomeActivity`.
6. **Hash Check**: Confirms the installed package matches the source release bytes.

---

## 6. Setting Matrix Launcher as Default Home on the New TV

### Standard Android TV
1. Press the **Home button** on your physical TV remote.
2. Select **Matrix Launcher** from the system launcher selection dialog.
3. Select **Always**.

### Google TV / TCL / Chromecast (Stubborn OEM Launchers)
Some Google TV firmware attempts to retain the stock Google TV launcher. You can set default Home directly via your AI CLI:

#### Method 1: Ask Your AI CLI
> *"Set Matrix Launcher as the default home launcher on the TV via ADB."*

Under the hood, the AI will execute:
```powershell
powershell -ExecutionPolicy Bypass -File tools/adb_tv.ps1 shell cmd package set-home-activity com.matrix.launcher/com.matrix.launcher.ComposeHomeActivity
```

#### Method 2: Accessibility Capture & Startup Recovery
If your TV firmware aggressively re-launches Google TV after waking from sleep:
1. **Open Matrix Launcher Settings**: Press **Up** $\to$ select the **Settings (gear)** icon $\to$ **Home & remote**.
2. **Enable Capture Home**: Select **Capture Home** $\to$ you will be directed to TV Accessibility settings.
3. **Turn On Service**: Select **Matrix Launcher · Home control** $\to$ toggle **Enable**.
4. **Optional Startup Recovery Grant** (ensures Home stays active after a cold reboot):
   Prompt your AI CLI:
   > *"Grant WRITE_SECURE_SETTINGS to Matrix Launcher for startup recovery."*
   
   Command executed:
   ```powershell
   powershell -ExecutionPolicy Bypass -File tools/adb_tv.ps1 shell pm grant com.matrix.launcher android.permission.WRITE_SECURE_SETTINGS
   ```

---

## 7. Verifying the New TV Installation

To verify that the launcher is running with optimal geometry and zero layout bugs, instruct your AI CLI:

> *"Run matrix-ui diagnostic dumpsys on the TV and check icon centering and status."*

Or run directly:
```powershell
powershell -ExecutionPolicy Bypass -File tools/adb_tv.ps1 shell dumpsys activity com.matrix.launcher --matrix-ui
```

### What to Verify in the Output:
- `MatrixUi page=home`
- `loading=false apps=XX rows=X homeConnected=true`
- `IconLayout All apps button=... offset=0.0,0.0` $\to$ Icon centering offsets must be `0.0,0.0`.
- `SearchUi available=CATALOG,SIMKL,AI` $\to$ Search providers ready.

---

## 8. Troubleshooting & FAQ

| Symptom | Cause | Solution |
| :--- | :--- | :--- |
| **`device unauthorized`** | TV pairing popup was not accepted. | Check TV screen, check "Always allow", press OK. Reconnect: `tools/adb_tv.ps1 connect <IP>:5555`. |
| **`unable to connect to <IP>:5555`** | TV went to sleep or IP changed. | Wake TV with remote. Check TV Settings $\to$ Network to verify current IP address. |
| **Google TV opens instead of Matrix** | OEM firmware ignores default home. | Enable Accessibility Capture: Settings $\to$ Home & remote $\to$ Capture Home $\to$ Enable "Matrix Launcher · Home control". |
| **Want to switch back to stock launcher** | Need rollback. | In Matrix Settings $\to$ Home & remote $\to$ disable Capture Home, or run: `tools/adb_tv.ps1 shell cmd package set-home-activity <stock_launcher_component>`. |
| **Complete uninstallation** | Removing launcher. | Run: `tools/adb_tv.ps1 uninstall com.matrix.launcher`. |

---

## 9. Debloating & Memory Optimization on New TVs

Budget and mid-range smart TVs (TCL, Realme, Xiaomi) ship with only 1GB–2GB RAM and preload heavy background loggers and ad channels.

- **TCL Smart TV Pro Optimization & Rollback**: See [TCL Smart TV Pro Optimization & Debloat Log](TCL-SMART-TV-PRO-OPTIMIZATION.md) for the 28 safe packages disabled, memory benchmarks (+257MB free RAM), and rollback script.
- **Universal Android TV Debloat Guide**: See `personal-home-tv/docs/TV-DEBLOAT-AND-OPTIMIZATION-GUIDE.md` and `tools/debloat-tv.ps1` for automated package auditing and debloating across other TV models.
