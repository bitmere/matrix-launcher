# Run 2 review and coordinator handoff

## Implementation diff

- `ComposeHomeActivity.kt`: observable capability decision fed by measured GPU evidence.
- `MatrixGpuEvidence.kt`, `MatrixGpuQualification.kt`: closed-by-default qualification,
  passive baseline, bounded offscreen completed-work probe, lifecycle cleanup/revocation.
- `MatrixGlassSurface.kt`, `MatrixSurfacePolicy.kt`: window-aligned shared frost and
  token fills; explicit FULL/live, STATIC/frost, CHEAP/solid header selection.
- `MatrixHomeSurface.kt`: header adopts tokens without changing position, measurement,
  focus order, reveal policy or content padding. Disable live capture while obscured.
- `NowPlayingCard.kt`: shared frost, .72 base/.08 ambient tint; remove redundant local
  decode/repaint/RenderEffect and hardcoded blue. Playback commands unchanged.
- `MatrixAppearance.kt`, `MatrixBackdrop.kt`: one panel material, same frost and fill
  for scroll-edge sampling. Panel-local provider remains; synthetic panel generation
  is no longer used. Window coordinates stay local to each window.
- `MatrixDialogBlur.kt`, `MatrixHomeScreen.kt`: runtime support subscription and disposal,
  radius in pixels, always-present panel fallback; Home visibility gates its sweeps.
- `MatrixFocusMotion.kt`, `MatrixCards.kt`, `MatrixButton.kt`: scale spring 600/1/.00075,
  glow spring 700/1 and displayed-alpha clamp; static state for CHEAP/reduced motion.
- `MatrixGlassSource.kt`: refresh generation after foreground trim; stop still suspends.
- Two test files: 11 new plain JVM test methods. No tests executed in this run.

## TV Material evidence

Inspected the **pinned 1.1.0 AAR** using `javap -c -p` (inspection only, no compilation).
Its `SurfaceScale.kt` bytecode calls a private `defaultScaleAnimationSpec`: focus 300ms,
unfocus 500ms, press 120ms, release/cancel 300ms, all tweens. `CardDefaults.scale`
has three targets; default base and pressed targets are both 1, focused is overridden
by Matrix's theme value. `ClassicCard`, `Button` and `IconButton` expose `interactionSource`. Pinned ButtonDefaults
also uses a pressed/resting target of 1; shared buttons use the same spring seam.

The dependency source archive was absent from the cache; fetching it failed because
sandbox egress could not connect to its proxy. Upstream source inspection agreed with
the private tween structure, but was not the pinned release. This is an explicit
source-inspection deviation: pinned **binary** evidence was used, not an assertion
that a different branch is 1.1.0 source. No library source was copied or vendored.

All three library targets are now fixed at 1. One app-owned scale follows the same
interaction source, keeping the theme focus target and pressed/resting target of 1.
There is no multiplication by an independently animated library scale. Card click,
long-press interceptor (380ms), focus navigation and layout are unchanged. No fluid
candidate was added: there is no existing nonpersistent selection surface for it.

## Impeccable source passes

Used user-level Impeccable 4.10.0 because `.agents/skills/impeccable/SKILL.md` was
missing. Loaded context, craft floor, native Android audit and polish guidance.
Preserved the explicit Apple-style Home / TV Material control brief.

- Audit: controls retain labels and shared geometry; focus border remains 1.5dp;
  no navigation, IME, settings semantics or playback command changes. Native Dialog
  remains the host. No new UI I/O or per-frame bitmap work.
- Polish: removed hardcoded blue and duplicate Now Playing blur; aligned panel edge
  sampling with the actual panel material; cached material computation; kept existing
  geometry and typography. Header layout/contentPadding arithmetic is unchanged.
- Motion/hardening: normal-motion sweep gates now include obscured Home; lifecycle
  already gates background activity. CHEAP/reduced focus transitions snap. Support
  changes and missing frost cannot remove the solid/readable fill.
- Detector: 10 touched UI Kotlin files scanned, exit 0, JSON `[]`. Kotlin extension
  uses text/regex rules; this is **not** measured rendered contrast or native behavior.
  The added pure policy file contains no rendered UI. Context reported setup drift;
  no setup files were repaired as part of this task.

P1 acceptance remains open: native rendering, final composite contrast (including
.78 secondary text and imported themes), glyph pixel centering, rapid D-pad
retargeting, pressed/release behavior, panel boundaries on TCL, tunneled video,
blur availability changes, exact GPU memory/residency and frame timing.
No native captures were generated: this run forbids compilation/installation and
hands device gates to the coordinator. No visual score or rendered pass is claimed.

## Coordinator review fix

The 65-second baseline deadline permanently disqualified healthy TVs that sat idle
on Home, because baseline evidence only accumulates when the window draws frames.
Removed that deadline so navigation can complete the baseline however long after
launch it happens, while the device stays STATIC until qualification passes. All
other safety gates, sample counts and timing limits, the next-screen-off probe,
and permanent revocation behavior remain unchanged.

## Checks performed

- `python3 tools/audit_structure.py`: passed, 17 manifest components, core Android-free.
- `git diff --check`: passed after whitespace cleanup.
- Impeccable detector: no text-rule findings, with the limitations above.
- No Gradle, compilation, test execution, install, commit, push or PR.

The pre-existing Run 1/toolchain working-tree changes were retained. This document
is the reviewable diff summary before coordinator compilation; it does not authorize
build, release or Git publishing actions beyond the user's separate coordination.
