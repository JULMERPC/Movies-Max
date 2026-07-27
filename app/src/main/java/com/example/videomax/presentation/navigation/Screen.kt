package com.example.videomax.presentation.navigation

sealed class Screen(val route: String) {
	data object Library : Screen("library")
	data object Playlists : Screen("playlists")
	data object Settings : Screen("settings")
	data object PrivateFolder : Screen("private_folder")
	data object Player : Screen("player/{videoId}") {
		fun createRoute(videoId: Long) = "player/$videoId"
	}
	data object Details : Screen("details/{videoId}") {
		fun createRoute(videoId: Long) = "details/$videoId"
	}
	data object PlaylistDetail : Screen("playlist/{playlistId}") {
		fun createRoute(playlistId: Long) = "playlist/$playlistId"
	}
	data object SmartCollection : Screen("smart/{type}") {
		fun createRoute(type: String) = "smart/$type"
		const val FAVORITES = "favorites"
		const val HISTORY = "history"
		const val MOST_PLAYED = "most_played"
		const val RECENT = "recent"
		const val QUEUE = "queue"
	}
}
