package com.iicytower.wanderlist.domain.repository

import com.iicytower.wanderlist.domain.model.WikipediaResult

interface WikipediaService {
    suspend fun getArticle(query: String): Result<WikipediaResult?>
}
