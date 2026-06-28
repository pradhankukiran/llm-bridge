package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MessageBubbleLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun shortPlainTextMessageDoesNotFillTheAvailableBubbleWidth() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.width(400.dp)) {
                    MessageBubble(
                        message = ChatMessage(
                            sessionId = 1,
                            role = "user",
                            content = "hi"
                        )
                    )
                }
            }
        }

        val bubbleBounds = composeTestRule
            .onNodeWithTag("message_bubble")
            .getUnclippedBoundsInRoot()
        val bubbleWidth = bubbleBounds.right - bubbleBounds.left

        assertTrue(
            "Expected short message bubble to wrap content, but width was $bubbleWidth",
            bubbleWidth < 120.dp
        )
    }

    @Test
    fun shortAssistantMessageDoesNotFillTheAvailableBubbleWidth() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.width(400.dp)) {
                    MessageBubble(
                        message = ChatMessage(
                            sessionId = 1,
                            role = "assistant",
                            content = "hi"
                        )
                    )
                }
            }
        }

        val bubbleBounds = composeTestRule
            .onNodeWithTag("message_bubble")
            .getUnclippedBoundsInRoot()
        val bubbleWidth = bubbleBounds.right - bubbleBounds.left

        assertTrue(
            "Expected short assistant bubble to wrap content, but width was $bubbleWidth",
            bubbleWidth < 140.dp
        )
    }

    @Test
    fun codeBlockCanStillUseWideBubbleLayout() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Box(modifier = Modifier.width(400.dp)) {
                    MessageBubble(
                        message = ChatMessage(
                            sessionId = 1,
                            role = "assistant",
                            content = """
                                ```kotlin
                                val result = client.generateStreamingResponse(input)
                                ```
                            """.trimIndent()
                        )
                    )
                }
            }
        }

        val bubbleBounds = composeTestRule
            .onNodeWithTag("message_bubble")
            .getUnclippedBoundsInRoot()
        val bubbleWidth = bubbleBounds.right - bubbleBounds.left

        assertTrue(
            "Expected code block bubble to retain a wide layout, but width was $bubbleWidth",
            bubbleWidth > 300.dp
        )
    }
}
