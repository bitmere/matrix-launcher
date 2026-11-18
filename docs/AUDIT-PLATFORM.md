# Platform audit coverage

Reviewed all platform source files in the current Compose repository, retaining framework callbacks and optional device behavior rather than treating absence of ordinary callers as dead code.

| Files | Review result |
| --- | --- |
| AppLaunchActivity.java, AppManagement.java, HomeRequest.java, InputsRequestActivity.java, ProbeActivity.java | Explicit launcher routing, system-confirmed package management, HOME/history consumption and manifest entry points retained. No incoming arbitrary Intent forwarding. |
| HomeKeyService.java, InputKeyService.java, GamepadPassthrough.java, VolumeKeys.java | Reviewed capture/redirect paths, remote mapping order, key-filter opt-in, held-key cleanup and service lifecycle. Retained tested routing and hardware compatibility behavior. Removed only two uncalled power-menu methods from HomeKeyService. |
| RemoteKeys.java | Reviewed key/scancode parsing, duplicate-helper press ownership, hold/release cancellation and launch targets. Removed unused Xiaomi menu-release policy/constant and sole test; no event path called it. |
| HomeRecovery.java, ServiceRepair.java | Reviewed explicit selection, secure-settings permission checks and bounded/rechecked recovery writes. Retained different semantics: selected-helper master-switch repair versus opt-in force-stop recovery. No repair policy changed. |
| GuardConfig.java, GuardKeep.java, GuardRevival.java | Reviewed permission-gated ADB opt-in, direct-boot handling and standby recovery. Retained manifest/system callbacks; not dead code. |
| RemoteConfig.java, OpenOnStart.java | Preserved bounded remote overlay loading and legacy preference exclusion. Removed OpenOnStart's unused boot/network/app-launch implementation and obsolete tests; sole used key moved into RemoteConfig. |
| ThemeFiles.java, ThemeProvider.java | Restricted provider to consumed config.json. Removed wallpaper, collection-delete, captions and revision paths with no production reader. Permission-gated provider retained for ADB remote configuration. Updated path rejection test. |
| SystemSettings.java | Fixed package-only fallback checks: require the exact enabled/resolvable activity/application. Vendor order remains unchanged; AOSP fallback shares the check. No vendor activity is launched during this review. |
| ComposeHomeActivity.kt | Reviewed lifecycle jobs, observer registration/debounce, foreground requests, permission flow and diagnostics. No activity behavior change by this reviewer. |
| MatrixRuntime.kt, MatrixComposeApplication.kt | Reviewed callback registration/cleanup and single bounded Coil loader. Retained connection-versus-internet semantics and shared cache. |

Also reviewed AndroidManifest.xml component visibility/permissions/queries; app/build.gradle and proguard-rules.pro; accessibility metadata, config/branding/string/design/style values and backup/data-extraction XML. The opaque platform window theme wraps a Compose TV Material theme and is not evidence of a legacy View screen. Exported ThemeProvider/GuardConfig remain gated by WRITE_SECURE_SETTINGS; platform services require their binding permissions. No permissions or default-Home settings were altered.

Corresponding tests reviewed: AppManagementTest, GamepadPassthroughTest, HomeKeyServiceTest, HomeRequestTest, RemoteConfigTest, RemoteKeysTest, ThemeFilesTest, VolumeKeysTest and the removed OpenOnStartTest. No new mock-only test was added for PackageManager resolution: existing JVM infrastructure cannot execute real Android package resolution. Parent build/lint and device checks remain the validation boundary; no build or TV operation was performed by this reviewer. This is source review, not proof that every manufacturer's remote/standby path has been exercised.
