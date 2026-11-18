# Top metas (parse limit)

## ADDED Requirements

### Requirement: Parse only the requested number of top metas
`CinemetaClient.getTopMetas` SHALL parse at most `limit` items from the
top-metas array; the live suggestions source SHALL request 2, the only items
its builder uses.

#### Scenario: Suggestions ask for top metas
- WHEN the suggestions builder asks for the top movies and series
- THEN at most 2 items per type are parsed and returned.

#### Scenario: Default callers
- WHEN no limit is passed
- THEN all items are parsed, as before.
