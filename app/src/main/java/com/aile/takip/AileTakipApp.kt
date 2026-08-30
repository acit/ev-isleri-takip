package com.aile.takip

import android.app.Application
import android.content.ComponentCallbacks2
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.disk.DiskCache
import coil3.request.crossfade
import com.aile.takip.data.db.AppDatabase
import com.aile.takip.utils.BitmapCache
import com.aile.takip.utils.ComputedCache
import okio.Path.Companion.toOkioPath
import java.io.File

class AileTakipApp : Application(), SingletonImageLoader.Factory {
    val db by lazy { AppDatabase.get(this) }

    /**
     * Global Coil ImageLoader with memory + disk caching.
     * Used for any future URL-based images.
     */
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25) // 25% of app memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "image_cache").toOkioPath())
                    .maxSizePercent(0.02) // 2% of device storage
                    .build()
            }
            .crossfade(true)
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
            BitmapCache.clear()
            ComputedCache.clear()
        }
    }
}
