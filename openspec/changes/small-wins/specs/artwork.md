# Artwork pipeline (memory pressure + decode cap)

## ADDED Requirements

### Requirement: Evict the image memory cache under pressure
The app SHALL clear Coil's memory cache when the system reports memory
pressure (`onTrimMemory` at `RUNNING_LOW` or above) and when it reports low
memory (`onLowMemory`). The disk cache SHALL be untouched.

#### Scenario: System trims memory
- WHEN the OS calls `onTrimMemory` with a level of `RUNNING_LOW` or higher
- THEN the Coil memory cache is cleared and the next visible image re-decodes
  from the disk cache.

### Requirement: Backdrop decodes at most 1920x1080
The Home backdrop image request SHALL decode at the view size in pixels,
capped at 1920 wide by 1080 tall.

#### Scenario: 1080p TV
- WHEN the screen is 1920x1080 or smaller
- THEN the backdrop decodes at the view size, exactly as before.

#### Scenario: 4K TV
- WHEN the screen is larger than 1920x1080
- THEN the backdrop decodes at 1920x1080 instead of the full screen size,
  and is upscaled for display.
