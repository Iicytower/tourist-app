package com.iicytower.wanderlist.feature.mylist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

// Prosty renderer podzbioru Markdown używanego przez LLM w notatkach planu:
// nagłówki (#/##/###), listy "- "/"* "/"1. ", **pogrubienie**, *kursywa*, `kod`.
// Zwykły tekst bez składni renderuje się identycznie jak dotychczas.

internal sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Bullet(val text: String) : MdBlock
    data class Numbered(val marker: String, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data object Blank : MdBlock
}

private val NUMBERED_REGEX = Regex("""^(\d+)\.\s+(.*)""")

internal fun parseMarkdownBlocks(markdown: String): List<MdBlock> =
    markdown.lines().map { line ->
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> MdBlock.Blank
            trimmed.startsWith("### ") -> MdBlock.Heading(3, trimmed.removePrefix("### "))
            trimmed.startsWith("## ") -> MdBlock.Heading(2, trimmed.removePrefix("## "))
            trimmed.startsWith("# ") -> MdBlock.Heading(1, trimmed.removePrefix("# "))
            trimmed.startsWith("- ") -> MdBlock.Bullet(trimmed.removePrefix("- "))
            trimmed.startsWith("* ") -> MdBlock.Bullet(trimmed.removePrefix("* "))
            else -> NUMBERED_REGEX.matchEntire(trimmed)
                ?.let { MdBlock.Numbered("${it.groupValues[1]}.", it.groupValues[2]) }
                ?: MdBlock.Paragraph(trimmed)
        }
    }

internal fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(parseInlineMarkdown(text.substring(i + 2, end))) }
                    i = end + 2
                } else {
                    append(text[i]); i++
                }
            }
            text[i] == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(parseInlineMarkdown(text.substring(i + 1, end))) }
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            text[i] == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append(text[i]); i++
                }
            }
            else -> {
                append(text[i]); i++
            }
        }
    }
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = Color.Unspecified
) {
    Column(modifier = modifier) {
        parseMarkdownBlocks(markdown).forEach { block ->
            when (block) {
                is MdBlock.Heading -> Text(
                    parseInlineMarkdown(block.text),
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.titleMedium
                        2 -> MaterialTheme.typography.titleSmall
                        else -> MaterialTheme.typography.labelLarge
                    },
                    color = color
                )
                is MdBlock.Bullet -> Row {
                    Text("•", style = style, color = color, modifier = Modifier.padding(end = 6.dp))
                    Text(parseInlineMarkdown(block.text), style = style, color = color)
                }
                is MdBlock.Numbered -> Row {
                    Text(block.marker, style = style, color = color, modifier = Modifier.padding(end = 6.dp))
                    Text(parseInlineMarkdown(block.text), style = style, color = color)
                }
                is MdBlock.Paragraph -> Text(parseInlineMarkdown(block.text), style = style, color = color)
                MdBlock.Blank -> Spacer(Modifier.height(4.dp))
            }
        }
    }
}
