# App labels (cache)

## ADDED Requirements

### Requirement: Cache app labels, bounded
`TvFeed.appName` SHALL cache resolved labels in a bounded LRU (128 entries)
instead of calling PackageManager for every row on every Home load.

#### Scenario: Same package read twice in one Home load
- WHEN the label for a package was already resolved
- THEN the cached label is returned with no PackageManager call.

#### Scenario: A package changes
- WHEN the app list is invalidated (package broadcast)
- THEN the label cache is cleared, so a renamed or updated app is re-read.
