package com.example.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkingTagContentTest {

    @Test
    fun extractsThinkingTagsAndReturnsVisibleAnswer() {
        val parsed = parseThinkingTagContent(
            """
            <think>
            Check route state.
            </think>

            The route is ready.
            """.trimIndent()
        )

        assertEquals("The route is ready.", parsed.answer)
        assertEquals(listOf("Check route state."), parsed.thoughts)
        assertFalse(parsed.hasUnclosedThought)
    }

    @Test
    fun preservesAnswerAroundMultipleThinkingBlocks() {
        val parsed = parseThinkingTagContent(
            "First. <think>one</think> Second. <think>two</think> Third."
        )

        assertEquals("First.  Second.  Third.", parsed.answer)
        assertEquals(listOf("one", "two"), parsed.thoughts)
    }

    @Test
    fun treatsUnclosedThinkingTagAsStreamingThought() {
        val parsed = parseThinkingTagContent("<think>Still working")

        assertEquals("", parsed.answer)
        assertEquals(listOf("Still working"), parsed.thoughts)
        assertTrue(parsed.hasUnclosedThought)
    }

    @Test
    fun ignoresCaseForThinkingTags() {
        val parsed = parseThinkingTagContent("<THINK>Reason</THINK>Answer")

        assertEquals("Answer", parsed.answer)
        assertEquals(listOf("Reason"), parsed.thoughts)
    }
}
