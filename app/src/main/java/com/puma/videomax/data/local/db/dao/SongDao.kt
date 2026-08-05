package com.puma.videomax.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.puma.videomax.data.local.db.entity.ScanKeepSongIdEntity
import com.puma.videomax.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

data class SongUserState(
	val id: Long,
	val isFavorite: Boolean,
	val lastPositionMs: Long,
	val playCount: Int
)

@Dao
interface SongDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun upsertAll(songs: List<SongEntity>)

	@Query("DELETE FROM songs")
	suspend fun clearAll()

	@Query("SELECT * FROM songs WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): SongEntity?

	@Query("SELECT id, isFavorite, lastPositionMs, playCount FROM songs")
	suspend fun getUserStates(): List<SongUserState>

	@Query("SELECT id, isFavorite, lastPositionMs, playCount FROM songs WHERE id IN (:ids)")
	suspend fun getUserStatesForIds(ids: List<Long>): List<SongUserState>

	@Query("SELECT * FROM songs WHERE id IN (:ids)")
	suspend fun getByIds(ids: List<Long>): List<SongEntity>

	@Query("SELECT * FROM songs ORDER BY dateAdded DESC")
	suspend fun getAllByDateDesc(): List<SongEntity>

	// --- pagingSongs (no search) ---
	@Query("SELECT * FROM songs ORDER BY dateAdded DESC")
	fun pagingSongsDateDesc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY dateAdded ASC")
	fun pagingSongsDateAsc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
	fun pagingSongsTitleAsc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE DESC")
	fun pagingSongsTitleDesc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY artist COLLATE NOCASE ASC")
	fun pagingSongsArtistAsc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY artist COLLATE NOCASE DESC")
	fun pagingSongsArtistDesc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY album COLLATE NOCASE ASC")
	fun pagingSongsAlbumAsc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY album COLLATE NOCASE DESC")
	fun pagingSongsAlbumDesc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY durationMs DESC")
	fun pagingSongsDurationDesc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY durationMs ASC")
	fun pagingSongsDurationAsc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY sizeBytes DESC")
	fun pagingSongsSizeDesc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY sizeBytes ASC")
	fun pagingSongsSizeAsc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY trackNumber ASC, discNumber ASC")
	fun pagingSongsTrackAsc(): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs ORDER BY trackNumber DESC, discNumber DESC")
	fun pagingSongsTrackDesc(): PagingSource<Int, SongEntity>

	// --- pagingSongs FTS search ---
	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.dateAdded DESC
		"""
	)
	fun pagingSearchDateDesc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.dateAdded ASC
		"""
	)
	fun pagingSearchDateAsc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.title COLLATE NOCASE ASC
		"""
	)
	fun pagingSearchTitleAsc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.title COLLATE NOCASE DESC
		"""
	)
	fun pagingSearchTitleDesc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.artist COLLATE NOCASE ASC
		"""
	)
	fun pagingSearchArtistAsc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.artist COLLATE NOCASE DESC
		"""
	)
	fun pagingSearchArtistDesc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.album COLLATE NOCASE ASC
		"""
	)
	fun pagingSearchAlbumAsc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.album COLLATE NOCASE DESC
		"""
	)
	fun pagingSearchAlbumDesc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.durationMs DESC
		"""
	)
	fun pagingSearchDurationDesc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.durationMs ASC
		"""
	)
	fun pagingSearchDurationAsc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.sizeBytes DESC
		"""
	)
	fun pagingSearchSizeDesc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.sizeBytes ASC
		"""
	)
	fun pagingSearchSizeAsc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.trackNumber ASC, songs.discNumber ASC
		"""
	)
	fun pagingSearchTrackAsc(ftsQuery: String): PagingSource<Int, SongEntity>

	@Query(
		"""
		SELECT songs.* FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.trackNumber DESC, songs.discNumber DESC
		"""
	)
	fun pagingSearchTrackDesc(ftsQuery: String): PagingSource<Int, SongEntity>

	// --- pagingByFolder ---
	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY dateAdded DESC")
	fun pagingByFolderDateDesc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY dateAdded ASC")
	fun pagingByFolderDateAsc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY title COLLATE NOCASE ASC")
	fun pagingByFolderTitleAsc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY title COLLATE NOCASE DESC")
	fun pagingByFolderTitleDesc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY artist COLLATE NOCASE ASC")
	fun pagingByFolderArtistAsc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY artist COLLATE NOCASE DESC")
	fun pagingByFolderArtistDesc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY album COLLATE NOCASE ASC")
	fun pagingByFolderAlbumAsc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY album COLLATE NOCASE DESC")
	fun pagingByFolderAlbumDesc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY durationMs DESC")
	fun pagingByFolderDurationDesc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY durationMs ASC")
	fun pagingByFolderDurationAsc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY sizeBytes DESC")
	fun pagingByFolderSizeDesc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY sizeBytes ASC")
	fun pagingByFolderSizeAsc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY trackNumber ASC, discNumber ASC")
	fun pagingByFolderTrackAsc(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY trackNumber DESC, discNumber DESC")
	fun pagingByFolderTrackDesc(folder: String): PagingSource<Int, SongEntity>

	// --- getSongIds (no search) ---
	@Query("SELECT id FROM songs ORDER BY dateAdded DESC")
	suspend fun getSongIdsDateDesc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY dateAdded ASC")
	suspend fun getSongIdsDateAsc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY title COLLATE NOCASE ASC")
	suspend fun getSongIdsTitleAsc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY title COLLATE NOCASE DESC")
	suspend fun getSongIdsTitleDesc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY artist COLLATE NOCASE ASC")
	suspend fun getSongIdsArtistAsc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY artist COLLATE NOCASE DESC")
	suspend fun getSongIdsArtistDesc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY album COLLATE NOCASE ASC")
	suspend fun getSongIdsAlbumAsc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY album COLLATE NOCASE DESC")
	suspend fun getSongIdsAlbumDesc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY durationMs DESC")
	suspend fun getSongIdsDurationDesc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY durationMs ASC")
	suspend fun getSongIdsDurationAsc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY sizeBytes DESC")
	suspend fun getSongIdsSizeDesc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY sizeBytes ASC")
	suspend fun getSongIdsSizeAsc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY trackNumber ASC, discNumber ASC")
	suspend fun getSongIdsTrackAsc(): List<Long>

	@Query("SELECT id FROM songs ORDER BY trackNumber DESC, discNumber DESC")
	suspend fun getSongIdsTrackDesc(): List<Long>

	// --- getSongIds FTS search ---
	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.dateAdded DESC
		"""
	)
	suspend fun getSearchIdsDateDesc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.dateAdded ASC
		"""
	)
	suspend fun getSearchIdsDateAsc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.title COLLATE NOCASE ASC
		"""
	)
	suspend fun getSearchIdsTitleAsc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.title COLLATE NOCASE DESC
		"""
	)
	suspend fun getSearchIdsTitleDesc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.artist COLLATE NOCASE ASC
		"""
	)
	suspend fun getSearchIdsArtistAsc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.artist COLLATE NOCASE DESC
		"""
	)
	suspend fun getSearchIdsArtistDesc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.album COLLATE NOCASE ASC
		"""
	)
	suspend fun getSearchIdsAlbumAsc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.album COLLATE NOCASE DESC
		"""
	)
	suspend fun getSearchIdsAlbumDesc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.durationMs DESC
		"""
	)
	suspend fun getSearchIdsDurationDesc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.durationMs ASC
		"""
	)
	suspend fun getSearchIdsDurationAsc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.sizeBytes DESC
		"""
	)
	suspend fun getSearchIdsSizeDesc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.sizeBytes ASC
		"""
	)
	suspend fun getSearchIdsSizeAsc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.trackNumber ASC, songs.discNumber ASC
		"""
	)
	suspend fun getSearchIdsTrackAsc(ftsQuery: String): List<Long>

	@Query(
		"""
		SELECT songs.id FROM songs
		JOIN songs_fts ON songs.rowid = songs_fts.rowid
		WHERE songs_fts MATCH :ftsQuery
		ORDER BY songs.trackNumber DESC, songs.discNumber DESC
		"""
	)
	suspend fun getSearchIdsTrackDesc(ftsQuery: String): List<Long>

	// --- getSongIdsByFolder ---
	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY dateAdded DESC")
	suspend fun getSongIdsByFolderDateDesc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY dateAdded ASC")
	suspend fun getSongIdsByFolderDateAsc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY title COLLATE NOCASE ASC")
	suspend fun getSongIdsByFolderTitleAsc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY title COLLATE NOCASE DESC")
	suspend fun getSongIdsByFolderTitleDesc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY artist COLLATE NOCASE ASC")
	suspend fun getSongIdsByFolderArtistAsc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY artist COLLATE NOCASE DESC")
	suspend fun getSongIdsByFolderArtistDesc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY album COLLATE NOCASE ASC")
	suspend fun getSongIdsByFolderAlbumAsc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY album COLLATE NOCASE DESC")
	suspend fun getSongIdsByFolderAlbumDesc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY durationMs DESC")
	suspend fun getSongIdsByFolderDurationDesc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY durationMs ASC")
	suspend fun getSongIdsByFolderDurationAsc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY sizeBytes DESC")
	suspend fun getSongIdsByFolderSizeDesc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY sizeBytes ASC")
	suspend fun getSongIdsByFolderSizeAsc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY trackNumber ASC, discNumber ASC")
	suspend fun getSongIdsByFolderTrackAsc(folder: String): List<Long>

	@Query("SELECT id FROM songs WHERE folderName = :folder ORDER BY trackNumber DESC, discNumber DESC")
	suspend fun getSongIdsByFolderTrackDesc(folder: String): List<Long>

	// --- Albums ---
	@Query(
		"""
		SELECT album, albumId, artist, MIN(dateAdded) AS dateAdded, COUNT(*) AS songCount
		FROM songs
		GROUP BY albumId
		ORDER BY album COLLATE NOCASE ASC
		"""
	)
	fun observeAlbums(): Flow<List<AlbumInfo>>

	@Query(
		"""
		SELECT album, albumId, artist, MIN(dateAdded) AS dateAdded, COUNT(*) AS songCount
		FROM songs
		WHERE albumId = :albumId
		GROUP BY albumId
		"""
	)
	suspend fun getAlbumInfo(albumId: Long): AlbumInfo?

	@Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY discNumber ASC, trackNumber ASC")
	fun pagingAlbumSongs(albumId: Long): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY discNumber ASC, trackNumber ASC")
	suspend fun getAlbumSongs(albumId: Long): List<SongEntity>

	// --- Artists ---
	@Query(
		"""
		SELECT artist, COUNT(*) AS songCount, COUNT(DISTINCT albumId) AS albumCount
		FROM songs
		GROUP BY artist
		ORDER BY artist COLLATE NOCASE ASC
		"""
	)
	fun observeArtists(): Flow<List<ArtistInfo>>

	@Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC, discNumber ASC, trackNumber ASC")
	fun pagingArtistSongs(artist: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album COLLATE NOCASE ASC, discNumber ASC, trackNumber ASC")
	suspend fun getArtistSongs(artist: String): List<SongEntity>

	// --- Genres (from folder as fallback) ---
	@Query("SELECT DISTINCT folderName FROM songs ORDER BY folderName COLLATE NOCASE ASC")
	fun observeFolders(): Flow<List<String>>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY title COLLATE NOCASE ASC")
	fun pagingFolderSongs(folder: String): PagingSource<Int, SongEntity>

	@Query("SELECT * FROM songs WHERE folderName = :folder ORDER BY title COLLATE NOCASE ASC")
	suspend fun getFolderSongs(folder: String): List<SongEntity>

	// --- Favorites ---
	@Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
	fun observeFavorites(): Flow<List<SongEntity>>

	@Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title COLLATE NOCASE ASC")
	fun pagingFavorites(): PagingSource<Int, SongEntity>

	// --- Most Played ---
	@Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC, dateModified DESC LIMIT :limit")
	fun observeMostPlayed(limit: Int = 100): Flow<List<SongEntity>>

	@Query("SELECT * FROM songs WHERE playCount > 0 ORDER BY playCount DESC, dateModified DESC")
	fun pagingMostPlayed(): PagingSource<Int, SongEntity>

	// --- Updates ---
	@Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
	suspend fun updateFavorite(songId: Long, isFavorite: Boolean)

	@Query("SELECT isFavorite FROM songs WHERE id = :songId LIMIT 1")
	suspend fun isFavorite(songId: Long): Boolean?

	@Query("UPDATE songs SET lastPositionMs = :positionMs WHERE id = :songId")
	suspend fun updateLastPosition(songId: Long, positionMs: Long)

	@Query("UPDATE songs SET playCount = playCount + 1 WHERE id = :songId")
	suspend fun incrementPlayCount(songId: Long)

	@Query("UPDATE songs SET title = :title WHERE id = :songId")
	suspend fun updateTitle(songId: Long, title: String)

	@Query("SELECT COUNT(*) FROM songs")
	fun observeCount(): Flow<Int>

	@Query("SELECT COUNT(*) FROM songs")
	suspend fun count(): Int

	@Query("DELETE FROM songs WHERE id IN (:ids)")
	suspend fun deleteByIds(ids: List<Long>)

	// --- Scan keep-set ---
	@Query("DELETE FROM scan_keep_song_ids")
	suspend fun clearScanKeepIds()

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertScanKeepIds(ids: List<ScanKeepSongIdEntity>)

	@Query("DELETE FROM songs WHERE id NOT IN (SELECT id FROM scan_keep_song_ids)")
	suspend fun deleteSongsNotInScanKeepSet()

	@RawQuery
	suspend fun queryIds(query: SupportSQLiteQuery): List<Long>
}

data class AlbumInfo(
	val album: String,
	val albumId: Long,
	val artist: String,
	val dateAdded: Long,
	val songCount: Int
)

data class ArtistInfo(
	val artist: String,
	val songCount: Int,
	val albumCount: Int
)
