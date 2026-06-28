package com.coda.music.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.coda.music.ui.screens.artist.ArtistScreen
import com.coda.music.ui.screens.home.HomeScreen
import com.coda.music.ui.screens.library.LibraryScreen
import com.coda.music.ui.screens.more.MoreScreen
import com.coda.music.ui.screens.player.PlayerScreen
import com.coda.music.ui.screens.search.SearchScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onArtistClick = { artistId ->
                    navController.navigate(Routes.artist(artistId))
                },
                onTrackClick = { trackId ->
                    navController.navigate(Routes.player(trackId))
                },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onTrackClick = { trackId ->
                    navController.navigate(Routes.player(trackId))
                }
            )
        }

        composable(Routes.LIBRARY) {
            LibraryScreen(
                onTrackClick = { trackId ->
                    navController.navigate(Routes.player(trackId))
                }
            )
        }

        composable(Routes.MORE) {
            MoreScreen()
        }

        composable(
            route = Routes.ARTIST,
            arguments = listOf(
                navArgument(NavArguments.ARTIST_ID) { type = NavType.StringType }
            )
        ) {
            ArtistScreen(
                onTrackClick = { trackId ->
                    navController.navigate(Routes.player(trackId))
                },
                onBackClick = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument(NavArguments.TRACK_ID) { type = NavType.StringType }
            )
        ) {
            PlayerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
