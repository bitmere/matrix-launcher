package com.matrix.launcher

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/** One bounded loader for visible Compose cards and focused-artwork sampling. */
class MatrixComposeApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("compose-images")).maxSizeBytes(64L * 1024 * 1024).build() }
        .allowRgb565(true)
        .crossfade(false).build()

    /** Under memory pressure the in-memory images go; the disk cache re-serves them. */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_RUNNING_LOW) Coil.imageLoader(this).memoryCache?.clear()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Coil.imageLoader(this).memoryCache?.clear()
    }
}
