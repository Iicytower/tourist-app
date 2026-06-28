package com.iicytower.wanderlist.core

import com.iicytower.wanderlist.core.constant.toKindsParam
import com.iicytower.wanderlist.core.model.AttractionCategory
import org.junit.Assert.*
import org.junit.Test

class CategoryKindsMappingTest {

    @Test
    fun `empty set returns all kinds`() {
        val result = emptySet<AttractionCategory>().toKindsParam()
        assertTrue(result.contains("castles"))
        assertTrue(result.contains("museums"))
        assertTrue(result.contains("view_points"))
        assertTrue(result.contains("caves_and_tunnels"))
    }

    @Test
    fun `single category returns only its kinds`() {
        val result = setOf(AttractionCategory.CASTLES_AND_FORTIFICATIONS).toKindsParam()
        val kinds = result.split(",")
        assertTrue(kinds.contains("castles"))
        assertTrue(kinds.contains("fortifications"))
        assertTrue(kinds.contains("palaces"))
        assertFalse(kinds.contains("museums"))
        assertFalse(kinds.contains("churches"))
    }

    @Test
    fun `multiple categories return union of kinds without duplicates`() {
        val result = setOf(
            AttractionCategory.CASTLES_AND_FORTIFICATIONS,
            AttractionCategory.MILITARY
        ).toKindsParam()
        val kinds = result.split(",")
        // fortifications appears in both — should be present exactly once
        assertEquals(1, kinds.count { it == "fortifications" })
        assertTrue(kinds.contains("castles"))
        assertTrue(kinds.contains("battlefields"))
    }

    @Test
    fun `all categories covered`() {
        val result = AttractionCategory.entries.toSet().toKindsParam()
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("castles"))
        assertTrue(result.contains("view_points"))
        assertTrue(result.contains("caves_and_tunnels"))
    }
}
