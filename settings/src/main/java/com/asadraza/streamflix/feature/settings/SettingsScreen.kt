package com.asadraza.streamflix.feature.settings

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.model.settings.ThemeMode
import com.asadraza.streamflix.core.ui.text.toUiText

@Composable
fun SettingsScreen(
    appVersion: String,
    privacyPolicyUrl: String,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message.asString(context))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        SettingsContent(
            state = state,
            appVersion = appVersion,
            privacyPolicyUrl = privacyPolicyUrl,
            onEvent = viewModel::onEvent,
            modifier = Modifier.padding(padding)
        )
    }

    if (state.showDeleteDialog) {
        DeleteAccountDialog(
            state = state,
            onConfirm = { viewModel.onEvent(SettingsEvent.OnDeleteConfirm(it)) },
            onDismiss = { viewModel.onEvent(SettingsEvent.OnDeleteDismiss) }
        )
    }
}

@Composable
internal fun SettingsContent(
    state: SettingsState,
    appVersion: String,
    privacyPolicyUrl: String,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        SectionHeader(R.string.settings_section_account)
        ListItem(
            overlineContent = { Text(stringResource(R.string.settings_signed_in_as)) },
            headlineContent = { Text(state.email) }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_sign_out)) },
            modifier = Modifier.clickableRow { onEvent(SettingsEvent.OnSignOutClick) }
        )
        ListItem(
            headlineContent = {
                Text(stringResource(R.string.settings_delete_account), color = MaterialTheme.colorScheme.error)
            },
            supportingContent = { Text(stringResource(R.string.settings_delete_account_summary)) },
            modifier = Modifier.clickableRow { onEvent(SettingsEvent.OnDeleteAccountClick) }
        )
        HorizontalDivider()

        ThemeRow(mode = state.themeMode, onSelect = { onEvent(SettingsEvent.OnThemeSelect(it)) })
        LanguageRow()

        SectionHeader(R.string.settings_section_privacy)
        NotificationsRow(
            enabled = state.notificationsEnabled,
            onToggle = { onEvent(SettingsEvent.OnNotificationsToggle(it)) },
            onPermissionDenied = { onEvent(SettingsEvent.OnNotificationPermissionDenied) }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_analytics)) },
            supportingContent = { Text(stringResource(R.string.settings_analytics_summary)) },
            trailingContent = {
                Switch(
                    checked = state.analyticsEnabled,
                    onCheckedChange = { onEvent(SettingsEvent.OnAnalyticsToggle(it)) }
                )
            }
        )
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_privacy_policy)) },
            modifier = Modifier.clickableRow { uriHandler.openUri(privacyPolicyUrl) }
        )
        HorizontalDivider()

        SectionHeader(R.string.settings_section_storage)
        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_clear_cache)) },
            supportingContent = { Text(stringResource(R.string.settings_clear_cache_summary)) },
            modifier = Modifier.clickableRow { onEvent(SettingsEvent.OnClearCacheClick) }
        )
        HorizontalDivider()

        SectionHeader(R.string.settings_section_about)
        ListItem(headlineContent = { Text(stringResource(R.string.settings_version, appVersion)) })
        ListItem(headlineContent = { Text(stringResource(R.string.settings_tmdb_attribution)) })
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionHeader(titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() }
    )
}

@Composable
private fun DeleteAccountDialog(state: SettingsState, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    // Not saved: the password must not end up in the saved instance state
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.delete_dialog_message))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.delete_dialog_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = state.deleteError != null,
                    supportingText = state.deleteError?.let { error -> { Text(error.toUiText().asString()) } },
                    modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty() && !state.isDeleting
            ) {
                if (state.isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.delete_dialog_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isDeleting) {
                Text(stringResource(R.string.delete_dialog_cancel))
            }
        }
    )
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = clickable(role = Role.Button, onClick = onClick)

/** Android 13+ asks for notification permission at runtime; it is requested only when turning the switch on. */
@Composable
private fun NotificationsRow(enabled: Boolean, onToggle: (Boolean) -> Unit, onPermissionDenied: () -> Unit) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onToggle(true) else onPermissionDenied()
    }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_notifications)) },
        supportingContent = { Text(stringResource(R.string.settings_notifications_summary)) },
        trailingContent = {
            Switch(
                checked = enabled,
                onCheckedChange = { wantsEnabled ->
                    val needsPermission = wantsEnabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onToggle(wantsEnabled)
                    }
                }
            )
        }
    )
}

/** Per-app language is a system setting from Android 13; older versions follow the system language. */
@Composable
private fun LanguageRow() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_language)) },
        supportingContent = { Text(stringResource(R.string.settings_language_summary)) },
        modifier = Modifier.clickableRow {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_APP_LOCALE_SETTINGS, Uri.fromParts("package", context.packageName, null))
                )
            } catch (_: ActivityNotFoundException) {
                // Some devices ship without the per-app language screen, so the row does nothing there
            }
        }
    )
}

@Composable
private fun ThemeRow(mode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_theme)) },
        supportingContent = { Text(stringResource(mode.labelRes())) },
        modifier = Modifier.clickableRow { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.settings_theme)) },
            text = {
                Column(modifier = Modifier.selectableGroup()) {
                    ThemeMode.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = option == mode,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onSelect(option)
                                        showDialog = false
                                    }
                                )
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = option == mode, onClick = null)
                            Text(text = stringResource(option.labelRes()), modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.delete_dialog_cancel)) }
            }
        )
    }
}

@StringRes
private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.Dark -> R.string.settings_theme_dark
}
