package com.iicytower.wanderlist.core

import com.iicytower.wanderlist.core.util.languageForCountryCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryLanguageTest {

    @Test
    fun `returns language for known countries`() {
        assertEquals("ja", languageForCountryCode("JP"))
        assertEquals("ko", languageForCountryCode("KR"))
        assertEquals("pl", languageForCountryCode("PL"))
        assertEquals("el", languageForCountryCode("GR"))
    }

    @Test
    fun `is case insensitive`() {
        assertEquals("ja", languageForCountryCode("jp"))
    }

    @Test
    fun `returns null for unknown country`() {
        assertNull(languageForCountryCode("ZZ"))
    }
}
