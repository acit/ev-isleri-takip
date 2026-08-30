package com.aile.takip.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object AttachmentHelper {

    /**
     * Compress and convert image to Base64 string
     */
    fun uriToBase64(context: Context, uri: Uri, maxWidth: Int = 800, quality: Int = 70): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val scaledBitmap = scaleBitmap(bitmap, maxWidth)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val bytes = outputStream.toByteArray()

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Convert camera bitmap to Base64 string
     */
    fun bitmapToBase64(bitmap: Bitmap, maxWidth: Int = 800, quality: Int = 70): String {
        val scaledBitmap = scaleBitmap(bitmap, maxWidth)
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decode Base64 string back to Bitmap (uses BitmapCache for performance)
     */
    fun base64ToBitmap(base64: String): Bitmap? {
        // Check cache first
        BitmapCache.getCached(base64)?.let { return it }
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Scale bitmap to max width maintaining aspect ratio
     */
    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap

        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }

    /**
     * Create a temp file for camera capture
     */
    fun createTempImageFile(context: Context, prefix: String = "IMG"): File {
        val timestamp = System.currentTimeMillis()
        val storageDir = context.cacheDir
        return File.createTempFile("${prefix}_${timestamp}_", ".jpg", storageDir)
    }

    /**
     * Get file size in human-readable format
     */
    fun getFileSizeString(context: Context, uri: Uri): String {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex >= 0) {
                        val size = it.getLong(sizeIndex)
                        return formatFileSize(size)
                    }
                }
            }
            "Bilinmeyen boyut"
        } catch (e: Exception) {
            "Bilinmeyen boyut"
        }
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }

    /**
     * Check if file is an image based on URI
     */
    fun isImageUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("image/") == true
    }

    /**
     * Check if file is a document based on URI
     */
    fun isDocumentUri(context: Context, uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType != null && !mimeType.startsWith("image/") && !mimeType.startsWith("video/")
    }
}
