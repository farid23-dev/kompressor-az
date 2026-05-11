package az.kompressor.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import az.kompressor.app.databinding.ActivityMainBinding
import az.kompressor.app.domain.repository.AuthRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    @Inject
    lateinit var authRepository: AuthRepository

    // Destinations where bottom bar should be hidden
    private val hiddenDestinations = setOf(
        R.id.signInFragment,
        R.id.signUpFragment,
        R.id.carDetailFragment,
        R.id.postCarFragment,
        R.id.profileFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire bottom nav (Home + Saved tabs only)
        binding.bottomNavigationView.setupWithNavController(navController)

        // FAB navigates to Post screen
        binding.fabPost.setOnClickListener {
            navController.navigate(R.id.postCarFragment)
        }

        // Show/hide bottom bar + FAB based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val visible = destination.id !in hiddenDestinations
            binding.bottomNavigationView.isVisible = visible
            binding.fabPost.isVisible = visible
        }

        // Skip auth if already logged in — pop signInFragment so it's never on the back stack
        if (authRepository.isUserLoggedIn()) {
            navController.navigate(
                R.id.homeFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.signInFragment, inclusive = true)
                    .build()
            )
        }
    }
}
