package com.matrix.launcher

import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.HardwareRenderer
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/** No trial blur in a visible window. Collect normal frames, then probe only after Home stops.
 * Evidence is deliberately not persisted: process, display, power and thermal changes close the gate.
 */
internal class MatrixGpuQualification(
    private val activity: Activity,
    private val owner: LifecycleOwner,
    private val eligible: Boolean,
    private val homeVisible: () -> Boolean,
    private val onResult: (Boolean) -> Unit,
) : LifecycleEventObserver {
    private val evidence = MatrixGpuEvidence()
    private val foreground = AtomicBoolean(true)
    private var probeJob: Job? = null
    private var attempted = false
    private var listener: Window.OnFrameMetricsAvailableListener? = null
    private val started = SystemClock.uptimeMillis()
    private val metrics = activity.resources.displayMetrics
    private val width = metrics.widthPixels
    private val height = metrics.heightPixels
    private val density = metrics.density
    private val power = activity.getSystemService(PowerManager::class.java)
    private var powerSafe = !power.isPowerSaveMode
    private var thermalSafe = false
    private var powerReceiver: BroadcastReceiver? = null
    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    init {
        // Image fences are required for evidence of completed GPU work (API 33).
        // API 31/32 stay STATIC rather than accepting submission time as GPU time.
        if (eligible && Build.VERSION.SDK_INT >= 33 && width in 1..1920 && height in 1..1080 &&
            activity.getSystemService(ActivityManager::class.java).memoryClass >= 256) {
            thermalSafe = power.currentThermalStatus == PowerManager.THERMAL_STATUS_NONE
            owner.lifecycle.addObserver(this)
            val callback = Window.OnFrameMetricsAvailableListener { _, frame, dropped ->
                if (foreground.get() && homeVisible() && SystemClock.uptimeMillis() - started >= 5_000) {
                    if (!safeEnvironment()) {
                        revoke(); return@OnFrameMetricsAvailableListener
                    }
                    val total = frame.getMetric(FrameMetrics.TOTAL_DURATION)
                    val gpu = frame.getMetric(FrameMetrics.GPU_DURATION)
                    if (evidence.qualified) {
                        // Revoke immediately on a bad frame; never retry this process.
                        if (total !in 1..16_000_000L || gpu !in 1..8_000_000L || dropped != 0) revoke()
                    } else if (!attempted) evidence.frame(total, gpu, dropped)
                }
            }
            try {
                activity.window.addOnFrameMetricsAvailableListener(callback, Handler(Looper.getMainLooper()))
                listener = callback
                val thermal = PowerManager.OnThermalStatusChangedListener { status ->
                    thermalSafe = status == PowerManager.THERMAL_STATUS_NONE
                    if (!thermalSafe) revoke()
                }
                power.addThermalStatusListener(activity.mainExecutor, thermal)
                thermalListener = thermal
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        powerSafe = !power.isPowerSaveMode
                        if (!powerSafe) revoke()
                        if (intent.action == Intent.ACTION_SCREEN_OFF) tryProbe()
                        if (intent.action == Intent.ACTION_SCREEN_ON && probeJob?.isActive == true) revoke()
                    }
                }
                val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED).apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                }
                activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                powerReceiver = receiver
            } catch (_: RuntimeException) { revoke() }
        }
    }

    private fun safeEnvironment() = powerSafe && thermalSafe &&
        activity.resources.displayMetrics.let { it.widthPixels == width && it.heightPixels == height && it.density == density }

    private fun revoke() { evidence.fail(); attempted = true; probeJob?.cancel(); onResult(false) }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (Build.VERSION.SDK_INT < 33) return
        when (event) {
            Lifecycle.Event.ON_START -> {
                foreground.set(true)
                powerSafe = !power.isPowerSaveMode
                thermalSafe = power.currentThermalStatus == PowerManager.THERMAL_STATUS_NONE
                if (probeJob?.isActive == true) revoke()
                if (!safeEnvironment()) revoke()
            }
            Lifecycle.Event.ON_STOP -> {
                foreground.set(false)
                tryProbe()
            }
            Lifecycle.Event.ON_DESTROY -> {
                foreground.set(true)
                probeJob?.cancel()
                listener?.let { activity.window.removeOnFrameMetricsAvailableListener(it) }
                thermalListener?.let(power::removeThermalStatusListener)
                powerReceiver?.let(activity::unregisterReceiver)
                owner.lifecycle.removeObserver(this)
            }
            else -> Unit
        }
    }

    @RequiresApi(33)
    private fun tryProbe() {
        if (!foreground.get() && !power.isInteractive && !attempted && evidence.baselineReady && safeEnvironment()) {
            attempted = true
            probeJob = owner.lifecycleScope.launch {
                try {
                    delay(1_000) // Screen must stay off; never compete with another visible app.
                    check(!power.isInteractive)
                    val samples = withTimeout(4_000) { withContext(Dispatchers.Default) { measureBlur() } }
                    if (foreground.get() || power.isInteractive || !safeEnvironment()) { revoke(); return@launch }
                    samples.forEach { evidence.probe(it) }
                    onResult(evidence.qualified)
                } catch (cancelled: CancellationException) { evidence.fail(); onResult(false); throw cancelled }
                catch (_: Exception) { revoke() }
                catch (_: OutOfMemoryError) { revoke() }
            }
        }
    }

    @RequiresApi(33)
    private suspend fun measureBlur(): List<Long> {
        // Two full-display buffers, no Bitmap, pixel readback, decode, or source capture.
        // Full-display blur is intentionally much larger than the bounded header effect.
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2,
            HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT)
        val renderer = HardwareRenderer()
        val node = RenderNode("Matrix offscreen qualification")
        try {
            renderer.setSurface(reader.surface)
            renderer.setContentRoot(node)
            node.setPosition(0, 0, width, height)
            node.setRenderEffect(RenderEffect.createBlurEffect(18f * density, 18f * density, Shader.TileMode.CLAMP))
            val paint = Paint()
            val samples = ArrayList<Long>(MatrixGpuEvidence.probeCount)
            repeat(MatrixGpuEvidence.probeCount) { index ->
                currentCoroutineContext().ensureActive()
                check(!foreground.get() && !power.isInteractive)
                val begin = System.nanoTime()
                val canvas = node.beginRecording()
                try {
                    canvas.drawColor(Color.BLACK)
                    // Change source content so cached effect output cannot pass the test.
                    repeat(256) { tile ->
                        paint.color = Color.rgb((tile * 73 + index * 17) and 255, (tile * 31) and 255, (index * 53) and 255)
                        val x = tile % 16 * width / 16f
                        val y = tile / 16 * height / 16f
                        canvas.drawRect(x, y, x + width / 16f, y + height / 16f, paint)
                    }
                } finally { node.endRecording() }
                check(renderer.createRenderRequest().setWaitForPresent(false).syncAndDraw() == HardwareRenderer.SYNC_OK)
                var image = reader.acquireLatestImage()
                while (image == null && System.nanoTime() - begin < MatrixGpuEvidence.probeLimit) {
                    delay(1)
                    image = reader.acquireLatestImage()
                }
                checkNotNull(image).use { output ->
                    output.fence.use { fence ->
                        check(fence.isValid && fence.await(Duration.ofMillis(4)))
                    }
                }
                val elapsed = System.nanoTime() - begin
                check(elapsed in 1..MatrixGpuEvidence.probeLimit)
                samples.add(elapsed)
                delay(100) // One outstanding submission; at most 24, never an unbounded benchmark.
            }
            return samples
        } finally {
            renderer.destroy()
            node.discardDisplayList()
            reader.close()
        }
    }
}
