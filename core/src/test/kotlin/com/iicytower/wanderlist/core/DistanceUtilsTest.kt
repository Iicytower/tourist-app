package com.iicytower.wanderlist.core

import com.iicytower.wanderlist.core.util.calculateDistanceKm
import com.iicytower.wanderlist.core.util.formatDistance
import org.junit.Assert.*
import org.junit.Test

class DistanceUtilsTest {

    @Test
    fun `Krakow to Warsaw is approximately 252 km`() {
        val distance = calculateDistanceKm(50.0619, 19.9368, 52.2297, 21.0122)
        assertEquals(252.0, distance, 2.0)
    }

    @Test
    fun `same point is zero distance`() {
        val distance = calculateDistanceKm(50.0, 20.0, 50.0, 20.0)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `short distance is accurate`() {
        // ~1 km north
        val distance = calculateDistanceKm(50.0, 20.0, 50.009, 20.0)
        assertEquals(1.0, distance, 0.1)
    }

    @Test
    fun `format below 1km shows meters`() {
        assertEquals("800 m", formatDistance(0.8))
        assertEquals("500 m", formatDistance(0.5))
        assertEquals("100 m", formatDistance(0.1))
    }

    @Test
    fun `format above 1km shows km with comma`() {
        assertEquals("1,2 km", formatDistance(1.2))
        assertEquals("3,5 km", formatDistance(3.5))
    }

    @Test
    fun `format whole km shows no decimal`() {
        assertEquals("5 km", formatDistance(5.0))
        assertEquals("12 km", formatDistance(12.0))
    }
}
