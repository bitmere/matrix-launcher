# Channels (per-channel program cap)

## ADDED Requirements

### Requirement: A full channel row skips further programs
While scanning `preview_program`, a channel row that already holds
`FeedRules.MAX_ITEMS` programs SHALL skip the duplicate scan and the
re-sort for any further program of that channel.

#### Scenario: Channel publishes more than 12 programs
- WHEN a row already holds 12 programs and another program for the same
  channel is scanned
- THEN no duplicate scan and no sort runs for that program, and the row is
  unchanged.

#### Scenario: Channel publishes 12 or fewer programs
- WHEN a row holds fewer than 12 programs
- THEN programs are added, deduplicated, and sorted exactly as before.
