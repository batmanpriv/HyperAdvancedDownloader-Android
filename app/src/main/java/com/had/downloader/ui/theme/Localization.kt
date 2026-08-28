package com.had.downloader.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val LANG_PREFS = "had_language_prefs"
private const val KEY_LANG = "selected_language"

enum class AppLanguage(val storageKey: String, val displayNameNative: String) {
    EN("en", "English"),
    FA("fa", "فارسی")
}

object LanguageManager {
    var current by mutableStateOf(AppLanguage.EN)
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
        current = if (prefs.getString(KEY_LANG, null) == AppLanguage.FA.storageKey) {
            AppLanguage.FA
        } else {
            AppLanguage.EN
        }
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        current = language
        context.applicationContext
            .getSharedPreferences(LANG_PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANG, language.storageKey).apply()
    }

    val isFa: Boolean get() = current == AppLanguage.FA
}

fun t(en: String, fa: String): String = if (LanguageManager.current == AppLanguage.FA) fa else en
