package com.asadraza.streamflix.feature.home.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.asadraza.streamflix.core.model.movie.MovieCategory

/**
 * Category tabs
 *
 * Separate composable for tabs when User switches between categories
 * At the Top of home screen
 */
@Composable
fun CategoryTabs(
    categories: List<MovieCategory>,
    selectedCategory: MovieCategory,
    onCategorySelect: (MovieCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    if (categories.isNotEmpty()) {
        SecondaryScrollableTabRow(
            categories.indexOf(selectedCategory),
            modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                Tab(
                    selected = category == selectedCategory,
                    onClick = { onCategorySelect(category) },
                    text = { Text(category.title) }
                )
            }
        }
    }
}