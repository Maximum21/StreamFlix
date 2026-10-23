package com.asadraza.streamflix.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.asadraza.streamflix.BuildConfig
import com.asadraza.streamflix.feature.auth.emailverification.EmailVerificationScreen
import com.asadraza.streamflix.feature.auth.forgotpassword.ForgotPasswordScreen
import com.asadraza.streamflix.feature.auth.login.LoginScreen
import com.asadraza.streamflix.feature.auth.signup.SignUpScreen
import com.asadraza.streamflix.feature.detail.DetailScreen
import com.asadraza.streamflix.feature.home.HomeScreen
import com.asadraza.streamflix.feature.mylist.MyListScreen
import com.asadraza.streamflix.feature.player.PlayerScreen
import com.asadraza.streamflix.feature.profile.management.ProfileManagementScreen
import com.asadraza.streamflix.feature.profile.selection.ProfileSelectionScreen
import com.asadraza.streamflix.feature.search.SearchScreen
import com.asadraza.streamflix.feature.settings.SettingsScreen

/**
 * Navigation graph for the entire app
 */
@Composable
fun AppNavGraph(
    navigator: AppNavigator,
    navController: NavHostController,
    startDestination: Screen,
    modifier: Modifier = Modifier
) {
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
                    navigator.clearBackStackAndNavigateTo(Screen.ProfileSelection)
                },
                onNavigateToSignUp = {
                    navigator.navigateTo(Screen.SignUp)
                },
                onNavigateToForgotPassword = {
                    navigator.navigateTo(Screen.ForgotPassword)
                }
            )
        }

        composable<Screen.SignUp> {
            SignUpScreen(
                onNavigateToLogin = {
                    navigator.replaceWith(Screen.Login)
                },
                onNavigateToEmailVerification = {
                    navigator.replaceWith(Screen.EmailVerification)
                }
            )
        }

        composable<Screen.ForgotPassword> {
            ForgotPasswordScreen(
                onNavigateToLogin = {
                    navigator.replaceWith(Screen.Login)
                }
            )
        }

        composable<Screen.EmailVerification> {
            EmailVerificationScreen(
                onNavigateToHome = {
                    navigator.clearBackStackAndNavigateTo(Screen.ProfileSelection)
                },
                onNavigateToLogin = {
                    navigator.clearBackStackAndNavigateTo(Screen.Login)
                }
            )
        }

        // ══════════════════════════════════════════════════════════
        //  Profile Screens
        // ══════════════════════════════════════════════════════════

        composable<Screen.ProfileSelection> {
            ProfileSelectionScreen(
                onProfileSelected = {
                    navigator.clearBackStackAndNavigateTo(Screen.Home)
                },
                onNavigateToManageProfiles = {
                    navigator.navigateTo(Screen.ProfileManagement)
                }
            )
        }

        composable<Screen.ProfileManagement> {
            ProfileManagementScreen(
                onNavigateBack = {
                    navigator.navigateBack()
                }
            )
        }

        // ══════════════════════════════════════════════════════════
        //  Main App Screens
        // ══════════════════════════════════════════════════════════

        composable<Screen.Home> {
            HomeScreen(
                onMovieClick = { movieId ->
                    navigator.navigateTo(Screen.Detail(movieId))
                },
                onPlayClick = { movieId ->
                    navigator.navigateTo(Screen.Player(movieId))
                },
                onSearchClick = {
                    navigator.navigateTo(Screen.Search)
                }
            )
        }

        composable<Screen.Search> {
            SearchScreen(
                onMovieClick = { movieId ->
                    navigator.navigateTo(Screen.Detail(movieId))
                }
            )
        }

        composable<Screen.MyList> {
            MyListScreen(onMovieClick = { navigator.navigateTo(Screen.Detail(it)) })
        }

        composable<Screen.Settings> {
            SettingsScreen(
                appVersion = BuildConfig.VERSION_NAME,
                privacyPolicyUrl = AppLinks.PRIVACY_POLICY
            )
        }

        // ══════════════════════════════════════════════════════════
        //  Detail Screens (with animations)
        // ══════════════════════════════════════════════════════════

        composable<Screen.Detail>(
            deepLinks = listOf(navDeepLink<Screen.Detail>(basePath = AppLinks.MOVIE_DEEP_LINK)),
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
        ) {
            DetailScreen(
                onBackClick = {
                    navigator.navigateBack()
                },
                onPlayClick = { movieId ->
                    navigator.navigateTo(Screen.Player(movieId))
                },
                onMovieClick = { movieId ->
                    navigator.navigateTo(Screen.Detail(movieId))
                }
            )
        }

        composable<Screen.Player>(
            enterTransition = { fadeIn(animationSpec = tween(300)) },
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            PlayerScreen(
                onBack = {
                    navigator.navigateBack()
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
