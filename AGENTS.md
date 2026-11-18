# Matrix Launcher Compose workflow

- This is an independent repository. Do not edit or depend on sibling Java checkouts to implement features.
- Keep the Apple-style Home, shared Matrix colors/type/card/spacing roles, TV Material 3, deterministic D-pad behavior and optional integrations.
- core is Android-free and reusable. app contains Android adapters and Compose feature screens. Keep I/O out of composition and the main thread; avoid duplicate clients/caches or a DI framework for simple wiring.
- Do not reintroduce Classic Views UI, RecyclerView, AndroidView wrappers, seasonal canvas engines or unrelated upstream addons. Preserve backend/device safety policies needed by supported features.
- Version comes from version.properties. Increase versionCode for each newly installed artifact. Betas remain prereleases; stable publication needs a user request.
- Collect edits before building. Use Preview for iteration, focused tests then the full suite. Run build.ps1 -Checks once for the final optimized artifact. Do not edit application/core sources during builds.
- Verify UI by XML/D-pad first, screenshots for visual accuracy. Underlying Home XML is not dialog evidence on TCL. A passing JVM suite is not proof of remote or account integration.
- Install and publish exact measured optimized release bytes; verify installed/downloaded hashes. Never publish an unminified preview. Preserve the Java app's accounts/preferences and selected Home helper unless a takeover is explicitly authorized.
- Credentials, SDK paths, signing material, account/device dumps, receipts and screenshots stay in ignored artifacts. Preserve MIT, font and icon attribution.

- All settings options/commands, header navigation and toolbar buttons use MatrixButton/MatrixControlStyle. Roles may add commit icons or destructive color, not separate gray/rectangular styles. Form fields and loading placeholders reuse its geometry/tone. Keep media/artwork geometry separate from control styling. Run tools/audit_structure.py to prevent raw TV Button styling returning in feature screens.

- Icon-only controls must use MatrixIconButton (TV Material IconButton), not a text MatrixButton forced into a square. It shares MatrixControlStyle but has centered icon layout. Verify IconLayout offsets in --matrix-ui after layout settles and use screenshots/pixel bounds for actual glyph centering.

- Text-based settings rows share one leading text edge. Do not add an isolated leading icon to one row. Standalone action labels inherit the shared MatrixButton typography; use MatrixRowText for setting title/detail hierarchy.

- New app code/tests use com.matrix.launcher. Keep the four minimal com.gathin.tvhome service bridges and the legacy Home alias stable for Android approvals/shortcuts. Preserve upstream copyright/provenance; do not rename installed application ID or preference keys for branding.
