package com.iicytower.wanderlist.domain.model

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, Any>
)
