## ADDED Requirements

### Requirement: One bounded live Home header
Home SHALL render the existing header as a floating sibling above the captured
background (gradient or wallpaper, dim, list). The header SHALL NOT capture itself.
Its single Haze blur SHALL use an 18dp radius. Existing icon glass gradients,
labels, actions and focused treatments SHALL remain unchanged.

#### Scenario: Rows scroll behind the header
- WHEN an eligible TV scrolls Home rows
- THEN the header remains fixed and blurs changing content behind its bounds
- AND list top padding reserves 34dp + measured header height + existing 100dp gap
- AND row reveal excludes the header's occupied region.

### Requirement: Capability fallback
Live blur SHALL require SDK >= 31, safe mode off and low-RAM false. Existing
animation/wallpaper decisions SHALL remain unchanged. No experimental path is enabled.

#### Scenario: Unsupported or cheap device
- WHEN the SDK is below 31 or safe mode/low-RAM is active
- THEN Home attaches neither a Haze source nor a Haze blur
- AND the floating header retains its static icon gradients and sharp content.

### Requirement: Remote behavior
The header SHALL precede rows in focus traversal. Existing initial-focus and Back
requesters SHALL retain their targets. Header/favorites focus SHALL return the list
to its start. Header controls SHALL preserve their horizontal/up exit restrictions.

#### Scenario: Return to the top group
- WHEN focus returns to header or favorites
- THEN the list scrolls to item zero with the reserved top space intact.
