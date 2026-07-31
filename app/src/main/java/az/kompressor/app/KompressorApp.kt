package az.kompressor.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import az.kompressor.app.util.LocaleHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KompressorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(LocaleHelper.getDarkMode(this))

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getString("app_version", "") != "5.0") {
            FirebaseAuth.getInstance().signOut()
            deleteDatabase("kompressor_db")
            val kompressorPrefs = getSharedPreferences("kompressor_prefs", MODE_PRIVATE)
            kompressorPrefs.edit { remove("dummy_seeded") }
            prefs.edit { putString("app_version", "5.0") }
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }
}
