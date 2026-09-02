package com.aile.takip.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.*

/**
 * Dil tercihlerini yonetir.
 * SharedPreferences ile kalici olarak saklar.
 * Guvenli: crash yapmayacak sekilde basitlestirildi.
 */
object LocaleHelper {
    private const val PREF_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "app_language"
    
    const val TURKISH = "tr"
    const val ENGLISH = "en"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    fun getLanguage(context: Context): String {
        return try {
            getPrefs(context).getString(KEY_LANGUAGE, TURKISH) ?: TURKISH
        } catch (e: Exception) {
            TURKISH
        }
    }
    
    fun setLanguage(context: Context, language: String) {
        try {
            getPrefs(context).edit().putString(KEY_LANGUAGE, language).apply()
            // Sadece Locale default'unu ayarla - updateConfiguration kullanma
            Locale.setDefault(Locale(language))
        } catch (e: Exception) {
            // Hata olursa sessizce gec
        }
    }
    
    /**
     * App basladiginda calistirilmalidir.
     * Sadece default locale'u ayarlar.
     */
    fun onAttach(context: Context) {
        try {
            val language = getLanguage(context)
            Locale.setDefault(Locale(language))
        } catch (e: Exception) {
            // Hata olursa sessizce gec
        }
    }
    
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            TURKISH to "Türkçe",
            ENGLISH to "English"
        )
    }
}
