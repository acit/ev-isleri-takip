package com.aile.takip.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import java.util.*

/**
 * Dil tercihlerini yonetir.
 * SharedPreferences ile kalici olarak saklar.
 */
object LocaleHelper {
    private const val PREF_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "app_language"
    
    // Desteklenen diller
    const val TURKISH = "tr"
    const val ENGLISH = "en"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Mevcut dil tercihini dondurur.
     */
    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, TURKISH) ?: TURKISH
    }
    
    /**
     * Dil tercihini kaydeder.
     */
    fun setLanguage(context: Context, language: String) {
        getPrefs(context).edit().putString(KEY_LANGUAGE, language).apply()
        applyLocale(context, language)
    }
    
    /**
     * Dil ayarini uygular.
     */
    fun applyLocale(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)
        
        val config = context.resources.configuration
        config.setLocale(locale)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }
    
    /**
     * App basladiginda calistirilmalidir.
     */
    fun onAttach(context: Context) {
        val language = getLanguage(context)
        applyLocale(context, language)
    }
    
    /**
     * Desteklenen dillerin listesini dondurur.
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            TURKISH to "Türkçe",
            ENGLISH to "English"
        )
    }
}
