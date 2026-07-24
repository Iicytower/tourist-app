package com.iicytower.wanderlist.core.model

import com.iicytower.wanderlist.core.R

/**
 * Zlokalizowana nazwa kategorii do UI. `displayName` (polski) zostaje jako
 * słownictwo promptów LLM — nie podlega lokalizacji interfejsu.
 */
val AttractionCategory.displayNameRes: Int
    get() = when (this) {
        AttractionCategory.CASTLES_AND_FORTIFICATIONS -> R.string.category_castles_and_fortifications
        AttractionCategory.CHURCHES_AND_SACRED -> R.string.category_churches_and_sacred
        AttractionCategory.MUSEUMS_AND_GALLERIES -> R.string.category_museums_and_galleries
        AttractionCategory.RUINS_AND_ARCHAEOLOGICAL -> R.string.category_ruins_and_archaeological
        AttractionCategory.NATURE_AND_PARKS -> R.string.category_nature_and_parks
        AttractionCategory.VIEWPOINTS -> R.string.category_viewpoints
        AttractionCategory.MILITARY -> R.string.category_military
        AttractionCategory.MILLS_AND_TECH -> R.string.category_mills_and_tech
        AttractionCategory.MEMORIALS_AND_CEMETERIES -> R.string.category_memorials_and_cemeteries
        AttractionCategory.CAVES_AND_GEOLOGY -> R.string.category_caves_and_geology
    }
