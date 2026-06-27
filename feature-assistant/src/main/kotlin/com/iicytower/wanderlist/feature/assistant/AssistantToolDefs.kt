package com.iicytower.wanderlist.feature.assistant

import com.iicytower.wanderlist.domain.model.ToolDefinition

internal object AssistantToolDefs {
    val SEARCH_ATTRACTIONS = ToolDefinition(
        name = "search_attractions",
        description = "Wyszukuje atrakcje turystyczne w poblizu wskazanego punktu.",
        parameters = mapOf(
            "latitude" to mapOf("type" to "number", "description" to "Szerokosc geograficzna"),
            "longitude" to mapOf("type" to "number", "description" to "Dlugosc geograficzna"),
            "radius_km" to mapOf("type" to "integer", "description" to "Promien wyszukiwania w km"),
            "categories" to mapOf("type" to "array", "items" to mapOf("type" to "string"), "nullable" to "true")
        )
    )

    val WEB_SEARCH = ToolDefinition(
        name = "web_search",
        description = "Wyszukuje informacje w internecie.",
        parameters = mapOf("query" to mapOf("type" to "string", "description" to "Zapytanie wyszukiwania"))
    )

    val GET_MY_LIST = ToolDefinition(
        name = "get_my_list",
        description = "Zwraca miejsca zapisane przez uzytkownika na Mojej Liscie.",
        parameters = emptyMap()
    )

    val ALL = listOf(SEARCH_ATTRACTIONS, WEB_SEARCH, GET_MY_LIST)
}
