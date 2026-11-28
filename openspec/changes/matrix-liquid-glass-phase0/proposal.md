# Floating Home header live-blur spike

Prototype one bounded 18dp live blur using Haze 2.1.0 over the wallpaper,
dim and scrolling Home rows. Preserve toolbar actions, existing icon glass
gradients, focus requesters, Back handling and return-to-top behavior.

Only non-safe-mode, non-low-RAM devices on API 31+ are eligible. Older and
CHEAP devices keep existing static styling with no Haze source/effect.
No three-tier redesign, preference changes, versions, dialog blur, bitmap
capture, optical glass or other Phase 1 work is included.

Adoption is pending coordinator compilation against the pinned stack and
physical-TV performance, focus and visual gates. See report.md for API evidence.
