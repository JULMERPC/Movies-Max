package com.example.videomax.data.local.db

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.videomax.data.local.db.entity.VideoEntity
import com.example.videomax.domain.model.SortOption

/**
 * Builds keyset SQL for lazy playback-queue windows.
 * Primary order matches Library paging; [id] is a stable tie-breaker only.
 */
internal object QueueWindowSql {

	data class SortKeys(
		val dateAdded: Long,
		val displayName: String,
		val durationMs: Long,
		val sizeBytes: Long,
		val id: Long
	)

	fun fromEntity(entity: VideoEntity) = SortKeys(
		dateAdded = entity.dateAdded,
		displayName = entity.displayName,
		durationMs = entity.durationMs,
		sizeBytes = entity.sizeBytes,
		id = entity.id
	)

	fun neighborsAfter(
		sortOption: SortOption,
		folder: String?,
		query: String,
		keys: SortKeys,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val (cmp, cmpArgs, order) = afterPredicate(sortOption, keys)
		val sql = "SELECT id FROM videos WHERE $filter AND ($cmp) ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + cmpArgs + limit).toTypedArray())
	}

	fun neighborsBefore(
		sortOption: SortOption,
		folder: String?,
		query: String,
		keys: SortKeys,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val (cmp, cmpArgs, order) = beforePredicate(sortOption, keys)
		val sql = "SELECT id FROM videos WHERE $filter AND ($cmp) ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + cmpArgs + limit).toTypedArray())
	}

	fun firstPage(
		sortOption: SortOption,
		folder: String?,
		query: String,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val order = primaryOrder(sortOption)
		val sql = "SELECT id FROM videos WHERE $filter ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + limit).toTypedArray())
	}

	/** Last [limit] IDs in library order (fetched reversed, reverse in Kotlin). */
	fun lastPage(
		sortOption: SortOption,
		folder: String?,
		query: String,
		limit: Int
	): SupportSQLiteQuery {
		val (filter, filterArgs) = baseFilter(folder, query)
		val order = reverseOrder(sortOption)
		val sql = "SELECT id FROM videos WHERE $filter ORDER BY $order LIMIT ?"
		return SimpleSQLiteQuery(sql, (filterArgs + limit).toTypedArray())
	}

	private fun baseFilter(folder: String?, query: String): Pair<String, List<Any>> {
		return if (folder != null) {
			"folderName = ?" to listOf(folder)
		} else {
			val q = query.trim()
			if (q.isEmpty()) {
				"1 = 1" to emptyList()
			} else {
				"(displayName LIKE '%' || ? || '%' OR folderName LIKE '%' || ? || '%')" to listOf(q, q)
			}
		}
	}

	private fun primaryOrder(sort: SortOption): String = when (sort) {
		SortOption.DATE_DESC -> "dateAdded DESC, id DESC"
		SortOption.DATE_ASC -> "dateAdded ASC, id ASC"
		SortOption.NAME_ASC -> "displayName COLLATE NOCASE ASC, id ASC"
		SortOption.NAME_DESC -> "displayName COLLATE NOCASE DESC, id DESC"
		SortOption.DURATION_DESC -> "durationMs DESC, id DESC"
		SortOption.DURATION_ASC -> "durationMs ASC, id ASC"
		SortOption.SIZE_DESC -> "sizeBytes DESC, id DESC"
		SortOption.SIZE_ASC -> "sizeBytes ASC, id ASC"
	}

	private fun reverseOrder(sort: SortOption): String = when (sort) {
		SortOption.DATE_DESC -> "dateAdded ASC, id ASC"
		SortOption.DATE_ASC -> "dateAdded DESC, id DESC"
		SortOption.NAME_ASC -> "displayName COLLATE NOCASE DESC, id DESC"
		SortOption.NAME_DESC -> "displayName COLLATE NOCASE ASC, id ASC"
		SortOption.DURATION_DESC -> "durationMs ASC, id ASC"
		SortOption.DURATION_ASC -> "durationMs DESC, id DESC"
		SortOption.SIZE_DESC -> "sizeBytes ASC, id ASC"
		SortOption.SIZE_ASC -> "sizeBytes DESC, id DESC"
	}

	private data class Predicate(val sql: String, val args: List<Any>, val order: String)

	/** Rows that appear after [keys] in library order (toward "next"). */
	private fun afterPredicate(sort: SortOption, keys: SortKeys): Predicate = when (sort) {
		SortOption.DATE_DESC -> Predicate(
			"dateAdded < ? OR (dateAdded = ? AND id < ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded DESC, id DESC"
		)
		SortOption.DATE_ASC -> Predicate(
			"dateAdded > ? OR (dateAdded = ? AND id > ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded ASC, id ASC"
		)
		SortOption.NAME_ASC -> Predicate(
			"(displayName COLLATE NOCASE > ?) OR (displayName COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.displayName, keys.displayName, keys.id),
			"displayName COLLATE NOCASE ASC, id ASC"
		)
		SortOption.NAME_DESC -> Predicate(
			"(displayName COLLATE NOCASE < ?) OR (displayName COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.displayName, keys.displayName, keys.id),
			"displayName COLLATE NOCASE DESC, id DESC"
		)
		SortOption.DURATION_DESC -> Predicate(
			"durationMs < ? OR (durationMs = ? AND id < ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs DESC, id DESC"
		)
		SortOption.DURATION_ASC -> Predicate(
			"durationMs > ? OR (durationMs = ? AND id > ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs ASC, id ASC"
		)
		SortOption.SIZE_DESC -> Predicate(
			"sizeBytes < ? OR (sizeBytes = ? AND id < ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes DESC, id DESC"
		)
		SortOption.SIZE_ASC -> Predicate(
			"sizeBytes > ? OR (sizeBytes = ? AND id > ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes ASC, id ASC"
		)
	}

	/**
	 * Rows that appear before [keys] (toward "previous").
	 * Ordered reversed so LIMIT returns the closest neighbors; reverse in Kotlin.
	 */
	private fun beforePredicate(sort: SortOption, keys: SortKeys): Predicate = when (sort) {
		SortOption.DATE_DESC -> Predicate(
			"dateAdded > ? OR (dateAdded = ? AND id > ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded ASC, id ASC"
		)
		SortOption.DATE_ASC -> Predicate(
			"dateAdded < ? OR (dateAdded = ? AND id < ?)",
			listOf(keys.dateAdded, keys.dateAdded, keys.id),
			"dateAdded DESC, id DESC"
		)
		SortOption.NAME_ASC -> Predicate(
			"(displayName COLLATE NOCASE < ?) OR (displayName COLLATE NOCASE = ? AND id < ?)",
			listOf(keys.displayName, keys.displayName, keys.id),
			"displayName COLLATE NOCASE DESC, id DESC"
		)
		SortOption.NAME_DESC -> Predicate(
			"(displayName COLLATE NOCASE > ?) OR (displayName COLLATE NOCASE = ? AND id > ?)",
			listOf(keys.displayName, keys.displayName, keys.id),
			"displayName COLLATE NOCASE ASC, id ASC"
		)
		SortOption.DURATION_DESC -> Predicate(
			"durationMs > ? OR (durationMs = ? AND id > ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs ASC, id ASC"
		)
		SortOption.DURATION_ASC -> Predicate(
			"durationMs < ? OR (durationMs = ? AND id < ?)",
			listOf(keys.durationMs, keys.durationMs, keys.id),
			"durationMs DESC, id DESC"
		)
		SortOption.SIZE_DESC -> Predicate(
			"sizeBytes > ? OR (sizeBytes = ? AND id > ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes ASC, id ASC"
		)
		SortOption.SIZE_ASC -> Predicate(
			"sizeBytes < ? OR (sizeBytes = ? AND id < ?)",
			listOf(keys.sizeBytes, keys.sizeBytes, keys.id),
			"sizeBytes DESC, id DESC"
		)
	}
}
