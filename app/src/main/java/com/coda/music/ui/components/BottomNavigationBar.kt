package com.coda.music.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.coda.music.navigation.Routes

data class NavItem(
    val label: String,
    val route: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val navItems = listOf(
    NavItem("Home",    Routes.HOME,    Icons.Filled.Home,         Icons.Outlined.Home),
    NavItem("Search",  Routes.SEARCH,  Icons.Filled.Search,       Icons.Outlined.Search),
    NavItem("Library", Routes.LIBRARY, Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    NavItem("More",    Routes.MORE,    Icons.Filled.MoreHoriz,    Icons.Outlined.MoreHoriz)
)

@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (route: String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(modifier = modifier) {
        navItems.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) }
            )
        }
    }
}
