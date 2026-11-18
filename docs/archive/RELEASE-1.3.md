# Matrix Launcher Compose 1.3.0

Focused maintenance release following 1.2.0; preserves the existing Home design and shared TV Material controls.

- Fix Search/List Back and initial focus when retained cards are hidden by loading or an error.
- Give empty saved-wallpaper pages a reachable initial Back focus.
- Reuse wallpaper album/filter/card mappings and list IDs across recompositions.
- Label wallpaper breadcrumbs explicitly and remove the obsolete native-picker package query.
- Stop cancelled wallpaper imports between stream reads and before decode/save; retain transfer limits and existing callers.

## Scope and limits

Two parallel reviews covered UI focus/navigation and backend import handling. This is a targeted review, not a claim that every project line or every TV state was exhaustively verified. No new feature, runtime dependency or permission. No measured Home-jank improvement is claimed. Cancellation cannot interrupt an OS-blocked read or an encode already in progress. Existing Home key-filtering/held-volume tradeoff remains.

The changes follow [Compose performance guidance](https://developer.android.com/develop/ui/compose/performance/bestpractices): reuse nontrivial derived collections and retain existing shared lazy layout keys/content types. Tests cover hidden results, breadcrumbs, cancellation during a read and exact/over-limit copies. Device validation is recorded in [VALIDATION.md](../VALIDATION.md).

## Release checks

148 JVM tests pass; lint has zero errors and one existing orientation warning. Optimized APK: 1,793,626 bytes; versionCode 94. SHA256 `4722597be2f1d903875ef7565d6302a6c24e9b8bf240676a1f9d658a33759fbe`. Exact installed bytes are published.
