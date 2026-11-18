> The local configurable-media preview uses this as its Nuvio adapter. For Stremio, selection and native channel behavior, see [media app setup](MEDIA-APPS.md).

> Release status: these tested fixes are included in stable 1.1.0. Preview labels and measurements below describe the original validation runs.

# Shared Nuvio content handoff - beta 56

Home channels, SIMKL previews/full lists and Search all enter MatrixHomeApp.activate and ComposeHomeRepository.mediaIntent. App icons use appIntent and resume normally.

## Reproduced failure and compatibility route

Installed Nuvio 1.1.0-beta.10 (1073) received different public meta links while still showing the old title. Its MainActivity navigates metadata links with launchSingleTop=true; MetaDetailsViewModel captures item identity at construction. This matches the observed stale detail state. Task clearing was tested and rejected because it reopened the profile picker; the previously authorized HYD home profile was restored once. No task reset or profile automation is shipped.

NuvioLinks parses bounded typed public title routes. One Android adapter resolves Nuvio's exported public handler and sends a fresh contentId/contentType handoff with launchMode=stream. In this Nuvio version, absence of videoId/name takes the fresh detail branch, without launching a stream. Detail requests omit the URI and old playback extras to prevent double routing. This is a version-tested interoperability fallback, not a guaranteed future API.

Native Continue Watching stream requests keep their video/episode/resume extras and explicit warm-delivery flags. Other publishers retain their routing behavior. Foreign components, selectors, clip data, grants and task-clearing flags are not forwarded. The plain Nuvio app icon remains normal app resume. Detail handoffs can retain normal Nuvio navigation history. No force-stop, private database access or account/settings reset.

## Validation

133 JVM tests pass, including title-parser rejection/roundtrip cases and delivery policy. Final optimized beta 56 installed with matching SHA256. Actual Matrix cards changed Inception to The Batman, then Batman: The Animated Series; a SIMKL Home card then opened Aadarsha Kutumbam House No: 47. The exact SIMKL -> Home -> Search -> The Batman sequence also passed. Dumps guarded Matrix selection; screenshots verified Nuvio's actual title. No further profile picker or Play selection.

One repeat harness assumed Home reset row focus, but Home retained row 2. Its Up/Left/Center sequence reached an unintended native Nuvio entry; automation stopped and returned to Matrix. No Play button was selected, but this accidental publisher handoff is not a controlled playback test. The harness now verifies row zero before entering Search. A subsequent result-settle timeout was recorded separately; the guarded exact-sequence rerun passed. Continue Watching context preservation is code-reviewed; full playback is not certified by these detail tests.

Private dumps/source snapshots/images stay in artifacts/nuvio56. Final preview TV state was Matrix; automatic input stopped. Beta 56 was not separately published; this fix is included in stable 1.1.0.

## Tagged upstream source

- [MainActivity handoff](https://github.com/NuvioMedia/NuvioTV/blob/1.1.0-beta.10/app/src/main/java/com/nuvio/tv/MainActivity.kt)
- [Detail ViewModel identity](https://github.com/NuvioMedia/NuvioTV/blob/1.1.0-beta.10/app/src/main/java/com/nuvio/tv/ui/screens/detail/MetaDetailsViewModel.kt)
- [Public handler declaration](https://github.com/NuvioMedia/NuvioTV/blob/1.1.0-beta.10/app/src/main/AndroidManifest.xml)
