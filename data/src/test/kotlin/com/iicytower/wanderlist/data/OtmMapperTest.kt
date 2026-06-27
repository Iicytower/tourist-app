package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDto
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmPoint
import com.iicytower.wanderlist.data.remote.opentripmap.mapper.toDomain
import org.junit.Assert.*
import org.junit.Test

class OtmMapperTest {

    private fun dto(kinds: String, dist: Double = 1000.0) =
        OtmAttractionDto(xid = "xid1", name = "Test", dist = dist, kinds = kinds, point = OtmPoint(20.0, 50.0))

    @Test
    fun mapsKinds_castles_toCastlesCategory() {
        val result = dto("castles,fortifications").toDomain()
        assertEquals(AttractionCategory.CASTLES_AND_FORTIFICATIONS, result.category)
    }

    @Test
    fun mapsKinds_museums_toMuseumsCategory() {
        val result = dto("museums,art_galleries").toDomain()
        assertEquals(AttractionCategory.MUSEUMS_AND_GALLERIES, result.category)
    }

    @Test
    fun mapsKinds_churches_toChurchesCategory() {
        val result = dto("churches,cathedrals").toDomain()
        assertEquals(AttractionCategory.CHURCHES_AND_SACRED, result.category)
    }

    @Test
    fun distanceConvertedFromMetersToKm() {
        val result = dto("castles", dist = 2500.0).toDomain()
        assertEquals(2.5, result.distanceKm!!, 0.001)
    }

    @Test
    fun isFromLastSearch_isTrue() {
        val result = dto("castles").toDomain()
        assertTrue(result.isFromLastSearch)
    }

    @Test
    fun isInMyList_isFalse() {
        val result = dto("museums").toDomain()
        assertFalse(result.isInMyList)
    }

    @Test
    fun descriptionIsNull() {
        val result = dto("castles").toDomain()
        assertNull(result.description)
    }
}
