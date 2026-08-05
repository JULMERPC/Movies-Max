package com.puma.videomax.presentation.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.puma.videomax.presentation.details.DetailsScreen
import com.puma.videomax.presentation.library.LibraryScreen
import com.puma.videomax.presentation.music.MiniPlayer
import com.puma.videomax.presentation.music.MusicScreen
import com.puma.videomax.presentation.player.PlayerScreen
import com.puma.videomax.presentation.playlists.PlaylistDetailScreen
import com.puma.videomax.presentation.playlists.PlaylistsScreen
import com.puma.videomax.presentation.playlists.SmartCollectionScreen
import com.puma.videomax.presentation.settings.PrivateFolderScreen
import com.puma.videomax.presentation.settings.SettingsScreen
import com.puma.videomax.presentation.theme.VideoMaxDimens
import com.puma.videomax.service.BackgroundAudioManager
import com.puma.videomax.service.BackgroundAudioService

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
	val showNav = currentRoute in setOf(
		Screen.Library.route,
		Screen.Music.route,
		Screen.Playlists.route,
		Screen.Settings.route
	)

	val items = listOf(
		BottomItem(Screen.Library, "Videos", Icons.Default.Home),
		BottomItem(Screen.Music, "Música", Icons.Default.Headphones),
		BottomItem(Screen.Playlists, "Listas", Icons.AutoMirrored.Filled.PlaylistPlay),
		BottomItem(Screen.Settings, "Ajustes", Icons.Default.Settings)
	)

	BoxWithConstraints {
		val isWideScreen = maxWidth >= 600.dp

		Scaffold(
			bottomBar = {
				if (showNav && !isWideScreen) {
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
			Box(modifier = Modifier.padding(padding).fillMaxSize()) {
				Row(modifier = Modifier.fillMaxSize()) {
					if (showNav && isWideScreen) {
						NavigationRail {
							items.forEach { item ->
								NavigationRailItem(
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

					NavHost(
						navController = navController,
						startDestination = Screen.Library.route,
						modifier = Modifier.weight(1f)
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
					composable(Screen.Music.route) {
						MusicScreen()
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
								animationSpec = tween(VideoMaxDimens.animationNormal)
							)
						},
						exitTransition = {
							slideOutHorizontally(
								targetOffsetX = { fullWidth -> fullWidth },
								animationSpec = tween(VideoMaxDimens.animationNormal)
							)
						},
						popEnterTransition = {
							slideInHorizontally(
								initialOffsetX = { fullWidth -> -fullWidth },
								animationSpec = tween(VideoMaxDimens.animationNormal)
							)
						},
						popExitTransition = {
							slideOutHorizontally(
								targetOffsetX = { fullWidth -> fullWidth },
								animationSpec = tween(VideoMaxDimens.animationNormal)
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

				MiniPlayer(
					onOpenFullPlayer = {
						navController.navigate(Screen.Music.route) {
							popUpTo(navController.graph.findStartDestination().id) {
								saveState = true
							}
							launchSingleTop = true
							restoreState = true
						}
					},
					onClose = {
						BackgroundAudioManager.clear()
						navController.context.stopService(
							android.content.Intent(navController.context, BackgroundAudioService::class.java)
						)
					},
					modifier = Modifier.align(Alignment.BottomCenter)
				)
			}
		}
	}
}
