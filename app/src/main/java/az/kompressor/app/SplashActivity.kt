package az.kompressor.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import az.kompressor.app.databinding.ActivitySplashBinding
import az.kompressor.app.util.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen") // We manage our own branded splash
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide system bars for a full-screen immersive splash
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        lifecycleScope.launch {
            delay(1_500L) // 1.5 s branded splash
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
            // Crossfade instead of default slide
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}
