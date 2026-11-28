# Tasks — tv-document-fallback

- [x] Read both diagnosis reports, existing OpenSpec examples, tests and audit rules.
- [x] Implement guarded shared SAF boundary and narrow intent queries.
- [x] Add Choose image using existing bounded wallpaper import.
- [x] Separate gallery permission, denial, empty, query and image-read failures.
- [x] Add bounded local backup save/list/read and settings actions.
- [x] Preserve SAF, share-import, codec, namespace, SDKs and core independence.
- [x] Add 21 plain JVM tests; update the existing SAF ownership fixture.
- [x] Review source for lifecycle, focus, layout, typography, simplification,
  clarity, hardening, polish and native accessibility.
- [ ] User diff review before any future compilation.
- [ ] Run focused DocumentLaunchTest and MatrixBackupFilesTest, then full JVM/lint.
- [ ] Verify real SAF contracts, URI grants and round trips with an Android handler.
- [ ] Verify no-handler, disabled handler, launch race, denial and cancellation on TV.
- [ ] Verify TCL dialogs through XML and D-pad; screenshots at settled layout,
  large fonts, long paths, empty/error states and focus after list changes.
- [ ] Verify optimized release artifact and hashes in a separately authorized round.

## Verification
- PASS: tools/audit_structure.py (17 manifest components; core Android-free).
- PASS: git diff --check.
- Impeccable detector: 2 changed UI files scanned, no regex findings; parsed
  element/contrast rules do not run on Kotlin. This is not rendered validation.

No Gradle, compilation, JVM tests or device commands were run, as requested.
The SAF round-trip test uses a fake provider and plain-value result seam; it
does not prove Android contract resolution, provider grants or remote usability.
The full-storage test injects a partial-write IOException; it does not fill a disk.

## Source-review notes
Settings actions retain shared Matrix controls, text-leading alignment and type.
Local files appear only after Restore from this TV; paths are details, with stable
IDs and deterministic order. Saved-path status uses fully wrapped status text.
No new icons, colors, shapes, effects or dependencies were added.

IO is dispatched through the existing wallpaper repository or Dispatchers.IO.
The local list retains at most 32 entries while iterating directory entries.
Failed picker launches clear callbacks without invoking user cancellation;
disposal drops page references but keeps an in-flight reservation until completion.
Optional backup sharing is omitted; existing incoming image sharing is unchanged.
Actual rendered contrast, D-pad focus and OEM dialog usability remain unverified.
