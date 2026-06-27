package com.example.ui

internal class StreamingUpdateGate(
    private val minIntervalMs: Long = 80,
    private val minCharsBetweenUpdates: Int = 240,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var lastPublishedAtMs: Long? = null
    private var lastPublishedLength: Int = 0

    fun shouldPublish(currentLength: Int, force: Boolean = false): Boolean {
        if (currentLength <= 0) return false
        if (force) return markPublishedIfChanged(currentLength)

        val now = clock()
        val previousPublishAt = lastPublishedAtMs
        val firstPublish = previousPublishAt == null
        val intervalElapsed = previousPublishAt != null && now - previousPublishAt >= minIntervalMs
        val enoughNewText = currentLength - lastPublishedLength >= minCharsBetweenUpdates

        return if (firstPublish || intervalElapsed || enoughNewText) {
            markPublished(currentLength, now)
        } else {
            false
        }
    }

    private fun markPublishedIfChanged(currentLength: Int): Boolean {
        if (currentLength == lastPublishedLength) return false
        return markPublished(currentLength, clock())
    }

    private fun markPublished(currentLength: Int, now: Long): Boolean {
        lastPublishedAtMs = now
        lastPublishedLength = currentLength
        return true
    }
}
