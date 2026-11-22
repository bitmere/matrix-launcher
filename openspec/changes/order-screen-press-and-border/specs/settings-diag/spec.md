## ADDED Requirements

### Requirement: The size-selection chain is traceable in a diagnostic build

In a diagnostic build only, ONE OK press on the size page SHALL produce
a log line at each step of the update chain, under the single tag
`MatrixSettingsDiag`: the persisted value written, the settings-state
revision after the write (with the changed preference key), the revision
`MatrixHomeApp` observes, the effective columns and selected flags the
size page computes on rebuild, the selected flags of the menu entries
actually built, and the selected flags the menu actually composed with.
While the diagnostic is off, NOTHING SHALL be built, read, or logged
for it — detail strings are lazy and no `StateFlow.value` read happens
in composition. In a normal build behaviour SHALL be unchanged.

#### Scenario: Diagnostic build, one size press
- **Given** a diagnostic build with the diagnostic flag on
- **When** the user presses OK on one size option
- **Then** the log shows, in order, the written value, the revision bump
  with the changed key, the parent revision, the rebuilt selected flags,
  the built menu's selected flags, and the composed menu's selected flags

#### Scenario: Normal build
- **Given** a normal (non-diagnostic) build
- **When** the user presses OK on a size option
- **Then** no diagnostic log lines appear and the selection behaves
  exactly as before

### Requirement: The diagnostic deletes in one step

All diagnostic code SHALL live in one file (`MatrixSettingsDiagnostics.kt`)
plus one-line call sites marked `// DIAG-BUG2`. Deleting the file and
those lines SHALL remove the diagnostic completely.

#### Scenario: Diagnostic removal
- **Given** the diagnostic has served its purpose
- **When** the file and its marked call sites are deleted
- **Then** the app compiles and behaves as before the diagnostic existed
