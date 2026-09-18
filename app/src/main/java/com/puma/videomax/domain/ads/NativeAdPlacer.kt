package com.puma.videomax.domain.ads

/**
 * Maps flat list positions to [content item | native ad] slots.
 *
 * One native ad is inserted after every [CONTENT_INTERVAL] content items,
 * i.e. roughly 1 ad per 12 rows. Pure logic — covered by unit tests.
 *
 * Example with CONTENT_INTERVAL = 11 (positions are 0-based):
 * positions 0..10 → content 0..10, position 11 → ad, position 12 → content 11…
 */
object NativeAdPlacer {

    const val CONTENT_INTERVAL = 11

    fun totalWithAds(contentCount: Int): Int {
        if (contentCount <= 0) return 0
        return contentCount + contentCount / CONTENT_INTERVAL
    }

    fun isAdPosition(position: Int): Boolean =
        (position + 1) % (CONTENT_INTERVAL + 1) == 0

    /**
     * Content index for a non-ad [position]. Do NOT call with ad positions.
     */
    fun contentIndexFor(position: Int): Int =
        position - (position + 1) / (CONTENT_INTERVAL + 1)
}
