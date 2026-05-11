package az.kompressor.app

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import az.kompressor.app.util.LocaleHelper
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KompressorApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(LocaleHelper.getDarkMode(this))

        // Version bump → sign out once so user sees signup page fresh.
        // Also wipe the local Room DB so stale favorites from the previous
        // account don't bleed into the new one.
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getString("app_version", "") != "4.0") {
            FirebaseAuth.getInstance().signOut()
            deleteDatabase("kompressor_db")
            prefs.edit().putString("app_version", "4.0").apply()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(base))
    }
}
