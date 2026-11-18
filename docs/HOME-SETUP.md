# Home setup and rollback

Installation does not enable a helper or change default Home. In Settings → Home & remote, try the ordinary default-Home picker first. Some Google TV firmware reports success but continues opening Google TV; verify with the real Home button.

For accessibility capture, turn off the previous launcher's override, enable Capture Home, then select Matrix Launcher · Home control in Accessibility. Do not run two Home interceptors together. Keep the stock launcher available. Test repeated real Home presses, D-pad and held-volume behavior. Some TCL firmware drops held-volume repeats while accessibility key filtering is enabled; the tested TV retains capture with that tradeoff.

Namespace note: implementation classes now use com.matrix.launcher. Legacy service IDs remain intentionally registered so an update preserves the selected Home helper; see [component compatibility](NAMESPACE.md). Do not rename Android secure-setting entries manually.

## Startup recovery

Recovery is optional and requires an explicit grant:

```text
adb shell pm grant com.matrix.launcher android.permission.WRITE_SECURE_SETTINGS
```

Enable recovery through Matrix's Home setup. It restores the accessibility master switch only while this service remains selected; it does not re-add a helper the user disabled. Do not infer successful startup from a checked toggle: reboot and verify actual Home/binding.

On the tested TCL, boot logs reported OP_AUTO_START denial for the Compose package. This permission does not transfer from the Java app. Only where firmware supports it and logs confirm the cause, record the previous mode before an authorized change:

```text
adb shell cmd appops get com.matrix.launcher AUTO_START
adb shell cmd appops set com.matrix.launcher AUTO_START allow
adb shell cmd appops write-settings
```

Reboot-test actual startup. Undo restores the recorded mode (`ignore` on the tested TCL). These are vendor-specific operations, not universal installation steps.

## Rollback

Disable Matrix Home capture/recovery, restore the previous default/override in system settings, and uninstall this edition if desired. The Java edition's account data remains separate. Revoke the optional secure-settings grant if no longer needed. Keep one chosen Home helper active.

See [current device evidence](VALIDATION.md) and [historical checks](archive/VALIDATION-HISTORY.md). No recent UI-only update repeats all physical-key/reboot verification.
