package com.puma.videomax.data.local.db

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.puma.videomax.data.local.db.entity.SongEntity
import com.puma.videomax.domain.model.MusicSortOption

/**
 * Builds keyset SQL for lazy music playback-queue windows.
 * Primary order matches Library paging; [id] is a stable tie-breaker only.
 */
internal object MusicQueueWindowSql {

	data class SortKeys(
		val dateAdded: Long,
		val title: String,
		val artist: String,
		val album: String,
		val durationMs: Long,
		val sizeBytes: Long,
		val trackNumber: Int,
		val discNumber: Int,
		val id: Long
	)

	fun fromEntity(entity: SongEntity) = SortKeys(
		dateAdded = entity.dateAdded,
		title = entity.title,
		artist = entity.artist,
		album = entity.album,
		durationMs = entity.durationMs,
		sizeBytes = entity.sizeBytes,
		trackNumber = entity.trackNumber,
		discNumber = entity.discNumber,
		id = entity.id
	)

	fun neighborsAfter(
		sortOption: MusicSortOption,
		folder: String?,
		query: String,
		keys: SortKeys,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val (cmp, cmpArgs, order) = afterPredicate(sortOption, keys)
		val sql = "SELECT id FROM songs WHERE $filter AND ($cmp) ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + cmpArgs + limit).toTypedArray())
	}

	fun neighborsBefore(
		sortOption: MusicSortOption,
		folder: String?,
		query: String,
		keys: SortKeys,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val (cmp, cmpArgs, order) = beforePredicate(sortOption, keys)
		val sql = "SELECT id FROM songs WHERE $filter AND ($cmp) ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + cmpArgs + limit).toTypedArray())
	}

	fun firstPage(
		sortOption: MusicSortOption,
		folder: String?,
		query: String,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val order = primaryOrder(sortOption)
		val sql = "SELECT id FROM songs WHERE $filter ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + limit).toTypedArray())
	}

	fun lastPage(
		sortOption: MusicSortOption,
		folder: String?,
		query: String,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val order = reverseOrder(sortOption)
		val sql = "SELECT id FROM songs WHERE $filter ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + limit).toTypedArray())
	}

	private fun baseFilter(folder: String?, query: String): Pair<String, List<Any>> {
		return if (folder != null) {
			"folderName = ?" to listOf(folder)
		} else {
			val fts = FtsQuery.fromUserInput(query)
			if (fts == null) {
				"1 = 1" to emptyList()
			} else {
				"rowid IN (SELECT rowid FROM songs_fts WHERE songs_fts MATCH ?)" to listOf(fts)
			}
		}
	}

	private fun primaryOrder(sort: MusicSortOption): String = when (sort) {
		MusicSortOption.DATE_DESC -> "dateAdded DESC, id DESC"
		MusicSortOption.DATE_ASC -> "dateAdded ASC, id ASC"
		MusicSortOption.TITLE_ASC -> "title COLLATE NOCASE ASC, id ASC"
		MusicSortOption.TITLE_DESC -> "title COLLATE NOCASE DESC, id DESC"
		MusicSortOption.ARTIST_ASC -> "artist COLLATE NOCASE ASC, id ASC"
		MusicSortOption.ARTIST_DESC -> "artist COLLATE NOCASE DESC, id DESC"
		MusicSortOption.ALBUM_ASC -> "album COLLATE NOCASE ASC, id ASC"
		MusicSortOption.ALBUM_DESC -> "album COLLATE NOCASE DESC, id DESC"
		MusicSortOption.DURATION_DESC -> "durationMs DESC, id DESC"
		MusicSortOption.DURATION_ASC -> "durationMs ASC, id ASC"
		MusicSortOption.SIZE_DESC -> "sizeBytes DESC, id DESC"
		MusicSortOption.SIZE_ASC -> "sizeBytes ASC, id ASC"
		MusicSortOption.TRACK_NUMBER_ASC -> "trackNumber ASC, discNumber ASC, id ASC"
		MusicSortOption.TRACK_NUMBER_DESC -> "trackNumber DESC, discNumber DESC, id DESC"
	}

	private fun reverseOrder(sort: MusicSortOption): String = when (sort) {
		MusicSortOption.DATE_DESC -> "dateAdded ASC, id ASC"
		MusicSortOption.DATE_ASC -> "dateAdded DESC, id DESC"
		MusicSortOption.TITLE_ASC -> "title COLLATE NOCASE DESC, id DESC"
		MusicSortOption.TITLE_DESC -> "title COLLATE NOCASE ASC, id ASC"
		MusicSortOption.ARTIST_ASC -> "artist COLLATE NOCASE DESC, id DESC"
		MusicSortOption.ARTIST_DESC -> "artist COLLATE NOCASE ASC, id ASC"
		MusicSortOption.ALBUM_ASC -> "album COLLATE NOCASE DESC, id DESC"
		MusicSortOption.ALBUM_DESC -> "album COLLATE NOCASE ASC, id ASC"
		MusicSortOption.DURATION_DESC -> "durationMs ASC, id ASC"
		MusicSortOption.DURATION_ASC -> "durationMs DESC, id DESC"
		MusicSortOption.SIZE_DESC -> "sizeBytes ASC, id ASC"
		MusicSortOption.SIZE_ASC -> "sizeBytes DESC, id DESC"
		MusicSortOption.TRACK_NUMBER_ASC -> "trackNumber DESC, discNumber DESC, id DESC"
		MusicSortOption.TRACK_NUMBER_DESC -> "trackNumber ASC, discNumber ASC, id ASC"
	}

	private data class Predicate(val sql: String, val args: List<Any>, val order: String)

	private fun afterPredicate(sort: MusicSortOption, keys: SortKeys): Predicate = when (sort) {
		MusicSortOption.DATE_DESC -> Predicate(
			"dateAdded < ? OR (dateAdded = ? AND id < ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded DESC, id DESC"
		)
		MusicSortOption.DATE_ASC -> Predicate(
			"dateAdded > ? OR (dateAdded = ? AND id > ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded ASC, id ASC"
		)
		MusicSortOption.TITLE_ASC -> Predicate(
			"(title COLLATE NOCASE > ?) OR (title COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.title, keys.title, keys.id),
			"title COLLATE NOCASE ASC, id ASC"
		)
		MusicSortOption.TITLE_DESC -> Predicate(
			"(title COLLATE NOCASE < ?) OR (title COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.title, keys.title, keys.id),
			"title COLLATE NOCASE DESC, id DESC"
		)
		MusicSortOption.ARTIST_ASC -> Predicate(
			"(artist COLLATE NOCASE > ?) OR (artist COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.artist, keys.artist, keys.id),
			"artist COLLATE NOCASE ASC, id ASC"
		)
		MusicSortOption.ARTIST_DESC -> Predicate(
			"(artist COLLATE NOCASE < ?) OR (artist COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.artist, keys.artist, keys.id),
			"artist COLLATE NOCASE DESC, id DESC"
		)
		MusicSortOption.ALBUM_ASC -> Predicate(
			"(album COLLATE NOCASE > ?) OR (album COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.album, keys.album, keys.id),
			"album COLLATE NOCASE ASC, id ASC"
		)
		MusicSortOption.ALBUM_DESC -> Predicate(
			"(album COLLATE NOCASE < ?) OR (album COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.album, keys.album, keys.id),
			"album COLLATE NOCASE DESC, id DESC"
		)
		MusicSortOption.DURATION_DESC -> Predicate(
			"durationMs < ? OR (durationMs = ? AND id < ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs DESC, id DESC"
		)
		MusicSortOption.DURATION_ASC -> Predicate(
			"durationMs > ? OR (durationMs = ? AND id > ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs ASC, id ASC"
		)
		MusicSortOption.SIZE_DESC -> Predicate(
			"sizeBytes < ? OR (sizeBytes = ? AND id < ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes DESC, id DESC"
		)
		MusicSortOption.SIZE_ASC -> Predicate(
			"sizeBytes > ? OR (sizeBytes = ? AND id > ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes ASC, id ASC"
		)
		MusicSortOption.TRACK_NUMBER_ASC -> Predicate(
			"(trackNumber > ?) OR (trackNumber = ? AND discNumber > ?) OR (trackNumber = ? AND discNumber = ? AND id > ?)",
			listOf(keys.trackNumber, keys.trackNumber, keys.discNumber, keys.trackNumber, keys.discNumber, keys.id),
			"trackNumber ASC, discNumber ASC, id ASC"
		)
		MusicSortOption.TRACK_NUMBER_DESC -> Predicate(
			"(trackNumber < ?) OR (trackNumber = ? AND discNumber < ?) OR (trackNumber = ? AND discNumber = ? AND id < ?)",
			listOf(keys.trackNumber, keys.trackNumber, keys.discNumber, keys.trackNumber, keys.discNumber, keys.id),
			"trackNumber DESC, discNumber DESC, id DESC"
		)
	}

	private fun beforePredicate(sort: MusicSortOption, keys: SortKeys): Predicate = when (sort) {
		MusicSortOption.DATE_DESC -> Predicate(
			"dateAdded > ? OR (dateAdded = ? AND id > ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded ASC, id ASC"
		)
		MusicSortOption.DATE_ASC -> Predicate(
			"dateAdded < ? OR (dateAdded = ? AND id < ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded DESC, id DESC"
		)
		MusicSortOption.TITLE_ASC -> Predicate(
			"(title COLLATE NOCASE < ?) OR (title COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.title, keys.title, keys.id),
			"title COLLATE NOCASE DESC, id DESC"
		)
		MusicSortOption.TITLE_DESC -> Predicate(
			"(title COLLATE NOCASE > ?) OR (title COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.title, keys.title, keys.id),
			"title COLLATE NOCASE ASC, id ASC"
		)
		MusicSortOption.ARTIST_ASC -> Predicate(
			"(artist COLLATE NOCASE < ?) OR (artist COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.artist, keys.artist, keys.id),
			"artist COLLATE NOCASE DESC, id DESC"
		)
		MusicSortOption.ARTIST_DESC -> Predicate(
			"(artist COLLATE NOCASE > ?) OR (artist COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.artist, keys.artist, keys.id),
			"artist COLLATE NOCASE ASC, id ASC"
		)
		MusicSortOption.ALBUM_ASC -> Predicate(
			"(album COLLATE NOCASE < ?) OR (album COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.album, keys.album, keys.id),
			"album COLLATE NOCASE DESC, id DESC"
		)
		MusicSortOption.ALBUM_DESC -> Predicate(
			"(album COLLATE NOCASE > ?) OR (album COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.album, keys.album, keys.id),
			"album COLLATE NOCASE ASC, id ASC"
		)
		MusicSortOption.DURATION_DESC -> Predicate(
			"durationMs > ? OR (durationMs = ? AND id > ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs ASC, id ASC"
		)
		MusicSortOption.DURATION_ASC -> Predicate(
			"durationMs < ? OR (durationMs = ? AND id < ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs DESC, id DESC"
		)
		MusicSortOption.SIZE_DESC -> Predicate(
			"sizeBytes > ? OR (sizeBytes = ? AND id > ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes ASC, id ASC"
		)
		MusicSortOption.SIZE_ASC -> Predicate(
			"sizeBytes < ? OR (sizeBytes = ? AND id < ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes DESC, id DESC"
		)
		MusicSortOption.TRACK_NUMBER_ASC -> Predicate(
			"(trackNumber < ?) OR (trackNumber = ? AND discNumber < ?) OR (trackNumber = ? AND discNumber = ? AND id < ?)",
			listOf(keys.trackNumber, keys.trackNumber, keys.discNumber, keys.trackNumber, keys.discNumber, keys.id),
			"trackNumber DESC, discNumber DESC, id DESC"
		)
		MusicSortOption.TRACK_NUMBER_DESC -> Predicate(
			"(trackNumber > ?) OR (trackNumber = ? AND discNumber > ?) OR (trackNumber = ? AND discNumber = ? AND id > ?)",
			listOf(keys.trackNumber, keys.trackNumber, keys.discNumber, keys.trackNumber, keys.discNumber, keys.id),
			"trackNumber ASC, discNumber ASC, id ASC"
		)
	}
}
