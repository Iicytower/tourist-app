package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterChoice
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterDelta
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterFunctionCall
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterStreamChunk
import com.iicytower.wanderlist.data.remote.openrouter.dto.OpenRouterToolCall
import com.iicytower.wanderlist.data.remote.openrouter.mapper.toLlmEvent
import com.iicytower.wanderlist.data.remote.openrouter.mapper.toOpenRouterMessages
import com.iicytower.wanderlist.domain.model.ChatMessage
import com.iicytower.wanderlist.domain.model.LlmEvent
import org.junit.Assert.*
import org.junit.Test

class LlmMapperTest {

    @Test
    fun userMessage_mapsToUserRole() {
        val messages = listOf(ChatMessage.User("Cześć"))
        val result = messages.toOpenRouterMessages("Jesteś asystentem")
        assertEquals("system", result[0].role)
        assertEquals("user", result[1].role)
        assertEquals("Cześć", result[1].content)
    }

    @Test
    fun assistantMessage_mapsToAssistantRole() {
        val messages = listOf(ChatMessage.Assistant("Oto odpowiedź"))
        val result = messages.toOpenRouterMessages("prompt")
        assertEquals("assistant", result[1].role)
    }

    @Test
    fun toolResult_mapsToToolRole() {
        val messages = listOf(ChatMessage.ToolResult("call_123", "wynik narzędzia"))
        val result = messages.toOpenRouterMessages("prompt")
        assertEquals("tool", result[1].role)
        assertEquals("call_123", result[1].toolCallId)
    }

    @Test
    fun textChunk_fromDeltaContent() {
        val chunk = OpenRouterStreamChunk(
            choices = listOf(OpenRouterChoice(delta = OpenRouterDelta(content = "Hej")))
        )
        val event = chunk.toLlmEvent()
        assertTrue(event is LlmEvent.TextChunk)
        assertEquals("Hej", (event as LlmEvent.TextChunk).text)
    }

    @Test
    fun toolCall_fromDeltaToolCalls() {
        val chunk = OpenRouterStreamChunk(
            choices = listOf(OpenRouterChoice(delta = OpenRouterDelta(
                toolCalls = listOf(OpenRouterToolCall(
                    id = "call_1",
                    function = OpenRouterFunctionCall(name = "web_search", arguments = """{"query":"Wawel"}""")
                ))
            )))
        )
        val event = chunk.toLlmEvent()
        assertTrue(event is LlmEvent.ToolCall)
        val tc = event as LlmEvent.ToolCall
        assertEquals("web_search", tc.name)
        assertEquals("call_1", tc.id)
        assertEquals("Wawel", tc.arguments["query"])
    }

    @Test
    fun emptyDelta_returnsNull() {
        val chunk = OpenRouterStreamChunk(
            choices = listOf(OpenRouterChoice(delta = OpenRouterDelta()))
        )
        assertNull(chunk.toLlmEvent())
    }
}
