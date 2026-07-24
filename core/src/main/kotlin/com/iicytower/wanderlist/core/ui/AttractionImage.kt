package com.iicytower.wanderlist.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WindPower
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.iicytower.wanderlist.core.model.AttractionCategory

val AttractionCategory.placeholderIcon: ImageVector
    get() = when (this) {
        AttractionCategory.CASTLES_AND_FORTIFICATIONS -> Icons.Default.Castle
        AttractionCategory.CHURCHES_AND_SACRED -> Icons.Default.Church
        AttractionCategory.MUSEUMS_AND_GALLERIES -> Icons.Default.Museum
        AttractionCategory.RUINS_AND_ARCHAEOLOGICAL -> Icons.Default.AccountBalance
        AttractionCategory.NATURE_AND_PARKS -> Icons.Default.Park
        AttractionCategory.VIEWPOINTS -> Icons.Default.Landscape
        AttractionCategory.MILITARY -> Icons.Default.MilitaryTech
        AttractionCategory.MILLS_AND_TECH -> Icons.Default.WindPower
        AttractionCategory.MEMORIALS_AND_CEMETERIES -> Icons.Default.LocalFlorist
        AttractionCategory.CAVES_AND_GEOLOGY -> Icons.Default.Terrain
    }

/**
 * Zdjęcie atrakcji z fallbackiem na stylizowany placeholder kategorii —
 * używane na karcie wyniku, w szczegółach, popupie mapy i listach podróży.
 * Błąd ładowania (404, brak sieci) także pokazuje placeholder.
 */
@Composable
fun AttractionImage(
    imageUrl: String?,
    category: AttractionCategory,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    if (imageUrl == null) {
        CategoryPlaceholder(category, modifier)
    } else {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = { CategoryPlaceholder(category, Modifier.fillMaxSize()) },
            error = { CategoryPlaceholder(category, Modifier.fillMaxSize()) }
        )
    }
}

@Composable
private fun CategoryPlaceholder(category: AttractionCategory, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = category.placeholderIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp)
        )
    }
}
