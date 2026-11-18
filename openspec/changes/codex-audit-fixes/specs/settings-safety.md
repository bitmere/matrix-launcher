# Portable settings and lifecycle safety

## MODIFIED Requirements

### Requirement: Portable backups are bounded and schema validated (F01, F03, F11, F12)
Imports SHALL cap actual bytes at 512 KiB, settings at 1024, sets at 512 entries,
keys/set text at 512 characters, and string values at 32768 characters. Known
portable keys and supported dynamic prefixes SHALL require exact live types and
bounded numeric values before any write. Credentials and local bookkeeping SHALL
not be imported/exported. Persistence failures SHALL be reported, including a
possible theme-only restore when preference commit fails.

#### Scenario: Wrong typed setting after a valid one
- WHEN a backup supplies a String for safe_mode, idle_clock_minutes, sources or list_columns
- THEN the entire restore is rejected and existing settings/theme remain intact.

### Requirement: Secrets require deliberate reveal (F02, F22)
Every secret editor SHALL start masked and offer a labeled eye-icon MatrixIconButton
for a session-only reveal/hide. Removing a provider key SHALL remove all its model
keys and the legacy fallback without removing other providers' keys.

#### Scenario: Reopen editor
- WHEN the user reveals a key, closes the editor, and opens it again
- THEN it starts hidden; no saved reveal preference is used.

### Requirement: Theme imports preserve a known-good file (F10, F12, F23)
Theme reads SHALL enforce a 16 KiB byte cap including startup reads. Imports and
edits SHALL validate before staging, fsync and atomic replacement. Failed saves
SHALL not recreate Home. Startup SHALL load off Main with default tokens initially;
artwork shape SHALL follow current radius in the same process.

#### Scenario: Interrupted document read
- WHEN a provider fails during import or exceeds the byte cap
- THEN the previous theme remains unchanged and the failure is displayed.

### Requirement: SAF ownership ends with its Activity/page (F24)
Destroying an Activity SHALL clear its pending result and global launch closures
only when it is still their owner. Page disposal SHALL cancel that page's owner
callback. A recreated Activity SHALL not replay an obsolete page callback.

#### Scenario: Old Activity destroyed after replacement
- WHEN the new Activity has registered its launchers
- THEN destruction of the old Activity does not clear the replacement launchers.
