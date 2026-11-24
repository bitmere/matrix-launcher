# Updater capability — delta spec

## MODIFIED Requirements

- A missing or visibility-filtered saved session SHALL count as settled.
- A live session SHALL be abandoned before its saved preferences are cleared.
- After SecurityException from abandonment, settlement SHALL succeed only if
  a fresh existence check confirms the session is gone.
- Failed existence checks SHALL NOT count as absence. An initial failed check
  SHALL still attempt abandonment.
- Other abandonment failures or inconclusive rechecks SHALL retain preferences
  and prevent a competing install; abandonment errors SHALL propagate to the
  existing installer error handler.
- With no saved ID, abandonment SHALL make no session store calls.
- Clearing SHALL synchronously commit and remain scoped to the matching ID.
- Both confirmation-failure branches SHALL settle the callback ID using the
  same rules.
- Installer settlement failures SHALL log the saved ID and exception under
  MatrixUpdate while preserving the existing user-facing error text.
