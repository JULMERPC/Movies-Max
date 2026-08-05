package com.puma.videomax.data.local.db

/**
 * Builds SQLite FTS MATCH strings from free-form user input.
 * Uses prefix tokens (`term*`) so typing "ava" matches "Avatar.mp4",
 * similar to professional players, without leading-wildcard LIKE scans.
 */
object FtsQuery {

	/**
	 * @return FTS query, or null when [raw] should mean "no search filter".
	 */
	fun fromUserInput(raw: String): String? {
		val trimmed = raw.trim()
		if (trimmed.isEmpty()) return null

		val terms = trimmed
			.split(Regex("\\s+"))
			.mapNotNull { token ->
				// Strip FTS operator / quote characters that would break MATCH.
				val cleaned = token.replace(Regex("""["*():^]"""), "")
				if (cleaned.isEmpty()) null else cleaned
			}

		if (terms.isEmpty()) return null

		// Prefix match per term; multiple terms → AND (all must appear).
		return terms.joinToString(separator = " ") { term ->
			"\"$term\"*"
		}
	}
}
