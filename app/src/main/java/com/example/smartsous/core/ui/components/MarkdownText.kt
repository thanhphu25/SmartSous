package com.example.smartsous.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified
) {
    Text(
        text = text.parseMarkdown(),
        modifier = modifier,
        style = style,
        color = color
    )
}

// Extension function — dùng được ở bất kỳ đâu
fun String.parseMarkdown(): AnnotatedString = buildAnnotatedString {
    val lines = this@parseMarkdown.split("\n")

    lines.forEachIndexed { lineIndex, rawLine ->
        if (lineIndex > 0) append("\n")

        var line = rawLine

        // ── Xử lý heading ────────────────────────────────────
        when {
            line.startsWith("### ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)) {
                    append(line.removePrefix("### ").removeBold())
                }
                return@forEachIndexed
            }
            line.startsWith("## ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp)) {
                    append(line.removePrefix("## ").removeBold())
                }
                return@forEachIndexed
            }
            line.startsWith("# ") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp)) {
                    append(line.removePrefix("# ").removeBold())
                }
                return@forEachIndexed
            }
            // Bullet list
            line.startsWith("- ") || line.startsWith("• ") -> {
                append("• ")
                line = line.removePrefix("- ").removePrefix("• ")
            }
        }

        // ── Parse inline bold **text** ─────────────────────
        parseInlineStyles(line)
    }
}

// Parse **bold** và *italic* trong 1 dòng
private fun AnnotatedString.Builder.parseInlineStyles(text: String) {
    // Pattern: **bold** hoặc *italic*
    val pattern = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")
    var lastIndex = 0

    pattern.findAll(text).forEach { match ->
        // Text trước match
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }

        val isBold = match.value.startsWith("**")
        val content = if (isBold) match.groupValues[1] else match.groupValues[2]

        if (isBold) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(content)
            }
        } else {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(content)
            }
        }

        lastIndex = match.range.last + 1
    }

    // Text sau match cuối
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

// Xoá ** xung quanh nếu còn sót
private fun String.removeBold() = replace("**", "").replace("*", "")