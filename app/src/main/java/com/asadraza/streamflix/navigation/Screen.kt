package com.asadraza.streamflix.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization
 */
sealed interface Screen {

    
    //  Auth Screens

    /**
     * Login screen
     */
    @Serializable
    data object Login : Screen

    /**
     * Sign up screen
     */
    @Serializable
    data object SignUp : Screen

    /**
     * Forgot password screen
     */
    @Serializable
    data object ForgotPassword : Screen

    /**
     * Email verification screen
     */
    @Serializable
    data object EmailVerification : Screen

    
    //  Profile Management
    

    /**
     * Profile selection screen
     */
    @Serializable
    data object ProfileSelection : Screen

    /**
     * Profile management screen
     */
    @Serializable
    data object ProfileManagement : Screen

    
    //  Main App Screens
    

    /**
     * Home screen
     */
    @Serializable
    data object Home : Screen

    /**
     * Search screen
     */
    @Serializable
    data object Search : Screen

    /**
     * Downloads screen
     */
    @Serializable
    data object Downloads : Screen

    /**
     * Settings screen
     */
    @Serializable
    data object Settings : Screen

    
    //  Detail Screens (with arguments)
    

    /**
     * Movie/Show detail screen
     */
    @Serializable
    data class Detail(
        val movieId: String
    ) : Screen

    /**
     * Video player screen
     * @param movieId The ID of the movie/show to play
     * @param startPosition Optional start position in milliseconds
     */
    @Serializable
    data class Player(
        val movieId: String,
        val startPosition: Long = 0L
    ) : Screen

    
    //  Bottom Navigation Screens
    

    /**
     * Sealed class for bottom navigation items
     */
    sealed class BottomNav(
        val route: Screen,
        val title: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector = icon
    ) {
        data object Home : BottomNav(
            route = Screen.Home,
            title = "Home",
            icon = Icons.Outlined.Home,
            selectedIcon = Icons.Filled.Home
        )

        data object Search : BottomNav(
            route = Screen.Search,
            title = "Search",
            icon = Icons.Outlined.Search,
            selectedIcon = Icons.Filled.Search
        )

        data object Downloads : BottomNav(
            route = Screen.Downloads,
            title = "Downloads",
            icon = Icons.Outlined.Download,
            selectedIcon = Icons.Filled.Download
        )

        data object Profile : BottomNav(
            route = Screen.ProfileManagement,
            title = "Profile",
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Filled.Person
        )

        companion object {
            val items = listOf(Home, Search, Downloads, Profile)
        }
    }
}

/**
 * Extension functions for Screen navigation helpers
 */
fun Screen.requiresAuth(): Boolean = when (this) {
    is Screen.Login,
    is Screen.SignUp,
    is Screen.ForgotPassword -> false
    else -> true
}

fun Screen.isAuthScreen(): Boolean = when (this) {
    is Screen.Login,
    is Screen.SignUp,
    is Screen.ForgotPassword,
    is Screen.EmailVerification -> true
    else -> false
}

fun Screen.showsBottomBar(): Boolean = when (this) {
    is Screen.Home,
    is Screen.Search,
    is Screen.Downloads,
    is Screen.ProfileManagement -> true
    else -> false
}