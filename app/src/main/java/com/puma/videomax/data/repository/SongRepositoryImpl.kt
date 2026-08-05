package com.puma.videomax.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.puma.videomax.data.local.db.FtsQuery
import com.puma.videomax.data.local.db.MusicQueueWindowSql
import com.puma.videomax.data.local.db.dao.SongDao
import com.puma.videomax.data.local.db.entity.ScanKeepSongIdEntity
import com.puma.videomax.data.local.mediastore.MediaStoreMusicScanner
import com.puma.videomax.data.mapper.toDomain
import com.puma.videomax.domain.model.Album
import com.puma.videomax.domain.model.Artist
import com.puma.videomax.domain.model.MusicSortOption
import com.puma.videomax.domain.model.Song
import com.puma.videomax.domain.repository.MusicQueueWindow
import com.puma.videomax.domain.repository.SettingsRepository
import com.puma.videomax.domain.repository.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SongRepositoryImpl @Inject constructor(
	@param:ApplicationContext private val context: Context,
	private val songDao: SongDao,
	private val scanner: MediaStoreMusicScanner,
	private val settingsRepository: SettingsRepository
) : SongRepository {

	private val pagingConfig = PagingConfig(
		pageSize = 40,
		prefetchDistance = 20,
		enablePlaceholders = false,
		initialLoadSize = 60
	)

	override fun pagingSongs(query: String, sortOption: MusicSortOption): Flow<PagingData<Song>> =
		Pager(pagingConfig) {
			val fts = FtsQuery.fromUserInput(query)
			if (fts == null) {
				when (sortOption) {
					MusicSortOption.DATE_DESC -> songDao.pagingSongsDateDesc()
					MusicSortOption.DATE_ASC -> songDao.pagingSongsDateAsc()
					MusicSortOption.TITLE_ASC -> songDao.pagingSongsTitleAsc()
					MusicSortOption.TITLE_DESC -> songDao.pagingSongsTitleDesc()
					MusicSortOption.ARTIST_ASC -> songDao.pagingSongsArtistAsc()
					MusicSortOption.ARTIST_DESC -> songDao.pagingSongsArtistDesc()
					MusicSortOption.ALBUM_ASC -> songDao.pagingSongsAlbumAsc()
					MusicSortOption.ALBUM_DESC -> songDao.pagingSongsAlbumDesc()
					MusicSortOption.DURATION_DESC -> songDao.pagingSongsDurationDesc()
					MusicSortOption.DURATION_ASC -> songDao.pagingSongsDurationAsc()
					MusicSortOption.SIZE_DESC -> songDao.pagingSongsSizeDesc()
					MusicSortOption.SIZE_ASC -> songDao.pagingSongsSizeAsc()
					MusicSortOption.TRACK_NUMBER_ASC -> songDao.pagingSongsTrackAsc()
					MusicSortOption.TRACK_NUMBER_DESC -> songDao.pagingSongsTrackDesc()
				}
			} else {
				when (sortOption) {
					MusicSortOption.DATE_DESC -> songDao.pagingSearchDateDesc(fts)
					MusicSortOption.DATE_ASC -> songDao.pagingSearchDateAsc(fts)
					MusicSortOption.TITLE_ASC -> songDao.pagingSearchTitleAsc(fts)
					MusicSortOption.TITLE_DESC -> songDao.pagingSearchTitleDesc(fts)
					MusicSortOption.ARTIST_ASC -> songDao.pagingSearchArtistAsc(fts)
					MusicSortOption.ARTIST_DESC -> songDao.pagingSearchArtistDesc(fts)
					MusicSortOption.ALBUM_ASC -> songDao.pagingSearchAlbumAsc(fts)
					MusicSortOption.ALBUM_DESC -> songDao.pagingSearchAlbumDesc(fts)
					MusicSortOption.DURATION_DESC -> songDao.pagingSearchDurationDesc(fts)
					MusicSortOption.DURATION_ASC -> songDao.pagingSearchDurationAsc(fts)
					MusicSortOption.SIZE_DESC -> songDao.pagingSearchSizeDesc(fts)
					MusicSortOption.SIZE_ASC -> songDao.pagingSearchSizeAsc(fts)
					MusicSortOption.TRACK_NUMBER_ASC -> songDao.pagingSearchTrackAsc(fts)
					MusicSortOption.TRACK_NUMBER_DESC -> songDao.pagingSearchTrackDesc(fts)
				}
			}
		}.flow
			.map { paging -> paging.map { it.toDomain() } }
			.flowOn(Dispatchers.IO)

	override fun pagingSongsByFolder(folder: String, sortOption: MusicSortOption): Flow<PagingData<Song>> =
		Pager(pagingConfig) {
			when (sortOption) {
				MusicSortOption.DATE_DESC -> songDao.pagingByFolderDateDesc(folder)
				MusicSortOption.DATE_ASC -> songDao.pagingByFolderDateAsc(folder)
				MusicSortOption.TITLE_ASC -> songDao.pagingByFolderTitleAsc(folder)
				MusicSortOption.TITLE_DESC -> songDao.pagingByFolderTitleDesc(folder)
				MusicSortOption.ARTIST_ASC -> songDao.pagingByFolderArtistAsc(folder)
				MusicSortOption.ARTIST_DESC -> songDao.pagingByFolderArtistDesc(folder)
				MusicSortOption.ALBUM_ASC -> songDao.pagingByFolderAlbumAsc(folder)
				MusicSortOption.ALBUM_DESC -> songDao.pagingByFolderAlbumDesc(folder)
				MusicSortOption.DURATION_DESC -> songDao.pagingByFolderDurationDesc(folder)
				MusicSortOption.DURATION_ASC -> songDao.pagingByFolderDurationAsc(folder)
				MusicSortOption.SIZE_DESC -> songDao.pagingByFolderSizeDesc(folder)
				MusicSortOption.SIZE_ASC -> songDao.pagingByFolderSizeAsc(folder)
				MusicSortOption.TRACK_NUMBER_ASC -> songDao.pagingByFolderTrackAsc(folder)
				MusicSortOption.TRACK_NUMBER_DESC -> songDao.pagingByFolderTrackDesc(folder)
			}
		}.flow
			.map { paging -> paging.map { it.toDomain() } }
			.flowOn(Dispatchers.IO)

	override fun observeFolders(): Flow<List<String>> = songDao.observeFolders()

	override fun observeSongCount(): Flow<Int> = songDao.observeCount()

	override fun observeMostPlayed(limit: Int): Flow<List<Song>> =
		songDao.observeMostPlayed(limit).map { list -> list.map { it.toDomain() } }

	override fun pagingMostPlayed(): Flow<PagingData<Song>> =
		Pager(pagingConfig) { songDao.pagingMostPlayed() }
			.flow.map { paging -> paging.map { it.toDomain() } }

	override fun pagingFavorites(): Flow<PagingData<Song>> =
		Pager(pagingConfig) { songDao.pagingFavorites() }
			.flow.map { paging -> paging.map { it.toDomain() } }

	override suspend fun getSongIds(
		query: String,
		sortOption: MusicSortOption,
		folder: String?
	): List<Long> = withContext(Dispatchers.IO) {
		if (folder != null) {
			when (sortOption) {
				MusicSortOption.DATE_DESC -> songDao.getSongIdsByFolderDateDesc(folder)
				MusicSortOption.DATE_ASC -> songDao.getSongIdsByFolderDateAsc(folder)
				MusicSortOption.TITLE_ASC -> songDao.getSongIdsByFolderTitleAsc(folder)
				MusicSortOption.TITLE_DESC -> songDao.getSongIdsByFolderTitleDesc(folder)
				MusicSortOption.ARTIST_ASC -> songDao.getSongIdsByFolderArtistAsc(folder)
				MusicSortOption.ARTIST_DESC -> songDao.getSongIdsByFolderArtistDesc(folder)
				MusicSortOption.ALBUM_ASC -> songDao.getSongIdsByFolderAlbumAsc(folder)
				MusicSortOption.ALBUM_DESC -> songDao.getSongIdsByFolderAlbumDesc(folder)
				MusicSortOption.DURATION_DESC -> songDao.getSongIdsByFolderDurationDesc(folder)
				MusicSortOption.DURATION_ASC -> songDao.getSongIdsByFolderDurationAsc(folder)
				MusicSortOption.SIZE_DESC -> songDao.getSongIdsByFolderSizeDesc(folder)
				MusicSortOption.SIZE_ASC -> songDao.getSongIdsByFolderSizeAsc(folder)
				MusicSortOption.TRACK_NUMBER_ASC -> songDao.getSongIdsByFolderTrackAsc(folder)
				MusicSortOption.TRACK_NUMBER_DESC -> songDao.getSongIdsByFolderTrackDesc(folder)
			}
		} else {
			val fts = FtsQuery.fromUserInput(query)
			if (fts == null) {
				when (sortOption) {
					MusicSortOption.DATE_DESC -> songDao.getSongIdsDateDesc()
					MusicSortOption.DATE_ASC -> songDao.getSongIdsDateAsc()
					MusicSortOption.TITLE_ASC -> songDao.getSongIdsTitleAsc()
					MusicSortOption.TITLE_DESC -> songDao.getSongIdsTitleDesc()
					MusicSortOption.ARTIST_ASC -> songDao.getSongIdsArtistAsc()
					MusicSortOption.ARTIST_DESC -> songDao.getSongIdsArtistDesc()
					MusicSortOption.ALBUM_ASC -> songDao.getSongIdsAlbumAsc()
					MusicSortOption.ALBUM_DESC -> songDao.getSongIdsAlbumDesc()
					MusicSortOption.DURATION_DESC -> songDao.getSongIdsDurationDesc()
					MusicSortOption.DURATION_ASC -> songDao.getSongIdsDurationAsc()
					MusicSortOption.SIZE_DESC -> songDao.getSongIdsSizeDesc()
					MusicSortOption.SIZE_ASC -> songDao.getSongIdsSizeAsc()
					MusicSortOption.TRACK_NUMBER_ASC -> songDao.getSongIdsTrackAsc()
					MusicSortOption.TRACK_NUMBER_DESC -> songDao.getSongIdsTrackDesc()
				}
			} else {
				when (sortOption) {
					MusicSortOption.DATE_DESC -> songDao.getSearchIdsDateDesc(fts)
					MusicSortOption.DATE_ASC -> songDao.getSearchIdsDateAsc(fts)
					MusicSortOption.TITLE_ASC -> songDao.getSearchIdsTitleAsc(fts)
					MusicSortOption.TITLE_DESC -> songDao.getSearchIdsTitleDesc(fts)
					MusicSortOption.ARTIST_ASC -> songDao.getSearchIdsArtistAsc(fts)
					MusicSortOption.ARTIST_DESC -> songDao.getSearchIdsArtistDesc(fts)
					MusicSortOption.ALBUM_ASC -> songDao.getSearchIdsAlbumAsc(fts)
					MusicSortOption.ALBUM_DESC -> songDao.getSearchIdsAlbumDesc(fts)
					MusicSortOption.DURATION_DESC -> songDao.getSearchIdsDurationDesc(fts)
					MusicSortOption.DURATION_ASC -> songDao.getSearchIdsDurationAsc(fts)
					MusicSortOption.SIZE_DESC -> songDao.getSearchIdsSizeDesc(fts)
					MusicSortOption.SIZE_ASC -> songDao.getSearchIdsSizeAsc(fts)
					MusicSortOption.TRACK_NUMBER_ASC -> songDao.getSearchIdsTrackAsc(fts)
					MusicSortOption.TRACK_NUMBER_DESC -> songDao.getSearchIdsTrackDesc(fts)
				}
			}
		}
	}

	override suspend fun getSongById(id: Long): Song? = songDao.getById(id)?.toDomain()

	override suspend fun getSongsByIds(ids: List<Long>): List<Song> = withContext(Dispatchers.IO) {
		if (ids.isEmpty()) return@withContext emptyList()
		val entities = songDao.getByIds(ids)
		val entityMap = entities.associateBy { it.id }
		ids.mapNotNull { entityMap[it]?.toDomain() }
	}

	override suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
		songDao.observeAlbums().first().map { it.toDomain() }
	}

	override suspend fun getAlbumSongs(albumId: Long): List<Song> = withContext(Dispatchers.IO) {
		songDao.getAlbumSongs(albumId).map { it.toDomain() }
	}

	override suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
		songDao.observeArtists().first().map { it.toDomain() }
	}

	override suspend fun getArtistSongs(artist: String): List<Song> = withContext(Dispatchers.IO) {
		songDao.getArtistSongs(artist).map { it.toDomain() }
	}

	override suspend fun getMusicQueueWindow(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		anchorId: Long,
		before: Int,
		after: Int
	): MusicQueueWindow = withContext(Dispatchers.IO) {
		val entity = songDao.getById(anchorId)
			?: return@withContext MusicQueueWindow(listOf(anchorId), hasMoreBefore = false, hasMoreAfter = false)
		val keys = MusicQueueWindowSql.fromEntity(entity)
		val q = query.trim()

		val beforeRaw = if (before > 0) {
			songDao.queryIds(MusicQueueWindowSql.neighborsBefore(sortOption, folder, q, keys, before))
		} else emptyList()
		val beforeIds = beforeRaw.asReversed()

		val afterIds = if (after > 0) {
			songDao.queryIds(MusicQueueWindowSql.neighborsAfter(sortOption, folder, q, keys, after))
		} else emptyList()

		MusicQueueWindow(
			ids = beforeIds + anchorId + afterIds,
			hasMoreBefore = beforeRaw.size >= before,
			hasMoreAfter = afterIds.size >= after
		)
	}

	override suspend fun getMusicQueueNeighborsBefore(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		val entity = songDao.getById(anchorId) ?: return@withContext emptyList()
		val keys = MusicQueueWindowSql.fromEntity(entity)
		songDao.queryIds(
			MusicQueueWindowSql.neighborsBefore(sortOption, folder, query.trim(), keys, limit)
		).asReversed()
	}

	override suspend fun getMusicQueueNeighborsAfter(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		anchorId: Long,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		val entity = songDao.getById(anchorId) ?: return@withContext emptyList()
		val keys = MusicQueueWindowSql.fromEntity(entity)
		songDao.queryIds(
			MusicQueueWindowSql.neighborsAfter(sortOption, folder, query.trim(), keys, limit)
		)
	}

	override suspend fun getMusicQueueFirstIds(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		songDao.queryIds(MusicQueueWindowSql.firstPage(sortOption, folder, query.trim(), limit))
	}

	override suspend fun getMusicQueueLastIds(
		query: String,
		sortOption: MusicSortOption,
		folder: String?,
		limit: Int
	): List<Long> = withContext(Dispatchers.IO) {
		if (limit <= 0) return@withContext emptyList()
		songDao.queryIds(MusicQueueWindowSql.lastPage(sortOption, folder, query.trim(), limit)).asReversed()
	}

	override suspend fun scanDeviceMusic(
		onProgress: (suspend (indexed: Int, totalHint: Int) -> Unit)?
	): Int = withContext(Dispatchers.IO) {
		val settings = settingsRepository.settings.first()
		val isFirstScan = settings.lastScanTimestamp == 0L
		var indexed = 0
		var totalHint = 0
		var receivedAny = false
		var keptAny = false

		songDao.clearScanKeepIds()

		scanner.scanBatches(BATCH_SIZE).collect { batch ->
			receivedAny = true
			totalHint = batch.totalHint

			val ids = batch.songs.map { it.id }
			val userState = if (ids.isEmpty()) {
				emptyMap()
			} else {
				songDao.getUserStatesForIds(ids).associateBy { it.id }
			}

			val merged = batch.songs.mapNotNull { fresh ->
				if (fresh.path?.contains(".nomedia", ignoreCase = true) == true) return@mapNotNull null
				val state = userState[fresh.id]
				if (state != null) {
					fresh.copy(
						isFavorite = state.isFavorite,
						lastPositionMs = state.lastPositionMs,
						playCount = state.playCount
					)
				} else {
					fresh
				}
			}

			if (merged.isNotEmpty()) {
				songDao.upsertAll(merged)
				songDao.insertScanKeepIds(merged.map { ScanKeepSongIdEntity(it.id) })
				keptAny = true
				indexed += merged.size
				onProgress?.invoke(indexed, totalHint.coerceAtLeast(indexed))
			} else {
				onProgress?.invoke(indexed, totalHint.coerceAtLeast(indexed))
			}
			yield()
		}

		if (!receivedAny || !keptAny) {
			songDao.clearAll()
			songDao.clearScanKeepIds()
			onProgress?.invoke(0, 0)
			return@withContext 0
		}

		songDao.deleteSongsNotInScanKeepSet()
		songDao.clearScanKeepIds()
		yield()

		onProgress?.invoke(indexed, indexed)
		indexed
	}

	override suspend fun updateFavorite(songId: Long, isFavorite: Boolean) {
		songDao.updateFavorite(songId, isFavorite)
	}

	override suspend fun updateLastPosition(songId: Long, positionMs: Long) {
		songDao.updateLastPosition(songId, positionMs)
	}

	override suspend fun incrementPlayCount(songId: Long) {
		songDao.incrementPlayCount(songId)
	}

	override suspend fun deleteSong(songId: Long) = withContext(Dispatchers.IO) {
		val song = songDao.getById(songId) ?: return@withContext
		val uri = Uri.parse(song.uri)
		try {
			context.contentResolver.delete(uri, null, null)
		} catch (_: Exception) { }
		songDao.deleteByIds(listOf(songId))
	}

	override suspend fun renameSong(songId: Long, newTitle: String) = withContext(Dispatchers.IO) {
		val song = songDao.getById(songId) ?: return@withContext
		val uri = Uri.parse(song.uri)
		val values = ContentValues().apply {
			put(MediaStore.Audio.Media.TITLE, newTitle)
		}
		try {
			context.contentResolver.update(uri, values, null, null)
		} catch (_: Exception) { }
		songDao.updateTitle(songId, newTitle)
	}

	private companion object {
		const val BATCH_SIZE = 64
	}
}
