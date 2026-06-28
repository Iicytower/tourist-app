package com.iicytower.wanderlist.domain.repository

interface WebSearchService {
    suspend fun search(query: String): Result<String>
}
