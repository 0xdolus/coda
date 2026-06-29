package com.coda.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.coda.music.navigation.AppNavGraph
import com.coda.music.navigation.Routes
import com.coda.music.player.PlayerController
import com.coda.music.ui.components.BottomNavigationBar
import com.coda.music.ui.components.MiniPlayer
import com.coda.music.ui.theme.CodaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CodaTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar = currentRoute != Routes.PLAYER
                val showMiniPlayer = currentRoute == Routes.HOME
                val playbackState by playerController.playbackState.collectAsStateWithLifecycle()

                CompositionLocalProvider(
                    LocalProviders.LocalPlayerController provides playerController
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        bottomBar = {
                            Column {
                                if (showMiniPlayer && playbackState.currentTrack != null) {
                                    MiniPlayer(
                                        onClick = { trackId ->
                                            navController.navigate(Routes.player(trackId))
                                        }
                                    )
                                }
                                if (showBottomBar) {
                                    val rootRoute = when {
                                        currentRoute?.startsWith("artist") == true -> null
                                        else -> currentRoute
                                    }
                                    BottomNavigationBar(
                                        currentRoute = rootRoute ?: Routes.HOME,
                                        onNavigate = { route ->
                                            navController.navigate(route) {
                                                popUpTo(Routes.HOME) { saveState = true }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        AppNavGraph(
                            navController = navController,
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaController connection only — the MediaPlaybackService
        // keeps running independently, sustaining background audio.
        playerController.release()
    }
}
