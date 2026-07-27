package com.example.videomax.presentation.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.videomax.presentation.details.DetailsScreen
import com.example.videomax.presentation.library.LibraryScreen
import com.example.videomax.presentation.player.PlayerScreen
import com.example.videomax.presentation.playlists.PlaylistDetailScreen
import com.example.videomax.presentation.playlists.PlaylistsScreen
import com.example.videomax.presentation.playlists.SmartCollectionScreen
import com.example.videomax.presentation.settings.PrivateFolderScreen
import com.example.videomax.presentation.settings.SettingsScreen

private data class BottomItem(
	val screen: Screen,
	val label: String,
	val icon: ImageVector
)

@Composable
fun VideoPlayerNavHost() {
	val navController = rememberNavController()
	val backStack by navController.currentBackStackEntryAsState()
	val currentRoute = backStack?.destination?.route
	val showBottomBar = currentRoute in setOf(
		Screen.Library.route,
		Screen.Playlists.route,
		Screen.Settings.route
	)

	val items = listOf(
		BottomItem(Screen.Library, "Library", Icons.Default.Home),
		BottomItem(Screen.Playlists, "Playlists", Icons.AutoMirrored.Filled.PlaylistPlay),
		BottomItem(Screen.Settings, "Settings", Icons.Default.Settings)
	)

	Scaffold(
		bottomBar = {
			if (showBottomBar) {
				NavigationBar {
					items.forEach { item ->
						NavigationBarItem(
							selected = currentRoute == item.screen.route,
							onClick = {
								navController.navigate(item.screen.route) {
									popUpTo(navController.graph.findStartDestination().id) {
										saveState = true
									}
									launchSingleTop = true
									restoreState = true
								}
							},
							icon = { Icon(item.icon, contentDescription = item.label) },
							label = { Text(item.label) }
						)
					}
				}
			}
		}
	) { padding ->
		NavHost(
			navController = navController,
			startDestination = Screen.Library.route,
			modifier = Modifier.padding(padding)
		) {
			composable(Screen.Library.route) {
				LibraryScreen(
					onOpenPlayer = { id ->
						navController.navigate(Screen.Player.createRoute(id))
					},
					onOpenDetails = { id ->
						navController.navigate(Screen.Details.createRoute(id))
					}
				)
			}
			composable(Screen.Playlists.route) {
				PlaylistsScreen(
					onOpenPlaylist = { id ->
						navController.navigate(Screen.PlaylistDetail.createRoute(id))
					},
					onOpenSmart = { type ->
						navController.navigate(Screen.SmartCollection.createRoute(type))
					}
				)
			}
			composable(Screen.Settings.route) {
				SettingsScreen(
					onOpenPrivateFolder = {
						navController.navigate(Screen.PrivateFolder.route)
					}
				)
			}
			composable(Screen.PrivateFolder.route) {
				PrivateFolderScreen(
					onBack = { navController.popBackStack() }
				)
			}
			composable(
				route = Screen.Player.route,
				arguments = listOf(navArgument("videoId") { type = NavType.LongType }),
				enterTransition = {
					slideInHorizontally(
						initialOffsetX = { fullWidth -> fullWidth },
						animationSpec = androidx.compose.animation.core.tween(300)
					)
				},
				exitTransition = {
					slideOutHorizontally(
						targetOffsetX = { fullWidth -> fullWidth },
						animationSpec = androidx.compose.animation.core.tween(300)
					)
				},
				popEnterTransition = {
					slideInHorizontally(
						initialOffsetX = { fullWidth -> -fullWidth },
						animationSpec = androidx.compose.animation.core.tween(300)
					)
				},
				popExitTransition = {
					slideOutHorizontally(
						targetOffsetX = { fullWidth -> fullWidth },
						animationSpec = androidx.compose.animation.core.tween(300)
					)
				}
			) {
				PlayerScreen(
					onBack = { navController.popBackStack() },
					onOpenDetails = { id ->
						navController.navigate(Screen.Details.createRoute(id))
					}
				)
			}
			composable(
				route = Screen.Details.route,
				arguments = listOf(navArgument("videoId") { type = NavType.LongType })
			) {
				DetailsScreen(
					onBack = { navController.popBackStack() },
					onPlay = { id ->
						navController.navigate(Screen.Player.createRoute(id))
					}
				)
			}
			composable(
				route = Screen.PlaylistDetail.route,
				arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
			) {
				PlaylistDetailScreen(
					onBack = { navController.popBackStack() },
					onOpenPlayer = { id ->
						navController.navigate(Screen.Player.createRoute(id))
					}
				)
			}
			composable(
				route = Screen.SmartCollection.route,
				arguments = listOf(navArgument("type") { type = NavType.StringType })
			) {
				SmartCollectionScreen(
					onBack = { navController.popBackStack() },
					onOpenPlayer = { id ->
						navController.navigate(Screen.Player.createRoute(id))
					}
				)
			}
		}
	}
}
