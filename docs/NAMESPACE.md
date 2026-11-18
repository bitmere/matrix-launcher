# Matrix naming and Android update compatibility

Local 1.1.1-beta.5 moves the application namespace and implementation/test packages to **com.matrix.launcher**, alongside Android-free **com.matrix.launcher.core**.

Main code: app/src/main/java/com/matrix/launcher. JVM app tests: app/src/test/java/com/matrix/launcher. The Android resource namespace, theme name, active test commands, installation scripts and current tooling use Matrix. The repository/app/APK names remain Matrix Launcher.

## Credit versus identity

Gathin23/cleanyourtv remains named as an upstream source in PROVENANCE.md, the existing About credit and the preserved MIT copyright/permission notices. Outfit and icon attribution are retained. These credits are not the product name. Corrected the font appendix's obsolete launcher/ file prefix; the original copyright and permission text is retained unchanged.

## Stable identifiers retained deliberately

The release ID is **com.matrix.launcher** and the preview ID is **com.matrix.launcher.preview**. Android gives each ID a separate data sandbox. The first bitmere build requires a clean install.

Four tiny classes under com.gathin.tvhome preserve previously registered Home/Input accessibility, notification-listener and clock-dream component identities. They contain no business logic; each extends its Matrix implementation. Home repair uses one explicit compatibility component helper so selection/binding checks match Android's saved service name. The screensaver implementation is open solely to support that bridge.

The old Home/launcher Activity name is retained as an activity-alias targeting com.matrix.launcher.ComposeHomeActivity. Only the alias advertises HOME/LAUNCHER, preserving saved shortcuts/defaults without duplicate Home choices. New scripts launch the canonical Matrix activity directly. Previously configured launcher-settings/app-library actions remain accepted; new actions use the Matrix prefix. The protected guard action accepts its old prefix too.

Do not remove these bridges or rename the installed app ID as cosmetic cleanup. A future removal needs an explicit migration plan and permission/default/shortcut regression checks. Source history and archived reports can still contain original names.

## Validation

Final beta 5 (code 90) is installed and checksum-verified. All 141 JVM tests pass; lint has zero errors and the existing orientation warning. Before/after Android secure-setting/default-Home snapshots match exactly. Canonical Matrix Activity/actions, legacy Home alias/settings action, Inputs, Back, Home capture/recovery, SIMKL connection, Automatic media selection and seven-row Home pass scoped dump checks. Final Home rendering inspected. Automatic input stopped. No account/profile/permission toggle, playback or reboot. After automatic input stopped, the user confirmed repeated real Home presses, Inputs and Settings all work. This physical confirmation is recorded separately from the ADB checks. Historical beta validation; included in stable 1.2.0. Static audit resolves manifest components to qualified source classes, verifies the one Home alias and permission-protected service bridges. Regression tests cover legacy action parsing and the service inheritance/identity contract. Runtime evidence belongs in ignored artifacts/branding5, not the repository.
