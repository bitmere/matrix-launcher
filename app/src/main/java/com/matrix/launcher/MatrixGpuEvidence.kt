package com.matrix.launcher

/** Process-local evidence. A failed or incomplete run never grants FULL. No model allowlist. */
internal class MatrixGpuEvidence {
    private var frames = 0
    private var failed = false
    private val blurSamples = mutableListOf<Long>()
    val baselineReady get() = !failed && frames == baselineCount
    val qualified get() = baselineReady && blurSamples.size == probeCount
    fun frame(totalNanos: Long, gpuNanos: Long, dropped: Int) {
        if (failed || frames == baselineCount) return
        if (totalNanos !in 1..baselineLimit || gpuNanos !in 1..baselineLimit || dropped != 0) failed = true
        else frames++
    }
    fun probe(completionNanos: Long): Boolean {
        if (!baselineReady || completionNanos !in 1..probeLimit) { fail(); return false }
        if (blurSamples.size < probeCount) blurSamples.add(completionNanos)
        return true
    }
    fun fail() { failed = true; blurSamples.clear() }
    companion object {
        const val baselineCount = 120
        const val probeCount = 24
        const val baselineLimit = 8_000_000L
        const val probeLimit = 4_000_000L
    }
}
