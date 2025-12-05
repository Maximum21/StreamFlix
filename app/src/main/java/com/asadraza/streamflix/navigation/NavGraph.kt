package com.asadraza.streamflix.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.asadraza.streamflix.feature.auth.email_verification.EmailVerificationScreen
import com.asadraza.streamflix.feature.auth.forgot_password.ForgotPasswordScreen
import com.asadraza.streamflix.feature.auth.login.LoginScreen
import com.asadraza.streamflix.feature.auth.signup.SignUpScreen
import com.asadraza.streamflix.feature.detail.DetailScreen
import com.asadraza.streamflix.feature.home.HomeScreen
import com.asadraza.streamflix.feature.player.PlayerScreen
import com.asadraza.streamflix.feature.profile.management.ProfileManagementScreen
import com.asadraza.streamflix.feature.profile.selection.ProfileSelectionScreen
import com.asadraza.streamflix.feature.search.SearchScreen
import timber.log.Timber

/**
 * Navigation graph for the entire app
 */
@Composable
fun AppNavGraph(
    navController: NavHostController,
    navigationManager: NavigationManagerImpl,
    startDestination: Screen,
    modifier: Modifier = Modifier
) {
    // Connect NavigationManager with NavController
    DisposableEffect(navController) {
        navigationManager.setNavController(navController)
        Timber.d("NavController connected to NavigationManager")

        onDispose {
            navigationManager.clearNavController()
            Timber.d("NavController disconnected from NavigationManager")
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.fillMaxSize(),
        enterTransition = { defaultEnterTransition() },
        exitTransition = { defaultExitTransition() },
        popEnterTransition = { defaultPopEnterTransition() },
        popExitTransition = { defaultPopExitTransition() }
    ) {
        // ══════════════════════════════════════════════════════════
        //  Authentication Screens
        // ══════════════════════════════════════════════════════════

        composable<Screen.Login> {
            LoginScreen(
                onNavigateToHome = {
                    navigationManager.clearBackStackAndNavigateTo(Screen.Home)
                },
                onNavigateToSignUp = {
                    navigationManager.navigateTo(Screen.SignUp)
                },
                onNavigateToForgotPassword = {
                    navigationManager.navigateTo(Screen.ForgotPassword)
                }
            )
        }

        composable<Screen.SignUp> {
            SignUpScreen(
                onNavigateToLogin = {
                    navigationManager.replaceWith(Screen.Login)
                },
                onNavigateToEmailVerification = {
                    navigationManager.replaceWith(Screen.EmailVerification)
                }
            )
        }

        composable<Screen.ForgotPassword> {
            ForgotPasswordScreen(
                onNavigateToLogin = {
                    navigationManager.replaceWith(Screen.Login)
                }
            )
        }

        composable<Screen.EmailVerification> {
            EmailVerificationScreen(
                onNavigateToHome = {
                    navigationManager.clearBackStackAndNavigateTo(Screen.Home)
                },
                onNavigateToLogin = {
                    navigationManager.clearBackStackAndNavigateTo(Screen.Login)
                }
            )
        }

        // ══════════════════════════════════════════════════════════
        //  Profile Screens
        // ══════════════════════════════════════════════════════════

        composable<Screen.ProfileSelection> {
            ProfileSelectionScreen(
                onProfileSelected = {
                    navigationManager.replaceWith(Screen.Home)
                },
                onNavigateToManageProfiles = {
                    navigationManager.navigateTo(Screen.ProfileManagement)
                }
            )
        }

        composable<Screen.ProfileManagement> {
            ProfileManagementScreen(
                onNavigateBack = {
                    navigationManager.navigateBack()
                }
            )
        }

        // ══════════════════════════════════════════════════════════
        //  Main App Screens
        // ══════════════════════════════════════════════════════════

        composable<Screen.Home> {
            HomeScreen(
                onMovieClick = { movieId ->
                    navigationManager.navigateTo(Screen.Detail(movieId))
                },
                onSearchClick = {
                    navigationManager.navigateTo(Screen.Search)
                }
            )
        }

        composable<Screen.Search> {
            SearchScreen(
                onNavigateBack = {
                    navigationManager.navigateBack()
                },
                onMovieClick = { movieId ->
                    navigationManager.navigateTo(Screen.Detail(movieId))
                }
            )
        }

        // ══════════════════════════════════════════════════════════
        //  Detail Screens (with animations)
        // ══════════════════════════════════════════════════════════

        composable<Screen.Detail>(
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Down,
                    animationSpec = tween(400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) { backStackEntry ->
            DetailScreen(
                onBackClick = {
                    navigationManager.navigateBack()
                },
                onPlayClick = { movieId ->
                    navigationManager.navigateTo(Screen.Player(movieId))
                },
                onMovieClick = { movieId ->
                    navigationManager.navigateTo(Screen.Detail(movieId))
                }
            )
        }

        composable<Screen.Player>(
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Player>()
            PlayerScreen(
//                startPosition = args.startPosition,
                onBack = {
                    navigationManager.navigateBack()
                }
            )
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Default Navigation Transitions
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalAnimationApi::class)
private fun defaultEnterTransition() = fadeIn(
    animationSpec = tween(300)
)

@OptIn(ExperimentalAnimationApi::class)
private fun defaultExitTransition() = fadeOut(
    animationSpec = tween(300)
)

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopEnterTransition() = fadeIn(
    animationSpec = tween(300)
)

@OptIn(ExperimentalAnimationApi::class)
private fun defaultPopExitTransition() = fadeOut(
    animationSpec = tween(300)
)