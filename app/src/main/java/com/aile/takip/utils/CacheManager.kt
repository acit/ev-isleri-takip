package com.aile.takip.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * LRU in-memory cache for decoded bitmaps.
 * Prevents re-decoding Base64 strings on every recomposition.
 * Max size: ~32MB (roughly 100 images at 800px width).
 */
object BitmapCache {

    private const val MAX_CACHE_SIZE = 32 * 1024 * 1024 // 32 MB

    private val cache = object : LruCache<String, Bitmap>(MAX_CACHE_SIZE) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    /**
     * Get a cached bitmap by Base64 key, or decode and cache it.
     * Decoding happens on the IO dispatcher to avoid blocking the UI thread.
     */
    suspend fun getOrDecode(base64: String, maxWidth: Int = 400): Bitmap? {
        if (base64.isBlank()) return null

        val key = md5(base64.take(200)) // Use first 200 chars as fast key

        // Return cached if available
        cache.get(key)?.let { return it }

        // Decode on background thread
        return withContext(Dispatchers.IO) {
            try {
                val bytes = Base64.decode(base64, Base64.NO_WRAP)
                val opts = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)

                // Calculate sample size for large images
                opts.inSampleSize = calculateSampleSize(opts.outWidth, opts.outHeight, maxWidth)
                opts.inJustDecodeBounds = false
                opts.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                if (bitmap != null) {
                    cache.put(key, bitmap)
                }
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Synchronous version for already-known-cached bitmaps.
     */
    fun getCached(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return cache.get(md5(base64.take(200)))
    }

    /**
     * Clear all cached bitmaps (e.g., on low memory).
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * Get current cache size in bytes.
     */
    fun cacheSize(): Int = cache.size()

    private fun calculateSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var sampleSize = 1
        if (width > maxDim || height > maxDim) {
            val halfWidth = width / 2
            val halfHeight = height / 2
            while (halfWidth / sampleSize >= maxDim && halfHeight / sampleSize >= maxDim) {
                sampleSize *= 2
            }
        }
        return sampleSize
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Computed values cache for expensive operations like date formatting, string processing.
 */
object ComputedCache {

    private val cache = LruCache<String, Any>(200)

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrCompute(key: String, compute: () -> T): T {
        val cached = cache.get(key)
        if (cached != null) return cached as T
        return compute().also { cache.put(key, it) }
    }

    fun clear() {
        cache.evictAll()
    }
}
