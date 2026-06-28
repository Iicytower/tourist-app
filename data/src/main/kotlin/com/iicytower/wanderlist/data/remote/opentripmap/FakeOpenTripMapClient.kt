package com.iicytower.wanderlist.data.remote.opentripmap

import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDetailDto
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmAttractionDto
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmInfo
import com.iicytower.wanderlist.data.remote.opentripmap.dto.OtmPoint
import kotlin.math.sqrt

class FakeOpenTripMapClient : OpenTripMapClient {

    private val attractions = listOf(
        // Kraków centrum
        OtmAttractionDto("wawel_castle", "Zamek Wawelski", 0.0, 7, "castles,palaces", OtmPoint(19.9352, 50.0540)),
        OtmAttractionDto("wawel_cathedral", "Katedra Wawelska", 200.0, 7, "churches,cathedrals", OtmPoint(19.9357, 50.0545)),
        OtmAttractionDto("czartoryski_museum", "Muzeum Czartoryskich", 1200.0, 6, "museums", OtmPoint(19.9332, 50.0631)),
        OtmAttractionDto("cloth_hall", "Sukiennice (Muzeum Narodowe)", 1500.0, 7, "museums,art_galleries", OtmPoint(19.9373, 50.0617)),
        OtmAttractionDto("barbican", "Barbakan Krakowski", 1700.0, 5, "fortifications", OtmPoint(19.9370, 50.0640)),
        OtmAttractionDto("st_mary_church", "Kościół Mariacki", 1600.0, 7, "churches,cathedrals", OtmPoint(19.9393, 50.0614)),
        OtmAttractionDto("planty_park", "Park Planty", 1800.0, 4, "national_parks,nature_reserves", OtmPoint(19.9365, 50.0610)),
        OtmAttractionDto("kosciuszko_mound", "Kopiec Kościuszki", 4500.0, 5, "memorials,monuments,view_points", OtmPoint(19.8956, 50.0565)),
        OtmAttractionDto("wieliczka_salt_mine", "Kopalnia Soli Wieliczka", 14000.0, 7, "caves_and_tunnels,industrial_facilities", OtmPoint(20.0551, 49.9831)),
        OtmAttractionDto("auschwitz_museum", "Muzeum Auschwitz-Birkenau", 60000.0, 7, "memorials,museums,burial_ground", OtmPoint(19.2037, 50.0274)),
        // Dodatkowe dla różnych kategorii
        OtmAttractionDto("ojcow_caves", "Jaskinia Łokietka", 24000.0, 5, "caves_and_tunnels,geological_formations", OtmPoint(19.8300, 50.2050)),
        OtmAttractionDto("ojcow_castle", "Zamek w Ojcowie (ruiny)", 24500.0, 4, "ruins,castles", OtmPoint(19.8275, 50.2040)),
        OtmAttractionDto("wola_justowska", "Rezerwat Panieńskie Skały", 6000.0, 3, "nature_reserves,rocks", OtmPoint(19.8901, 50.0781)),
        OtmAttractionDto("krakus_mound", "Kopiec Krakusa", 3500.0, 4, "memorials,monuments,archaeological_site", OtmPoint(19.9582, 50.0397)),
        OtmAttractionDto("benedictine_abbey", "Opactwo Benedyktynów w Tyńcu", 13000.0, 5, "monasteries,churches", OtmPoint(19.8203, 50.0200)),
        OtmAttractionDto("windy_viewpoint", "Punkt widokowy na Kopcu Józefa", 1000.0, 3, "view_points", OtmPoint(19.9420, 50.0550)),
        OtmAttractionDto("schindler_factory", "Fabryka Schindlera (Muzeum)", 2000.0, 6, "museums", OtmPoint(19.9578, 50.0490)),
        OtmAttractionDto("podgorze_ghetto", "Apteka Pod Orłem", 2100.0, 5, "memorials,museums", OtmPoint(19.9498, 50.0459)),
        OtmAttractionDto("wielka_synagogue", "Synagoga Remuh", 2300.0, 5, "synagogues", OtmPoint(19.9444, 50.0509)),
        OtmAttractionDto("dragon_cave", "Smocza Jama", 400.0, 5, "caves_and_tunnels", OtmPoint(19.9336, 50.0530))
    )

    override suspend fun searchAttractions(
        latitude: Double,
        longitude: Double,
        radiusMeters: Int,
        kinds: String
    ): Result<List<OtmAttractionDto>> {
        val activeKinds = kinds.split(",").map { it.trim() }.toSet()
        val result = attractions
            .filter { attraction ->
                val dist = haversineMeters(latitude, longitude, attraction.point.lat, attraction.point.lon)
                dist <= radiusMeters && attraction.kinds.split(",").any { it.trim() in activeKinds }
            }
            .map { attraction ->
                val dist = haversineMeters(latitude, longitude, attraction.point.lat, attraction.point.lon)
                attraction.copy(dist = dist)
            }
            .sortedBy { it.dist }
            .take(100)
        return Result.success(result)
    }

    override suspend fun getAttractionDetail(xid: String): Result<OtmAttractionDetailDto> {
        val attraction = attractions.find { it.xid == xid }
            ?: return Result.failure(NoSuchElementException("Attraction $xid not found"))
        return Result.success(
            OtmAttractionDetailDto(
                xid = attraction.xid,
                name = attraction.name,
                kinds = attraction.kinds,
                point = attraction.point,
                info = OtmInfo(descr = "Atrakcja turystyczna: ${attraction.name}")
            )
        )
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
