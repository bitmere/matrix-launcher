# search-posters

## MODIFIED Requirements

### Requirement: The second Cinemeta meta fetch runs only when the poster or year is missing
In `resolveCinemeta`, after the catalog search has matched a title, the
follow-up meta fetch SHALL run only when the poster or the release year is
still missing. When only the IMDb rating is missing, the fetch SHALL be
skipped and the card SHALL be built without the rating (the SIMKL
cross-reference may still supply a rating).

#### Scenario: Catalog match has a poster and a year but no rating
- **WHEN** the catalog search matches a title and supplies its poster and year
  but no IMDb rating
- **THEN** no second meta fetch is made for that title, and the card is built
  from the catalog poster

#### Scenario: Catalog match has no poster
- **WHEN** the catalog search matches a title but supplies no poster
- **THEN** the follow-up meta fetch still runs to obtain the poster

### Requirement: Returned cards are otherwise unchanged
Titles, order, links, ratings when present, and SIMKL references SHALL be
identical to before this change. Only the redundant round-trip is removed.
