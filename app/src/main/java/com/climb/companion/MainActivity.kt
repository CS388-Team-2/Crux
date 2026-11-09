package com.climb.companion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.climb.companion.navigation.CruxRoute
import com.climb.companion.ui.feed.FeedScreen
import com.climb.companion.ui.permissions.PermissionsScreen
import com.climb.companion.ui.theme.CruxTheme
import com.climb.companion.ui.welcome.WelcomeScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CruxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = CruxRoute.Welcome.route,
                    ) {
                        composable(CruxRoute.Welcome.route) {
                            WelcomeScreen(
                                onGetStarted = {
                                    navController.navigate(CruxRoute.Permissions.route)
                                },
                                onHasAccount = {
                                    navController.navigate(CruxRoute.Feed.route) {
                                        popUpTo(CruxRoute.Welcome.route) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(CruxRoute.Permissions.route) {
                            PermissionsScreen(
                                onBack = { navController.popBackStack() },
                                onContinue = {
                                    navController.navigate(CruxRoute.Feed.route) {
                                        popUpTo(CruxRoute.Welcome.route) { inclusive = true }
                                    }
                                },
                            )
                        }
                        composable(CruxRoute.Feed.route) {
                            FeedScreen()
                        }
                    }
                }
            }
        }
    }
}
