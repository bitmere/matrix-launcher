# Matrix Launcher Compose 2.0.0

This release combines the tested Home preview work with focused settings and Continue Watching refinements. It upgrades the existing Compose installation without clearing accounts or preferences.

- Home keeps translucent controls/dock and adds artwork-coloured, rotating outward focus glow. Android 13+ uses a bounded single-pass glow; older Android retains a compatible fallback. Motion stops when focus leaves or the activity is paused.
- Settings retain shared TV Material controls. Long explanations wrap fully; unfocused controls avoid constructing glow shaders. Menu/form identity mappings are retained across recompositions. Existing checkbox/radio state semantics remain intact.
- Continue Watching uses a slim 3dp translucent progress track without an opaque backing or outline. Cards expose the watched percentage while retaining their title and normal D-pad action.
- Shared measured rows/grids, whole-card slots and focus gutters remain. The expanded glow has reserved artwork space and a footprint regression test. No broad layout rewrite, framework migration or runtime dependency.
- Current docs are shorter; preview/release evidence is archived. No production subsystem or test was proven obsolete, so useful coverage and compatibility code remain.

## Validation and limits

See [the final build, installed checksum and TV evidence](../VALIDATION.md). Home smoothness is **not claimed fixed**: composition/layout and render-buffer waits remain. Continuous focused glow adds rendering work. Automatic wallpaper rotation limits strict before/after comparisons. The older Android glow fallback, all display/font sizes and a full TalkBack session were not device-tested.

Accounts, favorites, channels, wallpaper choices, Home capture/recovery, service identities and licenses are preserved. No integration is enabled by installation. Home key filtering retains its known held-volume repeat tradeoff. No playback engine, live background blur or background account-sync service is added.

## References

[Android TV focus](https://developer.android.com/design/ui/tv/guides/styles/focus-system), [Compose accessibility semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics), [Compose performance](https://developer.android.com/develop/ui/compose/performance/bestpractices), [Android AGSL](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl), [Apple materials](https://developer.apple.com/design/human-interface-guidelines/materials).
