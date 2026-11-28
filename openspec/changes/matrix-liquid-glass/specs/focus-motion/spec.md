# focus-motion

## Purpose
Replace the current eased focus tweens with spring motion in the ColorOS 17
"Fluid Motion" sense: movement follows velocity, interruptions mid-flight
continue smoothly instead of restarting, and elements settle with a physical
feel rather than a fixed-duration slide. No numbers from OPPO were found in
the reviewed official sources — the values below are Compose `spring()`
values proposed by us, to be validated on the TV.

## Current state (verified in code)
- Focus glow alpha: `animateFloatAsState` with `tween(160ms,
  FastOutSlowInEasing)` (`MatrixCards.kt:425–457`) — **eased, not linear**,
  and not capability-gated.
- Focus scale: TV Material 3 `CardDefaults.scale(focusedScale = 1.015)` —
  the library's own animation; the pinned tv-material 1.1.0 scale internals
  and their customization seam are **not confirmed** from app source alone.
  No `spring(` call exists anywhere in app main Kotlin source (that does
  not prove there are no implicit springs in Compose/library defaults).
- Backdrop crossfade: `tween(320)`.
- Search entrances: a shared AnimatedContent transition (`tween(220,
  delayMillis = 40)`), **not** demonstrated per-card staggering.
- Glow sweep: finite 12s rotation (1080° / 12000ms, linear, RESUMED-gated),
  then holds still. A stopped sweep animator does not prove Home draws 0
  frames at rest (clock, playback, loading state may still update).
- Motion is **not** comprehensively capability-gated: sweeps and wallpaper
  are gated by `MatrixCapabilities.animations`, but fades, scales, and the
  orb/loading loops are not.

## ADDED Requirements

### Requirement: Shared focus-motion contract
Matrix SHALL implement the following requirements across the staged runs.
- Focus scale and glow alpha animate with springs, not tweens. Starting
  values (ours to validate on the TV):
  - focus scale: conservative default `spring(stiffness = 500–700f,
    dampingRatio = 1.0f)`; keep `spring(stiffness = 400f, dampingRatio =
    0.8f)` as the "fluid" candidate. Damping 0.8 is underdamped: in an
    ideal zero-velocity second-order step it yields ~1.5% overshoot of the
    *delta*, not of total card size — for a 0.015 delta that is ~0.00023
    extra scale. Retargeted velocity can change the response; do not
    promise no visible overshoot for every theme.
  - glow alpha: `spring(stiffness = 700f, dampingRatio = 1.0f)` (sensible;
    not necessarily faster than the present 160ms tween — critical damping
    at stiffness 700 reaches ~95% in 180ms, ~99% in 250ms for an ideal
    normalized step; actual end time depends on thresholds and velocity).
  - Choose the scale visibility threshold explicitly around 0.0005–0.001
    (a generic float threshold is large relative to the 0.015 scale
    change). Clamp displayed alpha to [0, 1]. Verify rendered settling
    and no delayed focus indication.
- Inspect the pinned tv-material 1.1.0 scale implementation before
  replacing scale behaviour; `CardDefaults.scale` configures target scales,
  not a spring spec. **Do not layer a new graphics scale on top of the
  library scale.** Preserve press behaviour and user focus-scale values.
- Springs must be interruptible: a D-pad press mid-animation retargets from
  the current value and velocity (springs preserve velocity continuity on
  retarget; tweens also support interruption but may change velocity
  abruptly). Verify on-device that rapid focus moves do not stutter or snap.
- **Keep the 320ms backdrop crossfade as a fade** unless an actual defect
  justifies changing it — alpha does not need elastic motion. Keep the
  bring-into-view policy (`BringIntoViewSpec`) and the 380ms long-press
  threshold in `tvCardClickable` unchanged.
- Entrances: stagger 20–30ms is a **candidate, not confirmed vendor
  guidance**. Stagger only the initial visible groups; never delay focus,
  input, or every streamed result. Cap total delay at 120ms; the focused
  destination appears immediately. Stable item identity prevents animation
  restarts on streamed results.
- The 12s glow sweep is preserved **only in the normal-motion tier**, paused
  when obscured/backgrounded, disabled under reduced motion.
- A remote gives discrete events, not finger velocity — use state/animation
  velocity, never invented "gesture velocity" from D-pad events. "No linear
  motion anywhere" is not a useful rule: **linear rotation stays correct
  for the orb.**
- Motion respects the capability tier: CHEAP disables sweeps and falls back
  to instant state changes; STATIC keeps springs but no live blur. Existing
  orb/loading loops are gated before any new effect is added. System
  Reduce Motion is respected; the animation-duration-disabled system
  setting is honoured.
- D-pad focus must stay deterministic: motion never changes focus order,
  focus restoration, or the 380ms long-press timing.

## Delivery boundary
Run 1 implements source/policy/token foundations only. Run 2 adopts these on
surfaces and performs rendered/device validation; existing settings behaviour,
1.5dp focused borders and navigation remain intact. Persistent in-app motion
and transparency preferences are explicitly out of scope.

#### Scenario: Unmeasured device
- GIVEN a device without a measured GPU qualification
- WHEN selecting material quality
- THEN use STATIC wallpaper frost, or CHEAP for safe mode, low RAM,
  allocation failure or a nonpersistent low-effects choice
- AND do not enable live blur merely because API 31 is available.

## Run 2 implementation

Focus scale uses spring(stiffness=600, dampingRatio=1, visibilityThreshold=.00075).
Glow uses spring(stiffness=700, dampingRatio=1), with displayed alpha clamped [0,1].
CHEAP/reduced motion uses snap. animateFloatAsState retargets its animation state;
no focus-event Animatable is recreated or snapped under normal motion.

Pinned TV Material 1.1.0 binary inspection found private scale tweens, no public
animation-spec seam. Set CardDefaults and shared ButtonDefaults scale targets to 1, then let a single
Matrix scale modifier observe the library's shared MutableInteractionSource.
The theme focusedScale and library pressed target of 1 are preserved. See
run2-review.md for the source-archive access limitation. No copied dependency code.

The fluid 400/.8 candidate is not added: there is no nonpersistent selector, and
adding a preference is out of scope. The default is critically damped. Backdrop
320ms fades, linear orb rotation, bring-into-view and 380ms long press are unchanged.
The 12s linear glow sweep requires FULL+normal motion, RESUMED lifecycle, focus and
unobscured Home. Opening a Dialog cancels Home's sweep. Search entrances are deferred.
