# Capabilities (new)

## ADDED Requirements

### Requirement: One capability decision for old and new TVs
The app SHALL decide once, at Home startup, which effects are allowed, from
safe mode and from whether the device reports itself low-RAM.

#### Scenario: Full effects on a capable TV
- WHEN safe mode is off and the device is not low-RAM
- THEN animations and wallpaper are both allowed.

#### Scenario: Cheap variant on an old or safe-mode TV
- WHEN safe mode is on OR the device is low-RAM
- THEN animations and wallpaper are both off.

### Requirement: Glow follows the capability
The focus-glow sweep SHALL run only when the capabilities allow animations;
otherwise the focused card holds its still glow (rotation 0, no animation).

#### Scenario: Low-RAM TV focuses a card
- WHEN animations are off and a card gains focus
- THEN no animation runs and Home draws 0 frames at rest.

### Requirement: Wallpaper follows the capability
The wallpaper refresh loop and the wallpaper-changed path SHALL run only when
the capabilities allow wallpaper; otherwise Home uses a plain background.

#### Scenario: Safe mode starts
- WHEN safe mode is on
- THEN no wallpaper is fetched or shown (unchanged behaviour, now decided
  through the capability object instead of ad-hoc safe-mode checks).

### Requirement: Defaults are full effects
Composition without a provided capability SHALL use full effects, so previews
and tests behave as on a capable TV.
