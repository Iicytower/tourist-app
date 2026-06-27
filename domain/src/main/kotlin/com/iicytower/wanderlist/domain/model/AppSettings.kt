package com.iicytower.wanderlist.domain.model

import com.iicytower.wanderlist.core.model.AttractionCategory

data class AppSettings(
    val openRouterApiKey: String,
    val tavilyApiKey: String,
    val aiModel: String,
    val defaultRadiusKm: Int,
    val descriptionLanguage: String,
    val userInterests: Set<AttractionCategory>,
    val systemPromptDescription: String,
    val systemPromptAssistant: String,
    val tavilyUsageCount: Int,
    val tavilyUsageMonth: String
)
