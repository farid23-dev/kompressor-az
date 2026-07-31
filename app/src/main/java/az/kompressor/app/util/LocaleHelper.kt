package az.kompressor.app.util

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.edit
import java.util.Locale

object LocaleHelper {

    const val PREF_LANGUAGE = "pref_language"
    const val PREF_DARK_MODE = "pref_dark_mode"

    fun applyLocale(context: Context): Context {
        val lang = getLanguage(context)
        return setLocale(context, lang)
    }

    fun setLocale(context: Context, language: String): Context {
        val locale = Locale.forLanguageTag(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getLanguage(context: Context): String =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getString(PREF_LANGUAGE, "en") ?: "en"

    fun saveLanguage(context: Context, language: String) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit { putString(PREF_LANGUAGE, language) }
    }

    fun getDarkMode(context: Context): Int =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getInt(PREF_DARK_MODE, -1)

    fun saveDarkMode(context: Context, mode: Int) {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit { putInt(PREF_DARK_MODE, mode) }
    }
}
