## ADDED Requirements

### Requirement: Catalog failure is reported honestly

When the Cinemeta catalog cannot be reached, the search screen SHALL
say the catalog is unavailable instead of claiming no titles match.
`CinemetaClient.searchCatalog()` SHALL return a Result/sealed outcome
that distinguishes failure from an empty result, and
`MatrixSearchRepository.cinemetaSearch()` SHALL propagate it.

#### Scenario: Both catalog calls fail
- **Given** the movie and series catalog calls both fail
- **When** the search completes
- **Then** the screen shows "Catalog unavailable — check connection"
  and does not show the no-results line.

#### Scenario: One catalog call succeeds
- **Given** one catalog call succeeds and the other fails
- **When** the search completes
- **Then** the successful call's results are shown, as today.

#### Scenario: Genuine empty result
- **Given** both catalog calls succeed with no matching titles
- **When** the search completes
- **Then** the existing no-results copy is shown.

### Requirement: No change to successful-search behaviour

Caching, ranking, and recents behaviour for successful searches SHALL
be unchanged.
