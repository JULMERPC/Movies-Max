package com.example.videomax.presentation.navigation

sealed class Screen(val route: String) {
	data object Library : Screen("library")
	data object Favorites : Screen("favorites")
	data object Playlists : Screen("playlists")
	data object History : Screen("history")
	data object Settings : Screen("settings")
	data object Player : Screen("player/{videoId}") {
		fun createRoute(videoId: Long) = "player/$videoId"
	}
	data object Details : Screen("details/{videoId}") {
		fun createRoute(videoId: Long) = "details/$videoId"
	}
	data object PlaylistDetail : Screen("playlist/{playlistId}") {
		fun createRoute(playlistId: Long) = "playlist/$playlistId"
	}
	data object Folder : Screen("folder/{folderName}") {
		fun createRoute(folderName: String) = "folder/${android.net.Uri.encode(folderName)}"
	}
}

val bottomNavItems = listOf(
	Screen.Library,
	Screen.Favorites,
	Screen.Playlists,
	Screen.History,
	Screen.Settings
)
