package com.iicytower.wanderlist.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.iicytower.wanderlist.feature.assistant.ui.AssistantScreen
import com.iicytower.wanderlist.feature.detail.ui.AttractionDetailScreen
import com.iicytower.wanderlist.feature.map.ui.MapScreen
import com.iicytower.wanderlist.feature.mylist.ui.MyListScreen
import com.iicytower.wanderlist.feature.search.ui.SearchScreen
import com.iicytower.wanderlist.feature.settings.ui.SettingsScreen

private data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.Search, "Szukaj", Icons.Default.Search),
    BottomNavItem(Screen.Map, "Mapa", Icons.Default.Place),
    BottomNavItem(Screen.MyList, "Moja Lista", Icons.Default.Favorite),
    BottomNavItem(Screen.Assistant, "Asystent", Icons.Default.List),
    BottomNavItem(Screen.Settings, "Ustawienia", Icons.Default.Settings)
)

@Composable
fun WanderListNavGraph(navController: NavHostController = rememberNavController()) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route != Screen.AttractionDetail.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Search.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Search.route) {
                SearchScreen(onAttractionClick = { xid ->
                    navController.navigate(Screen.AttractionDetail.createRoute(xid))
                })
            }
            composable(Screen.Map.route) {
                MapScreen(onAttractionClick = { xid ->
                    navController.navigate(Screen.AttractionDetail.createRoute(xid))
                })
            }
            composable(Screen.MyList.route) {
                MyListScreen(onAttractionClick = { xid ->
                    navController.navigate(Screen.AttractionDetail.createRoute(xid))
                })
            }
            composable(Screen.Assistant.route) {
                AssistantScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = Screen.AttractionDetail.route,
                arguments = listOf(navArgument("xid") { type = NavType.StringType })
            ) { backStackEntry ->
                val xid = backStackEntry.arguments?.getString("xid") ?: return@composable
                AttractionDetailScreen(
                    xid = xid,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
