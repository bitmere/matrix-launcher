# Proposal — tv-document-fallback

## Why
Some TVs have no activity for opening or creating documents. Unguarded picker
launches can crash backup and theme import. The wallpaper gallery also conflates
image-index failures with missing permission and uses newer permission names on
older Android versions.

## Scope
- Guard the actual OpenDocument/CreateDocument contract intents with resolution,
  narrow package visibility queries, and launch exception handling.
- Distinguish unavailable/denied launches from user cancellation; clean callbacks
  on failure and page/activity disposal.
- Keep the MediaStore gallery primary and add permission-free Choose image,
  copying immediately through the existing 10 MiB / 20-import pipeline.
- Separate gallery permission, permanent denial, empty index, failed query and
  unreadable-image messages, with API-specific permission labels.
- Add Save on this TV and Restore from this TV, using app-owned backup directories,
  the existing versioned JSON codec and 512 KiB limit, and a bounded file list.
- Show the real saved path and warn that uninstall removes these files.
- Add plain JVM tests and document device verification gates.

## Non-goals
No dependencies, SDK changes, version bump, broad storage permission, generic
filesystem browser, local theme browser, or changes to core, legacy components,
preference keys, backup schema, MediaStore browsing or incoming image sharing.
Optional backup sharing is deferred to keep this change focused on picker-free
save and restore. No Gradle, compilation, installation, commit, push or PR.
