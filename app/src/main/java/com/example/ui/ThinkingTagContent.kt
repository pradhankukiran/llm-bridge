package com.example.ui

const val REASONING_MODE_SHOW_THINKING = "show_thinking_tags"

data class ThinkingTagContent(
    val answer: String,
    val thoughts: List<String>,
    val hasUnclosedThought: Boolean
) {
    val hasThoughts: Boolean = thoughts.any { it.isNotBlank() }
}

fun parseThinkingTagContent(content: String): ThinkingTagContent {
    val answer = StringBuilder()
    val thoughts = mutableListOf<String>()
    var hasUnclosedThought = false
    var cursor = 0

    while (cursor < content.length) {
        val openRange = content.indexOfTag("<think>", cursor)
        if (openRange == null) {
            answer.append(content.substring(cursor))
            break
        }

        answer.append(content.substring(cursor, openRange.first))
        val thoughtStart = openRange.last + 1
        val closeRange = content.indexOfTag("</think>", thoughtStart)

        if (closeRange == null) {
            thoughts += content.substring(thoughtStart).trim()
            hasUnclosedThought = true
            cursor = content.length
        } else {
            thoughts += content.substring(thoughtStart, closeRange.first).trim()
            cursor = closeRange.last + 1
        }
    }

    return ThinkingTagContent(
        answer = answer.toString().trim(),
        thoughts = thoughts.filter { it.isNotBlank() },
        hasUnclosedThought = hasUnclosedThought
    )
}

private fun String.indexOfTag(tag: String, startIndex: Int): IntRange? {
    val index = indexOf(tag, startIndex = startIndex, ignoreCase = true)
    return if (index >= 0) index until index + tag.length else null
}
