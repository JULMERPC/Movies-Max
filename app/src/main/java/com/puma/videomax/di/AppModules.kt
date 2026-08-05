package com.puma.videomax.di

import android.content.Context
import androidx.room.Room
import com.puma.videomax.data.local.db.VideoDatabase
import com.puma.videomax.data.local.db.dao.HistoryDao
import com.puma.videomax.data.local.db.dao.MusicQueueDao
import com.puma.videomax.data.local.db.dao.PlaylistDao
import com.puma.videomax.data.local.db.dao.SongDao
import com.puma.videomax.data.local.db.dao.SongStatsDao
import com.puma.videomax.data.local.db.dao.VideoDao
import com.puma.videomax.data.repository.FavoritesRepositoryImpl
import com.puma.videomax.data.repository.HistoryRepositoryImpl
import com.puma.videomax.data.repository.PlaylistRepositoryImpl
import com.puma.videomax.data.repository.SettingsRepositoryImpl
import com.puma.videomax.data.repository.SongRepositoryImpl
import com.puma.videomax.data.repository.VideoRepositoryImpl
import com.puma.videomax.domain.repository.FavoritesRepository
import com.puma.videomax.domain.repository.HistoryRepository
import com.puma.videomax.domain.repository.PlaylistRepository
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.domain.repository.SongRepository
import com.puma.videomax.domain.repository.VideoRepository
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
		)
			.addMigrations(VideoDatabase.MIGRATION_5_6)
			.addMigrations(VideoDatabase.MIGRATION_6_7)
			.addMigrations(VideoDatabase.MIGRATION_7_8)
			.fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
			.build()

	@Provides
	fun provideVideoDao(db: VideoDatabase): VideoDao = db.videoDao()

	@Provides
	fun providePlaylistDao(db: VideoDatabase): PlaylistDao = db.playlistDao()

	@Provides
	fun provideHistoryDao(db: VideoDatabase): HistoryDao = db.historyDao()

	@Provides
	fun provideSongDao(db: VideoDatabase): SongDao = db.songDao()

	@Provides
	fun provideSongStatsDao(db: VideoDatabase): SongStatsDao = db.songStatsDao()

	@Provides
	fun provideMusicQueueDao(db: VideoDatabase): MusicQueueDao = db.musicQueueDao()
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

	@Binds
	@Singleton
	abstract fun bindSongRepository(impl: SongRepositoryImpl): SongRepository
}
