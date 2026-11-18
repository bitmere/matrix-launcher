# backup

## ADDED Requirements

- THE SYSTEM SHALL export settings as versioned JSON
  (`{"format":"matrix-backup","version":1,"prefs":{…},
  "theme":{…}|null}`) and import the same format.

- THE EXPORT SHALL include every entry of the "personal_home"
  SharedPreferences EXCEPT secrets (any key starting with
  `ai_api_key`, or containing `token`, `password`, or `secret`),
  plus the theme.json contents when present.

- THE IMPORT SHALL validate format and version, write the prefs
  (never importing a secret key even if one is present in the file),
  write theme.json when present, then recreate the activity and
  report how many settings were restored.

- THE TRANSPORT SHALL be the Storage Access Framework
  (ACTION_CREATE_DOCUMENT for export, ACTION_OPEN_DOCUMENT for
  import, MIME application/json, default name
  `matrix-backup-YYYYMMDD.json`). No new permissions.

- THE SYSTEM SHALL provide a Backup page in the settings registry,
  reachable from the Settings root page, with Export and Import
  actions.

- THE BACKUP codec SHALL be pure (String in/out, no Context) and
  covered by JVM-only unit tests.

## Non-goals

- No new permissions. No cloud backup, no account, no automatic
  scheduled backup.
- Secrets are never exported and never imported. The SIMKL account
  token (stored in the separate `simkl_v2` prefs) is out of scope;
  the user re-links SIMKL after a reinstall.
