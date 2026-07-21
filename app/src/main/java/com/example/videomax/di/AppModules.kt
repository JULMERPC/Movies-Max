package com.example.videomax.di

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.room.Room
import com.example.videomax.data.local.db.VideoDatabase
import com.example.videomax.data.local.db.dao.HistoryDao
import com.example.videomax.data.local.db.dao.PlaylistDao
import com.example.videomax.data.local.db.dao.VideoDao
import com.example.videomax.data.repository.FavoritesRepositoryImpl
import com.example.videomax.data.repository.HistoryRepositoryImpl
import com.example.videomax.data.repository.PlaylistRepositoryImpl
import com.example.videomax.data.repository.SettingsRepositoryImpl
import com.example.videomax.data.repository.VideoRepositoryImpl
import com.example.videomax.domain.repository.FavoritesRepository
import com.example.videomax.domain.repository.HistoryRepository
import com.example.videomax.domain.repository.PlaylistRepository
import com.example.videomax.domain.repository.SettingsRepository
import com.example.videomax.domain.repository.VideoRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

	@Provides
	@Singleton
	fun provideDatabase(@ApplicationContext context: Context): VideoDatabase =
		Room.databaseBuilder(
			context,
			VideoDatabase::class.java,
			"video_player_pro.db"
		).fallbackToDestructiveMigration(dropAllTables = true)
			.build()

	@Provides
	fun provideVideoDao(db: VideoDatabase): VideoDao = db.videoDao()

	@Provides
	fun providePlaylistDao(db: VideoDatabase): PlaylistDao = db.playlistDao()

	@Provides
	fun provideHistoryDao(db: VideoDatabase): HistoryDao = db.historyDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

	@Binds
	@Singleton
	abstract fun bindVideoRepository(impl: VideoRepositoryImpl): VideoRepository

	@Binds
	@Singleton
	abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

	@Binds
	@Singleton
	abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository

	@Binds
	@Singleton
	abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

	@Binds
	@Singleton
	abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

	@Provides
	@Singleton
	fun provideExoPlayer(@ApplicationContext context: Context): ExoPlayer =
		ExoPlayer.Builder(context).build().apply {
			playWhenReady = true
			repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
		}
}
