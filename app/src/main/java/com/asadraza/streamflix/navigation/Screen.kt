package com.asadraza.streamflix.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes.
 */
sealed interface Screen {
    @Serializable
    data object Login : Screen

    @Serializable
    data object SignUp : Screen

    @Serializable
    data object ForgotPassword : Screen

    @Serializable
    data object EmailVerification : Screen

    @Serializable
    data object ProfileSelection : Screen

    @Serializable
    data object ProfileManagement : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object MyList : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Search : Screen

    @Serializable
    data class Detail(val movieId: String) : Screen

    @Serializable
    data class Player(val movieId: String) : Screen
}

/** A top-level destination reachable from the bottom bar. */
enum class TopLevelDestination(
    val route: Screen,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Home(Screen.Home, "Home", Icons.Outlined.Home, Icons.Filled.Home),
    Search(Screen.Search, "Search", Icons.Outlined.Search, Icons.Filled.Search),
    MyList(Screen.MyList, "My List", Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark),
    Profile(Screen.ProfileManagement, "Profile", Icons.Outlined.Person, Icons.Filled.Person),
    Settings(Screen.Settings, "Settings", Icons.Outlined.Settings, Icons.Filled.Settings);

    fun isSelected(destination: NavDestination?): Boolean = destination?.hasRoute(route::class) == true
}

/** The bottom bar is shown on exactly the top-level destinations. */
fun NavDestination?.showsBottomBar(): Boolean = TopLevelDestination.entries.any { it.isSelected(this) }
