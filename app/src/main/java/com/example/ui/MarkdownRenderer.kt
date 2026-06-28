package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: AnnotatedString) : MarkdownBlock()
    data class Paragraph(val text: AnnotatedString) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class BulletList(val items: List<AnnotatedString>) : MarkdownBlock()
    data class TableBlock(
        val headers: List<AnnotatedString>,
        val rows: List<List<AnnotatedString>>
    ) : MarkdownBlock()
}

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    isStreaming: Boolean = false
) {
    val blocks by rememberMarkdownBlocks(content = content, isStreaming = isStreaming)
    val clipboardManager = LocalClipboardManager.current
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val fontSize = when (block.level) {
                        1 -> 20.sp
                        2 -> 18.sp
                        else -> 16.sp
                    }
                    val topPadding = if (block.level == 1) 12.dp else 8.dp
                    Text(
                        text = block.text,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = topPadding, bottom = 4.dp)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = block.text,
                        fontSize = 14.sp,
                        color = color,
                        lineHeight = 20.sp
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCompos(
                        language = block.language,
                        code = block.code,
                        onCopyClick = {
                            clipboardManager.setText(AnnotatedString(block.code))
                        }
                    )
                }
                is MarkdownBlock.BulletList -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        block.items.forEach { bullet ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "•",
                                    fontSize = 14.sp,
                                    color = color,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = bullet,
                                    fontSize = 14.sp,
                                    color = color,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                is MarkdownBlock.TableBlock -> {
                    TableBlockCompos(block = block)
                }
            }
        }
    }
}

@Composable
private fun rememberMarkdownBlocks(
    content: String,
    isStreaming: Boolean
): State<List<MarkdownBlock>> {
    if (!isStreaming) {
        return remember(content) { mutableStateOf(parseMarkdown(content)) }
    }

    val initialBlocks = remember { parseMarkdown(content) }
    return produceState(initialValue = initialBlocks, content, isStreaming) {
        withFrameNanos { }
        value = parseMarkdown(content)
    }
}

@Composable
fun CodeBlockCompos(
    language: String,
    code: String,
    onCopyClick: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }
    
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(2000)
            copied = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E24))
            .border(1.dp, Color(0xFF33333C), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E38))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.uppercase().ifEmpty { "CODE" },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA0A0AB)
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (copied) Color(0xFF2E7D32) else Color(0xFF3E3E4C))
                    .clickable {
                        onCopyClick()
                        copied = true
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (copied) "COPIED" else "COPY",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Color(0xFFE2E2E9),
                lineHeight = 16.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
fun TableBlockCompos(block: MarkdownBlock.TableBlock) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                block.headers.forEach { header ->
                    Text(
                        text = header,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(140.dp)
                    )
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            
            block.rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier
                        .background(if (index % 2 == 1) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.width(140.dp)
                        )
                    }
                }
                if (index < block.rows.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var index = 0
    
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length
            if (level in 1..6 && trimmed.getOrNull(level) == ' ') {
                val headerText = trimmed.substring(level + 1).trim()
                blocks.add(MarkdownBlock.Header(level = level, text = parseInlineStyles(headerText)))
                index++
                continue
            }
        }
        
        if (trimmed.startsWith("```")) {
            val lang = trimmed.substringAfter("```").trim()
            val codeLines = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trim().startsWith("```")) {
                codeLines.add(lines[index])
                index++
            }
            if (index < lines.size) {
                index++
            }
            blocks.add(MarkdownBlock.CodeBlock(language = lang, code = codeLines.joinToString("\n")))
            continue
        }
        
        if (trimmed.startsWith("|") && trimmed.endsWith("|") && index + 1 < lines.size) {
            val nextLine = lines[index + 1].trim()
            if (nextLine.startsWith("|") && nextLine.endsWith("|") && nextLine.contains("-")) {
                val rawHeaders = line.split("|").map { it.trim() }
                val headers = if (rawHeaders.size > 2) rawHeaders.subList(1, rawHeaders.size - 1) else rawHeaders
                
                val rows = mutableListOf<List<String>>()
                index += 2
                while (index < lines.size && lines[index].trim().startsWith("|") && lines[index].trim().endsWith("|")) {
                    val rawRow = lines[index].split("|").map { it.trim() }
                    val rowCells = if (rawRow.size > 2) rawRow.subList(1, rawRow.size - 1) else rawRow
                    rows.add(rowCells)
                    index++
                }
                
                blocks.add(
                    MarkdownBlock.TableBlock(
                        headers = headers.map { parseInlineStyles(it) },
                        rows = rows.map { r -> r.map { parseInlineStyles(it) } }
                    )
                )
                continue
            }
        }
        
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || (trimmed.firstOrNull()?.isDigit() == true && trimmed.contains(". "))) {
            val listItems = mutableListOf<AnnotatedString>()
            while (index < lines.size) {
                val currentLine = lines[index]
                val currentTrimmed = currentLine.trim()
                if (currentTrimmed.startsWith("- ") || currentTrimmed.startsWith("* ")) {
                    val content = currentTrimmed.substring(2)
                    listItems.add(parseInlineStyles(content))
                    index++
                } else if (currentTrimmed.firstOrNull()?.isDigit() == true && currentTrimmed.contains(". ")) {
                    val dotIdx = currentTrimmed.indexOf(". ")
                    val content = currentTrimmed.substring(dotIdx + 2)
                    listItems.add(parseInlineStyles(content))
                    index++
                } else {
                    break
                }
            }
            if (listItems.isNotEmpty()) {
                blocks.add(MarkdownBlock.BulletList(items = listItems))
            }
            continue
        }
        
        if (trimmed.isEmpty()) {
            index++
            continue
        }
        
        val paragraphLines = mutableListOf<String>()
        while (index < lines.size) {
            val currentLine = lines[index]
            val currentTrimmed = currentLine.trim()
            if (currentTrimmed.startsWith("```") || currentTrimmed.startsWith("- ") || currentTrimmed.startsWith("* ") || 
                currentTrimmed.startsWith("#") || 
                (currentTrimmed.firstOrNull()?.isDigit() == true && currentTrimmed.contains(". ")) || 
                (currentTrimmed.startsWith("|") && index + 1 < lines.size && lines[index + 1].trim().startsWith("|") && lines[index + 1].trim().contains("-"))
            ) {
                break
            }
            if (currentTrimmed.isEmpty()) {
                index++
                break
            }
            paragraphLines.add(currentLine)
            index++
        }
        
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(text = parseInlineStyles(paragraphLines.joinToString("\n"))))
        }
    }
    
    return blocks
}

fun parseInlineStyles(text: String): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val endIdx = text.indexOf("**", i + 2)
                if (endIdx != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(text.substring(i + 2, endIdx))
                    }
                    i = endIdx + 2
                    continue
                }
            }
            
            if (text.startsWith("`", i)) {
                val endIdx = text.indexOf("`", i + 1)
                if (endIdx != -1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = Color(0xFFF2F1F5),
                            color = Color(0xFFE91E63),
                            fontSize = 11.5.sp
                        )
                    ) {
                        append(text.substring(i + 1, endIdx))
                    }
                    i = endIdx + 1
                    continue
                }
            }
            
            if (text.startsWith("*", i) && !text.startsWith("**", i)) {
                val endIdx = text.indexOf("*", i + 1)
                if (endIdx != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, endIdx))
                    }
                    i = endIdx + 1
                    continue
                }
            }
            if (text.startsWith("_", i)) {
                val endIdx = text.indexOf("_", i + 1)
                if (endIdx != -1) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(text.substring(i + 1, endIdx))
                    }
                    i = endIdx + 1
                    continue
                }
            }
            
            append(text[i])
            i++
        }
    }
}

fun estimateTokenCount(text: String): Int {
    if (text.isBlank()) return 0
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val wordCount = words.size
    val charCount = text.length
    val estFromChars = Math.round(charCount / 4.0).toInt()
    val estFromWords = Math.round(wordCount * 1.3).toInt()
    return maxOf(estFromChars, estFromWords, 1)
}
