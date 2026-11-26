# Matrix Launcher workflow

- Matrix Launcher 2.7.14 is published in the public bitmere/matrix-launcher
  GitHub repository with no mirrors. The updater reads GitHub Releases through
  the GitHub API. GitHub Actions signs releases with the bitmere keys held in
  Actions secrets.
- Repo rulesets block direct pushes to main. Every change, including version
  bumps, goes through a PR with green CI and a squash merge. Never merge,
  release, push, or open a PR without explicit user approval for that exact
  action. Show the planned diff for review before compiling and address the
  review before starting the build.
- The renamed application ID is `com.matrix.launcher`. Use that namespace for
  new app code and tests. Preserve the four legacy service bridges and Home
  activity alias declared in the manifest for Android approvals and shortcuts.
  Keep the application ID and preference keys stable. Read the version from
  `version.properties` and increase versionCode for each newly installed
  artifact. Keep betas as prereleases.
- Keep the Apple-style Home, TV Material 3, shared Matrix color, type, card,
  and spacing roles, deterministic D-pad behavior, and optional integrations.
  Keep classic Views, RecyclerView, AndroidView wrappers, seasonal canvas
  engines, and unrelated upstream add-ons out of the UI.
- Keep `core` Android-free and reusable. Keep Android adapters and Compose
  screens in `app`. Keep I/O out of composition and off the main thread.
  Use simple wiring and shared clients and caches. Preserve supported
  functionality and device safety policies while keeping the footprint small.
  Keep the repository independent of sibling checkouts.
- Use MatrixButton and MatrixControlStyle for settings options and commands,
  header navigation, and toolbar buttons. Use shared control geometry and
  tones for form fields and loading placeholders. Roles can add commit icons
  or destructive color within the shared style. Keep media and artwork geometry
  separate. Run `tools/audit_structure.py` to catch raw TV Button styling in
  feature screens.
- Use MatrixIconButton (TV Material IconButton) for icon-only controls. It
  shares MatrixControlStyle and centers the icon. Check IconLayout offsets in
  `--matrix-ui` after layout settles and verify glyph centering with screenshot
  pixel bounds.
- Align text-based settings rows on a shared leading text edge. Keep isolated
  leading icons out of those rows. Use shared MatrixButton typography for
  standalone actions and MatrixRowText for settings titles and details.
- Collect edits before building and keep app and core sources stable during
  builds. Use Preview for iteration, focused tests first, then the full suite.
  `scripts/build.ps1 -Mode Check` selects local JVM tests and lint.
  `scripts/build.ps1 -Checks` selects the optimized release checks. Use the
  GitHub Actions release workflow to build and verify the signed artifact.
- Verify TV behavior through XML and D-pad first and use screenshots for
  visual checks. On TCL, inspect the dialog itself. Verify remote and account
  integration on-device. Install and publish the exact measured optimized
  release APK and verify both downloaded and installed hashes.
- Keep credentials, SDK paths, signing material, account and device dumps,
  receipts, and screenshots in ignored artifacts. Preserve upstream
  provenance, MIT credits, and the font and icon attributions in
  `THIRD_PARTY_NOTICES.md`.
- Use OpenSpec for user-facing changes. Include a proposal, tasks, and specs
  under `openspec/changes/<change>/`. Take design inspiration from GPL-licensed
  sources, but write all code fresh.
- Impeccable design skill: linked from `vendor/impeccable` (submodule) into
  `.agents/skills/impeccable`; product context in `PRODUCT.md`, design system
  in `DESIGN.md`. Run the Impeccable review passes on UI changes and the
  detector before calling UI work done. The design hook (`.codex/hooks.json`,
  `.impeccable/config.json`) covers `.kt` files via `detector.extensions`.
  Skill resolution order: project-local skills first, then user-level skills,
  then the ai-config repo index as reference only; never use a stale skill.
