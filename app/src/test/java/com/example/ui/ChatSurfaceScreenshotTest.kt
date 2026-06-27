package com.example.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.example.data.ChatMessage
import com.example.data.LlmConfiguration
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ChatSurfaceScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun longCompletedResponse() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ChatInterface(
                    activeConfig = sampleConfig,
                    chatHistory = listOf(
                        ChatMessage(
                            id = 1,
                            sessionId = 1,
                            role = "user",
                            content = "Summarize the implementation plan."
                        ),
                        ChatMessage(
                            id = 2,
                            sessionId = 1,
                            role = "assistant",
                            content = longAssistantResponse
                        )
                    ),
                    isGenerating = false,
                    isWaitingForFirstChunk = false,
                    onSendMessage = { _, _, _, _ -> },
                    onRetryLastMessage = {},
                    onMessageCopied = {},
                    onStopGeneration = {}
                )
            }
        }

        assertCoreChatNodesExist()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/chat-long-completed.png"
        )
    }

    @Test
    fun streamingResponse() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ChatInterface(
                    activeConfig = sampleConfig,
                    chatHistory = listOf(
                        ChatMessage(
                            id = 1,
                            sessionId = 1,
                            role = "user",
                            content = "Write a careful release note."
                        ),
                        ChatMessage(
                            sessionId = 1,
                            role = "assistant",
                            content = streamingAssistantResponse
                        )
                    ),
                    isGenerating = true,
                    isWaitingForFirstChunk = false,
                    onSendMessage = { _, _, _, _ -> },
                    onRetryLastMessage = {},
                    onMessageCopied = {},
                    onStopGeneration = {}
                )
            }
        }

        assertCoreChatNodesExist()
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/chat-streaming.png"
        )
    }

    private fun assertCoreChatNodesExist() {
        composeTestRule.onNodeWithTag("chat_surface").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("message_scroller").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("chat_composer").fetchSemanticsNode()
        composeTestRule.onNodeWithTag("chat_input").fetchSemanticsNode()
    }

    private companion object {
        val sampleConfig = LlmConfiguration(
            name = "Local route",
            baseUrl = "https://example.com/v1",
            apiKey = "test",
            modelName = "deepseek/deepseek-v4-flash",
            apiType = "OPENAI",
            stream = true
        )

        val longAssistantResponse = """
            ## Implementation summary

            The chat surface needs to stay stable across long model output, streaming updates,
            and repeated message turns. The core rule is that transport events should not map
            one-to-one to expensive UI work.

            - Stream updates are coalesced before they reach Compose state.
            - Markdown parsing is delayed to the frame boundary while the message is streaming.
            - Scroll following is also scheduled at the frame boundary.
            - Completed messages render full markdown immediately.

            ```kotlin
            val gate = StreamingUpdateGate()
            if (gate.shouldPublish(buffer.length)) {
                publish(buffer.toString())
            }
            ```

            This keeps the interface responsive while preserving the full final response.
        """.trimIndent()

        val streamingAssistantResponse = """
            Drafting the release note now.

            The new chat pipeline batches rapid response chunks, keeps markdown work off the
            hottest part of the stream, and only exposes final message actions after generation
            finishes.
        """.trimIndent()
    }
}
