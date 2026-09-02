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
    val db by lazy {
        try {
            AppDatabase.get(this)
        } catch (e: Exception) {
            // Database init failed - this is fatal but we try to recover
            // by deleting the corrupted database
            try {
                deleteDatabase("aile_takip.db")
                AppDatabase.get(this)
            } catch (e2: Exception) {
                throw RuntimeException("Cannot initialize database", e2)
            }
        }
    }

    /**
     * Global Coil ImageLoader with memory + disk caching.
     * Used for any future URL-based images.
     */
    override fun newImageLoader(context: coil3.PlatformContext): ImageLoader {
        return try {
            ImageLoader.Builder(context)
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
        } catch (e: Exception) {
            // Fallback: basic image loader without caching
            ImageLoader.Builder(context).crossfade(true).build()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            if (level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE) {
                BitmapCache.clear()
                ComputedCache.clear()
            }
        } catch (e: Exception) {
            // Cache clear failed, ignore
        }
    }
}
