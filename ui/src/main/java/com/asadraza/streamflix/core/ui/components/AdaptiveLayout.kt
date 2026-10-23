package com.asadraza.streamflix.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DEFAULT_FORM_WIDTH = 480.dp

/**
 * Keeps single-column content (forms, article-style pages) readable on wide windows: it fills
 * narrow screens and is centred at [maxWidth] on tablets and unfolded devices.
 */
fun Modifier.formWidth(maxWidth: Dp = DEFAULT_FORM_WIDTH): Modifier = this
    .fillMaxWidth()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = maxWidth)
    .fillMaxWidth()
