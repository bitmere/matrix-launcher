# Search, Home and architecture

## MODIFIED Requirements

### Requirement: List submission replaces the form (F05)
Saving a numeric SIMKL list ID SHALL pop the form before pushing the list.

#### Scenario: Save then Back
- WHEN an ID is saved
- THEN the list is visible and Back returns to the SIMKL parent.

### Requirement: Search work is bounded and operation owned (F13, F14, F15, F16, F26)
One coroutine SHALL aggregate AI tool actions; UI progress SHALL be published on
Main with a current-operation check. Clear/replacement/disposal SHALL cancel and
invalidate old work, including errors, results and focus. App catalogue enumeration
SHALL run on IO, including invalidated-cache paths. AI response bodies SHALL cap at
1 MiB, model entries at 256, recommendations at 100, calls per turn at 16 and
concurrent tools at four. Write-only focus state SHALL be removed.

#### Scenario: Obsolete search finishes after Clear
- WHEN an old provider returns or emits progress after Clear
- THEN query/results/progress remain cleared and focus remains with the field.

### Requirement: Resolution preserves title identity and media kind (F20, F21)
Normalization SHALL retain Unicode letters/numbers with NFKC and locale-independent
case normalization, falling back to original text when needed. Blank titles SHALL
not match recommendations. Provider candidates SHALL match requested media kind
before title/year ranking; specified years SHALL not fall through to unrelated years.

#### Scenario: Distinct Japanese titles or movie/series namesakes
- WHEN titles share year/type or movie and series share title/year
- THEN distinct identities stay separate and the requested media kind is selected.

### Requirement: Home uses consistent snapshots and effective recovery state (F17, F18, F19)
SIMKL library synchronization SHALL serialize read/sync/write per type; activities
and index refreshes SHALL be single flight. Status rows SHALL derive from the cached
snapshot with deterministic assembly. The startup effective safe-mode value SHALL
reach Home, AI, custom feeds and columns, including intent-only recovery. Channel
ranking SHALL evaluate later scanned candidates while retaining a bounded top twelve.

#### Scenario: Concurrent watching and planned sections
- WHEN both sections need the same cold library type
- THEN one type fetch and one activities fetch supply both status views.

### Requirement: Web cache respects result count (F27)
Cache identity SHALL include the bounded requested count and all returns SHALL obey it.

#### Scenario: Three then eight, or eight then three
- WHEN the same query is requested with different limits
- THEN each response satisfies its requested limit without inheriting a truncated cache.

## ADDED Requirements

### Requirement: Structure guard and integration boundaries remain testable (F28, F30)
The structure audit SHALL support actual Gradle namespace assignment syntax,
index declared classes independently of filenames, and exempt only explicitly known
dependency components. Fixtures SHALL reject missing components. Regression suites
SHALL cover typed preference apply, actual SIMKL client concurrency and recovery
consumers. Physical Home/volume, account cancel/resume, AOT, D-pad and dialog evidence
SHALL be recorded separately from JVM coverage.
