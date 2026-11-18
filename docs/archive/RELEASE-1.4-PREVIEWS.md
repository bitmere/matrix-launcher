# 1.4 preview — not published

Clear translucent Home controls and dock lighting retain wallpaper detail. Shared control tokens provide a restrained reflective edge; focused artwork keeps one light rim with a softer halo. Existing TV Material focus scaling, icon geometry and D-pad routing remain intact. A shared artwork-coloured gradient revolves around the focused card/control border every four seconds, with soft outward plumes. App accents and media palettes reuse existing caches; no additional image decoding. Card artwork reserves space for the glow within unchanged row slots. It stops when focus leaves or the activity is no longer resumed. Shader/paint/matrix objects are cached, with animation values read in drawing only. Android 13+ uses a bounded single-pass glow; older Android retains the stroke fallback. No live blur or new runtime dependency. The focused border intentionally remains animated while Home is visible.

Home reuses its favorites mapping until the app/favorites lists change. The profiling script now refuses to start if Matrix is not foreground. No production subsystem was proven unused; none was removed. Current docs link to archived release and cleanup history.

## Evidence and limits

[Validation](VALIDATION-1.4-BETA6.md) records a new 1.3 baseline and the installed preview comparison. This is not a claim of improved Home smoothness. Composition/layout and buffer waits remain investigation targets. Artwork requests already use sizing/caching; return refresh, backdrop quality and Home key filtering are unchanged. Automatic wallpaper rotation limits strict visual matching between samples. Palette-background transitions need a separate controlled scenario.

Accounts, favorites, channels, wallpaper preferences, Home capture/recovery, service identities and licenses are retained. No integration was enabled. Synthetic D-pad tests do not certify physical held-volume behavior; its known tradeoff remains.

## Design references

- [Apple materials](https://developer.apple.com/design/human-interface-guidelines/materials): clear material over rich media, with content visible through controls. The Home backdrop already supplies dimming.
- [Apple focus and selection](https://developer.apple.com/design/human-interface-guidelines/focus-and-selection): predictable directional focus and recognizable focused state.
- [Meet Liquid Glass](https://developer.apple.com/videos/play/wwdc2025/219/): lightweight forms and edge light. This Android treatment is a low-cost approximation, not Apple's real-time refraction renderer.
- [Android AGSL](https://developer.android.com/develop/ui/views/graphics/agsl/using-agsl): cached runtime shaders drawn directly on Canvas without a live background blur.
- [Compose performance](https://developer.android.com/develop/ui/compose/performance/bestpractices): reuse derived work and measure before broader changes.
