package com.iicytower.wanderlist.data

import com.iicytower.wanderlist.data.local.OsmOpeningHoursParser
import com.iicytower.wanderlist.domain.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class OsmOpeningHoursParserTest {

    // środa 2026-07-15, 12:00
    private val wednesdayNoon = LocalDateTime.of(2026, 7, 15, 12, 0)

    @Test
    fun `parses day range with hours`() {
        val result = OsmOpeningHoursParser.parse("Mo-Fr 09:00-17:00", wednesdayNoon)
        assertNotNull(result.slots)
        assertEquals(5, result.slots!!.size)
        assertEquals(TimeSlot(1, 540, 1020), result.slots!![0])
        assertEquals(true, result.isOpenNow)
    }

    @Test
    fun `parses multiple rules and off`() {
        val result = OsmOpeningHoursParser.parse("Mo-Fr 09:00-17:00; Sa 10:00-14:00; Su off", wednesdayNoon)
        val slots = result.slots!!
        assertEquals(6, slots.size)
        assertTrue(slots.any { it.dayOfWeek == 6 && it.openMinutes == 600 })
        assertFalse(slots.any { it.dayOfWeek == 7 })
    }

    @Test
    fun `parses day list and multiple time ranges`() {
        val result = OsmOpeningHoursParser.parse("Mo,We,Fr 09:00-12:00,13:00-17:00", wednesdayNoon)
        assertEquals(6, result.slots!!.size)
        // 12:00 wypada w przerwie
        assertEquals(false, result.isOpenNow)
    }

    @Test
    fun `24_7 is always open`() {
        val result = OsmOpeningHoursParser.parse("24/7", wednesdayNoon)
        assertEquals(7, result.slots!!.size)
        assertEquals(true, result.isOpenNow)
    }

    @Test
    fun `closed day means not open`() {
        val sundayNoon = LocalDateTime.of(2026, 7, 19, 12, 0)
        val result = OsmOpeningHoursParser.parse("Mo-Fr 09:00-17:00", sundayNoon)
        assertEquals(false, result.isOpenNow)
    }

    @Test
    fun `unknown format keeps raw and null slots`() {
        val result = OsmOpeningHoursParser.parse("sunrise-sunset", wednesdayNoon)
        assertNull(result.slots)
        assertNull(result.isOpenNow)
        assertEquals("sunrise-sunset", result.raw)
    }

    @Test
    fun `overnight range counts as open before midnight`() {
        val fridayNight = LocalDateTime.of(2026, 7, 17, 23, 0)
        val result = OsmOpeningHoursParser.parse("Fr 20:00-02:00", fridayNight)
        assertEquals(true, result.isOpenNow)
    }
}
