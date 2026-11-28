# Backup

## ADDED Requirements

### Requirement: Picker-free local backups
Backup settings SHALL retain SAF export/import and always offer Save on this TV
and Restore from this TV. Missing picker launches SHALL show “This TV has no file
picker” and identify local backup alternatives. Theme import SHALL report the
same unavailable outcome without a local theme browser.

#### Scenario: Local save and restore
- GIVEN a TV with or without a document picker
- WHEN Save on this TV is chosen
- THEN Matrix encodes the existing versioned JSON with secret filtering
- AND writes to external app files/backups, or internal files/backups if the
  external parent is null or the external directory cannot be created
- AND never constructs a path from a null parent
- AND uses a unique filename, a synced temporary write and rename
- AND displays the actual absolute path with the notice:
  “Removed if Matrix is uninstalled; copy this file elsewhere to keep it.”
- WHEN Restore from this TV is chosen
- THEN Matrix lists at most 32 regular JSON files from the app-owned backup
  roots, without recursion, ordered deterministically by filename/path
- AND selection uses existing settings rows with stable path IDs
- AND read/decode/apply uses BoundedInput, the 512 KiB bound, existing schema
  validation and partial-restore failure reporting, off the main thread.

#### Scenario: Failure preserves completed backups
- GIVEN malformed/oversized input, unavailable storage or a full disk
- WHEN saving or restoring
- THEN Matrix reports failure and leaves prior completed backups intact
- AND failed writes leave no temporary or incomplete JSON file
- AND normal SAF cancellation never creates a fallback file.

#### Scenario: Restart or storage availability changes
- GIVEN saved backups and a restarted launcher
- WHEN listing local backups
- THEN completed files remain discoverable without an in-memory receipt
- AND both external and internal backup roots are considered
- AND files outside those roots and symbolic-link entries are not accepted.
