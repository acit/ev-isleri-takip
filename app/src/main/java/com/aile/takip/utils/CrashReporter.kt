package com.aile.takip.utils

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Yakalanmayan istisnaları cihazda dosyaya kaydeder.
 *
 * Amaç: gerçek cihazlardaki çökmeler USB/logcat olmadan teşhis edilebilsin.
 * Raporlar `files/crash_logs/crash_*.txt` altına yazılır; en fazla
 * [MAX_FILES] rapor tutulur. Bu sınıf kendisi asla çökmemelidir — her şey
 * try-catch ile sarılır.
 */
object CrashReporter {

    private const val DIR_NAME = "crash_logs"
    private const val MAX_FILES = 5

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                writeCrash(context.applicationContext, thread, throwable)
            } catch (_: Throwable) {
                // rapor yazılamadıysa bile zinciri bozma
            }
            try {
                previous?.uncaughtException(thread, throwable)
            } catch (_: Throwable) {
            }
        }
    }

    private fun crashDir(context: Context): File =
        File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun writeCrash(context: Context, thread: Thread?, throwable: Throwable) {
        val dir = crashDir(context)
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(dir, "crash_$stamp.txt")

        val sw = StringWriter()
        PrintWriter(sw).use { pw ->
            pw.println("Aile Takip çökme raporu")
            pw.println("Zaman: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            pw.println("Thread: ${thread?.name}")
            pw.println("Sürüm: ${versionName()} (${versionCode()})")
            pw.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            pw.println("Cihaz: ${Build.MANUFACTURER} ${Build.MODEL}")
            pw.println()
            throwable.printStackTrace(pw)
        }
        out.writeText(sw.toString())

        // Android 11+ dosya yöneticisinden erişilebilsin diye dış klasöre de kopyala
        // (Android/data/com.aile.takip/files/crash_logs; izin gerektirmez)
        try {
            val external = context.getExternalFilesDir(null)
            if (external != null) {
                val publicDir = File(external, "crash_logs")
                publicDir.mkdirs()
                File(publicDir, out.name).writeText(sw.toString())
            }
        } catch (_: Throwable) {
        }

        // Eski raporları temizle (en fazla MAX_FILES dosya)
        val files = dir.listFiles()?.sortedBy { it.name } ?: return
        if (files.size > MAX_FILES) {
            files.take(files.size - MAX_FILES).forEach { it.delete() }
        }
    }

    /** Son çökme raporu dosyası (varsa). Paylaş/diagnostik ekranları için. */
    fun latestReport(context: Context): File? =
        try {
            crashDir(context).listFiles()?.maxByOrNull { it.name }
        } catch (_: Throwable) {
            null
        }

    fun hasReports(context: Context): Boolean = latestReport(context) != null

    private fun versionName(): String = try {
        com.aile.takip.BuildConfig.VERSION_NAME
    } catch (_: Throwable) {
        "?"
    }

    private fun versionCode(): Int = try {
        com.aile.takip.BuildConfig.VERSION_CODE
    } catch (_: Throwable) {
        0
    }
}
