package com.example.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamingUpdateGateTest {

    @Test
    fun publishesFirstNonEmptyChunkImmediately() {
        val gate = StreamingUpdateGate(clock = { 0L })

        assertTrue(gate.shouldPublish(currentLength = 1))
    }

    @Test
    fun suppressesTinyUpdatesInsideThrottleWindow() {
        var now = 0L
        val gate = StreamingUpdateGate(clock = { now })

        assertTrue(gate.shouldPublish(currentLength = 10))

        now = 20L

        assertFalse(gate.shouldPublish(currentLength = 20))
    }

    @Test
    fun publishesWhenEnoughTextAccumulatesInsideThrottleWindow() {
        var now = 0L
        val gate = StreamingUpdateGate(clock = { now })

        assertTrue(gate.shouldPublish(currentLength = 10))

        now = 20L

        assertTrue(gate.shouldPublish(currentLength = 250))
    }

    @Test
    fun publishesAfterThrottleWindowElapses() {
        var now = 0L
        val gate = StreamingUpdateGate(clock = { now })

        assertTrue(gate.shouldPublish(currentLength = 10))

        now = 80L

        assertTrue(gate.shouldPublish(currentLength = 20))
    }

    @Test
    fun forcePublishesChangedFinalContentOnlyOnce() {
        val gate = StreamingUpdateGate(clock = { 0L })

        assertTrue(gate.shouldPublish(currentLength = 10))
        assertTrue(gate.shouldPublish(currentLength = 20, force = true))
        assertFalse(gate.shouldPublish(currentLength = 20, force = true))
    }
}
