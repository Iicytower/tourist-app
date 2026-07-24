package com.iicytower.wanderlist.feature.mylist

import androidx.compose.ui.text.font.FontWeight
import com.iicytower.wanderlist.feature.mylist.ui.MdBlock
import com.iicytower.wanderlist.feature.mylist.ui.parseInlineMarkdown
import com.iicytower.wanderlist.feature.mylist.ui.parseMarkdownBlocks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun `plain text is a single paragraph`() {
        val blocks = parseMarkdownBlocks("otwarte tylko do 17:00")
        assertEquals(listOf(MdBlock.Paragraph("otwarte tylko do 17:00")), blocks)
    }

    @Test
    fun `bullets and headings are recognized`() {
        val blocks = parseMarkdownBlocks("## Dzień 1\n- parasol\n- bilety\n1. rano\nzwykły tekst")
        assertEquals(
            listOf(
                MdBlock.Heading(2, "Dzień 1"),
                MdBlock.Bullet("parasol"),
                MdBlock.Bullet("bilety"),
                MdBlock.Numbered("1.", "rano"),
                MdBlock.Paragraph("zwykły tekst")
            ),
            blocks
        )
    }

    @Test
    fun `bold renders without asterisks and with bold span`() {
        val result = parseInlineMarkdown("to jest **ważne** info")
        assertEquals("to jest ważne info", result.text)
        val boldSpans = result.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)
        assertEquals("ważne", result.text.substring(boldSpans[0].start, boldSpans[0].end))
    }

    @Test
    fun `unclosed markers are kept literally`() {
        val result = parseInlineMarkdown("cena od 20** zl")
        assertEquals("cena od 20** zl", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }
}
