package az.kompressor.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import az.kompressor.app.databinding.ActivityMainBinding
import az.kompressor.app.domain.repository.AuthRepository
import az.kompressor.app.util.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var authRepository: AuthRepository

    private val hiddenDestinations = setOf(
        R.id.signInFragment,
        R.id.signUpFragment,
        R.id.carDetailFragment,
        R.id.postCarFragment,
        R.id.profileFragment,
        R.id.settingsFragment
    )

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.applyLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationView.setupWithNavController(navController)

        binding.fabPost.setOnClickListener {
            navController.navigate(R.id.postCarFragment)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val visible = destination.id !in hiddenDestinations
            binding.bottomNavigationView.isVisible = visible
            binding.fabPost.isVisible = visible
        }

        if (authRepository.isUserLoggedIn()) {
            navController.navigate(
                R.id.homeFragment, null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.signInFragment, inclusive = true)
                    .build()
            )
        }

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        val carId: String? = when {
            data.scheme == "https" && data.host == "kompressor.az" ->
                data.pathSegments.getOrNull(1)
            data.scheme == "kompressor" && data.host == "car" ->
                data.pathSegments.firstOrNull()
            else -> null
        }
        if (!carId.isNullOrBlank()) {
            navController.navigate(R.id.carDetailFragment, bundleOf("carId" to carId))
        }
    }
}
