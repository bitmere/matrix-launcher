# Document access

## ADDED Requirements

### Requirement: Guarded document launch
Matrix SHALL derive the resolution intent from the registered contract's
createIntent, check an enabled default-capable handler, and catch
ActivityNotFoundException and SecurityException around preflight/launch.
OpenDocument SHALL add CATEGORY_OPENABLE in the contract used for both preflight
and launch. Queries SHALL cover OPEN_DOCUMENT's top-level */* MIME and JSON
CREATE_DOCUMENT without general package visibility.

#### Scenario: Missing, disappearing or denied handler
- GIVEN no resolving handler, a handler removed after preflight, or denied launch
- WHEN backup, theme or wallpaper requests a document
- THEN Matrix reports unavailable or denied explicitly and clears the callback
- AND no cancellation callback or automatic local save is triggered.

#### Scenario: Cancel or leave the page
- GIVEN a successfully launched picker
- WHEN the user cancels
- THEN the callback receives null once, with no failure notice
- AND page disposal drops the callback while reserving the outstanding flight
  until its result arrives, preventing delivery to a replacement page.
- WHEN activity ownership changes
- THEN cleanup from the old owner cannot clear the new owner's wiring.

### Requirement: Wallpaper access choices
MediaStore SHALL remain the primary remote-friendly browse option. Choose image
SHALL request image/* through guarded OpenDocument without a gallery permission.
Readable results SHALL be copied immediately by the existing bounded importer;
10 MiB input, 20 imported-image retention, sampled decoding and IO dispatch
SHALL remain unchanged. Shared-image importing SHALL remain available.

#### Scenario: Permission is absent or permanently denied
- GIVEN no gallery permission
- THEN Matrix offers permission access, App permissions and Choose image
- WHEN a denied request can no longer show a rationale
- THEN Matrix directs the user to App permissions instead of repeated requests
- AND the permission label is Storage through API 32 and Photos and videos on 33+.

#### Scenario: Empty or failed image index
- GIVEN granted image access, including selected-only access on API 34+
- WHEN the index returns no images
- THEN Matrix identifies the empty index and offers other import routes
- WHEN the query throws or returns no cursor
- THEN Matrix reports a query failure with retry, rather than claiming denial
- WHEN importing yields no readable image
- THEN Matrix identifies the unreadable image separately from gallery access.
