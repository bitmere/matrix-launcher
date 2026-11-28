# Spec — gpu-qualification (API 31/32 enablement)

## Requirements

- The GPU qualification SHALL run on API 31 and above (previously 33+), keeping the existing bounds: display width 1–1920, height 1–1080, memoryClass ≥ 256.
- The `onStateChanged` lifecycle observer SHALL NOT bail out on API 31/32 (previously `< 33` returned immediately).
- `tryProbe()` and `measureBlur()` SHALL be annotated `@RequiresApi(31)` (previously 33).
- On API 33+, the probe SHALL keep the existing fence completion wait: `output.fence.use { fence -> check(fence.isValid && fence.await(Duration.ofMillis(4))) }`.
- On API 31/32, the probe SHALL wait for GPU completion by touching the image planes: read `output.planes[0].buffer` and check its capacity is > 0. Rationale: the native lock behind `Image.getPlanes()` consumes the buffer's acquire fence (AOSP `ImageReader_createImagePlanes` passes the fence fd to `lockImageFromBuffer`), so the measured submit → planes-readable wall time still includes GPU completion, under the same 4 ms bound.
- On API 31/32, the probe SHALL run on a dedicated daemon single thread named `MatrixGpuProbe31`, closed in a `finally`, because the planes lock has no timeout and a wedged GPU must not pin a shared pool thread. On API 33+, the probe SHALL keep `Dispatchers.Default`.
- The broadcast receiver SHALL be registered with the 3-arg `registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)` overload on API 33+ only; on API 31/32 it SHALL use the 2-arg overload, because the 3-arg overload is API 33+ and calling it below 33 would throw `NoSuchMethodError` (an `Error`, not caught by the existing `RuntimeException` handler). The 2-arg fallback is safe: the registered broadcasts are system broadcasts (power-save, screen off/on) that no other app can send.
- The outer `withTimeout(4_000)`, the 120-frame baseline (GPU ≤ 8 ms, zero dropped), the 24-sample / 4 ms per-sample bound, and the revoke-on-any-bad-frame rule (permanent for the process) SHALL NOT change.
- `MatrixCapabilities.decide(sdkInt = 31, measuredGpu = true)` and `decide(sdkInt = 32, measuredGpu = true)` SHALL return FULL (already the case; locked in by the new JVM test `measuredGpuGetsFullOnApi31And32`).
- API 31 SHALL be treated as provisional: Haze recaptures blur input after any snapshot state change on API 31 only, and Home changes snapshot state every frame (clock + orb), so the recapture cost is unmeasured. This status SHALL be recorded in the code comment. Real-device validation on API 31 and API 32 is out of scope for this change (no device in this VM) and stays open.
- API ≤30 SHALL remain STATIC; this change does not alter that path.
