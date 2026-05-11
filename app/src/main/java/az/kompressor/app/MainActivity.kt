package az.kompressor.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavController
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

    // Destinations where bottom nav should be HIDDEN
    private val authDestinations = setOf(
        R.id.signInFragment,
        R.id.signUpFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Wire BottomNavigationView with NavController
        binding.bottomNavigationView.setupWithNavController(navController)

        // Show/hide bottom nav based on destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigationView.isVisible = destination.id !in authDestinations
        }

        // Skip auth if already logged in
        if (authRepository.isUserLoggedIn()) {
            navController.navigate(R.id.homeFragment)
        }
    }
}
