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

    val GET_TRIP_LISTS = ToolDefinition(
        name = "get_trip_lists",
        description = "Zwraca wszystkie listy wycieczek uzytkownika z liczba atrakcji na kazdej.",
        parameters = emptyMap()
    )

    val GET_LIST_ATTRACTIONS = ToolDefinition(
        name = "get_list_attractions",
        description = "Zwraca atrakcje zapisane na wybranej liscie wycieczek.",
        parameters = mapOf(
            "list_id" to mapOf("type" to "integer", "description" to "ID listy wycieczek")
        )
    )

    val ADD_TO_LIST = ToolDefinition(
        name = "add_to_list",
        description = "Dodaje atrakcje do wybranej listy wycieczek.",
        parameters = mapOf(
            "xid" to mapOf("type" to "string", "description" to "Identyfikator atrakcji"),
            "list_id" to mapOf("type" to "integer", "description" to "ID listy wycieczek")
        )
    )

    val REMOVE_FROM_LIST = ToolDefinition(
        name = "remove_from_list",
        description = "Usuwa atrakcje z wybranej listy wycieczek.",
        parameters = mapOf(
            "xid" to mapOf("type" to "string", "description" to "Identyfikator atrakcji"),
            "list_id" to mapOf("type" to "integer", "description" to "ID listy wycieczek")
        )
    )

    val CREATE_LIST = ToolDefinition(
        name = "create_list",
        description = "Tworzy nowa liste wycieczek o podanej nazwie.",
        parameters = mapOf(
            "name" to mapOf("type" to "string", "description" to "Nazwa nowej listy")
        )
    )

    val ALL = listOf(SEARCH_ATTRACTIONS, WEB_SEARCH, GET_TRIP_LISTS, GET_LIST_ATTRACTIONS, ADD_TO_LIST, REMOVE_FROM_LIST, CREATE_LIST)
}
