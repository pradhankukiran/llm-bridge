package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.example.data.LlmConfiguration
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatComposerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun imeSendActionSubmitsTheCurrentMessage() {
        val sentMessages = mutableListOf<String>()

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                ChatComposer(
                    activeConfig = sampleConfig,
                    isGenerating = false,
                    onSendMessage = { text, _, _, _ -> sentMessages += text },
                    onStopGeneration = {},
                    onHeightChanged = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("chat_input").performTextInput("hello")
        composeTestRule.onNodeWithTag("chat_input").performImeAction()

        assertEquals(listOf("hello"), sentMessages)
        composeTestRule.onNodeWithTag("chat_input").assertExists()
    }

    private companion object {
        val sampleConfig = LlmConfiguration(
            name = "Local route",
            baseUrl = "https://example.com/v1",
            apiKey = "test",
            modelName = "test-model",
            apiType = "OPENAI"
        )
    }
}
