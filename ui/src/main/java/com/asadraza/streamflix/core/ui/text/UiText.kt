package com.asadraza.streamflix.core.ui.text

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Text that is resolved at the UI edge, so ViewModels and repositories never hold display strings.
 */
sealed interface UiText {
    data class Dynamic(val value: String) : UiText

    class Resource(@param:StringRes val id: Int, vararg val args: Any) : UiText

    // The platform string APIs take varargs, so the spread cannot be avoided here
    @Suppress("SpreadOperator")
    @Composable
    fun asString(): String = when (this) {
        is Dynamic -> value
        is Resource -> stringResource(id, *args)
    }

    @Suppress("SpreadOperator")
    fun asString(context: Context): String = when (this) {
        is Dynamic -> value
        is Resource -> context.getString(id, *args)
    }
}
