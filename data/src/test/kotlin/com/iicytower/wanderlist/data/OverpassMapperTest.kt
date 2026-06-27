package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.core.model.AttractionCategory
import com.iicytower.wanderlist.data.remote.overpass.dto.OverpassElement
import com.iicytower.wanderlist.data.remote.overpass.mapper.resolveCategory
import com.iicytower.wanderlist.data.remote.overpass.mapper.toAttraction
import org.junit.Assert.*
import org.junit.Test

class OverpassMapperTest {

    private fun element(
        id: Long = 1L,
        lat: Double = 50.0,
        lon: Double = 20.0,
        tags: Map<String, String> = emptyMap()
    ) = OverpassElement(type = "node", id = id, lat = lat, lon = lon, tags = tags)

    @Test
    fun toAttraction_returnsNull_whenNoName() {
        val el = element(tags = mapOf("tourism" to "museum"))
        assertNull(el.toAttraction(50.0, 20.0))
    }

    @Test
    fun toAttraction_returnsAttraction_whenNamePresent() {
        val el = element(tags = mapOf("name" to "Muzeum", "tourism" to "museum"))
        val result = el.toAttraction(50.0, 20.0)
        assertNotNull(result)
        assertEquals("Muzeum", result!!.name)
        assertEquals("n1", result.xid)
    }

    @Test
    fun toAttraction_xid_usesTypePrefix() {
        val node = OverpassElement(type = "node", id = 42L, lat = 50.0, lon = 20.0, tags = mapOf("name" to "Test"))
        val way = OverpassElement(
            type = "way", id = 99L, lat = null, lon = null,
            center = com.iicytower.wanderlist.data.remote.overpass.dto.OverpassCenter(50.1, 20.1),
            tags = mapOf("name" to "Test")
        )
        assertEquals("n42", node.toAttraction(50.0, 20.0)!!.xid)
        assertEquals("w99", way.toAttraction(50.0, 20.0)!!.xid)
    }

    @Test
    fun toAttraction_usesCenter_whenLatLonNull() {
        val way = OverpassElement(
            type = "way", id = 1L, lat = null, lon = null,
            center = com.iicytower.wanderlist.data.remote.overpass.dto.OverpassCenter(51.0, 21.0),
            tags = mapOf("name" to "Zamek")
        )
        val result = way.toAttraction(51.0, 21.0)
        assertNotNull(result)
        assertEquals(51.0, result!!.latitude, 0.0001)
    }

    @Test
    fun resolveCategory_castle() {
        assertEquals(
            AttractionCategory.CASTLES_AND_FORTIFICATIONS,
            resolveCategory(mapOf("historic" to "castle"))
        )
    }

    @Test
    fun resolveCategory_museum() {
        assertEquals(
            AttractionCategory.MUSEUMS_AND_GALLERIES,
            resolveCategory(mapOf("tourism" to "museum"))
        )
    }

    @Test
    fun resolveCategory_church() {
        assertEquals(
            AttractionCategory.CHURCHES_AND_SACRED,
            resolveCategory(mapOf("amenity" to "place_of_worship"))
        )
    }

    @Test
    fun resolveCategory_viewpoint() {
        assertEquals(
            AttractionCategory.VIEWPOINTS,
            resolveCategory(mapOf("tourism" to "viewpoint"))
        )
    }

    @Test
    fun resolveCategory_park() {
        assertEquals(
            AttractionCategory.NATURE_AND_PARKS,
            resolveCategory(mapOf("leisure" to "park"))
        )
    }

    @Test
    fun resolveCategory_ruins() {
        assertEquals(
            AttractionCategory.RUINS_AND_ARCHAEOLOGICAL,
            resolveCategory(mapOf("historic" to "ruins"))
        )
    }

    @Test
    fun resolveCategory_memorial() {
        assertEquals(
            AttractionCategory.MEMORIALS_AND_CEMETERIES,
            resolveCategory(mapOf("historic" to "memorial"))
        )
    }

    @Test
    fun resolveCategory_cave() {
        assertEquals(
            AttractionCategory.CAVES_AND_GEOLOGY,
            resolveCategory(mapOf("natural" to "cave_entrance"))
        )
    }
}
