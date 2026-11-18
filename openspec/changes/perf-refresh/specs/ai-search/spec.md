# AI search capability — delta spec

## MODIFIED Requirements

- WHEN an AI search resolves its recommendations, THE SYSTEM SHALL
  dedupe repeated titles (by normalized title) before resolving them.
  (D-02)

- WHEN an AI search resolves titles, THE SYSTEM SHALL memoize
  Cinemeta / TMDB lookups within that one search (small cache, max 64
  entries), so a title the AI suggests twice is resolved once. (D-02)

- THE SYSTEM SHALL return the same titles in the same order as before
  the change — only the number of network calls changes, never the
  results. (D-02)

- Regex patterns on search hot paths SHALL be compiled once as
  constants, not constructed on every call. (D-04)

## Non-goals

- Capping how many recommendations get resolved — that changes visible
  results and is deferred until the dedupe + memoize win is measured.
- Any change to AI providers, models, prompts, or the result ranking.
