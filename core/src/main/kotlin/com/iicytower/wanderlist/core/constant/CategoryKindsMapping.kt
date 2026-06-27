package com.iicytower.wanderlist.core.constant

import com.iicytower.wanderlist.core.model.AttractionCategory

val CATEGORY_KINDS_MAP: Map<AttractionCategory, List<String>> = mapOf(
    AttractionCategory.CASTLES_AND_FORTIFICATIONS to listOf("castles", "fortifications", "palaces"),
    AttractionCategory.CHURCHES_AND_SACRED to listOf("churches", "cathedrals", "monasteries", "mosques", "synagogues", "temples", "other_temples"),
    AttractionCategory.MUSEUMS_AND_GALLERIES to listOf("museums", "art_galleries"),
    AttractionCategory.RUINS_AND_ARCHAEOLOGICAL to listOf("ruins", "archaeological_site", "other_archaeological_site"),
    AttractionCategory.NATURE_AND_PARKS to listOf("national_parks", "nature_reserves", "biosphere_reserves"),
    AttractionCategory.VIEWPOINTS to listOf("view_points"),
    AttractionCategory.MILITARY to listOf("battlefields", "fortifications"),
    AttractionCategory.MILLS_AND_TECH to listOf("windmills", "watermills", "industrial_facilities"),
    AttractionCategory.MEMORIALS_AND_CEMETERIES to listOf("burial_ground", "memorials", "monuments"),
    AttractionCategory.CAVES_AND_GEOLOGY to listOf("caves_and_tunnels", "geological_formations", "rocks")
)

fun Set<AttractionCategory>.toKindsParam(): String {
    val categories = if (isEmpty()) AttractionCategory.entries.toSet() else this
    return categories
        .flatMap { CATEGORY_KINDS_MAP[it] ?: emptyList() }
        .distinct()
        .joinToString(",")
}
