package com.asadraza.streamflix.feature.auth.forgotpassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.ui.components.formWidth
import com.asadraza.streamflix.feature.auth.R
import com.asadraza.streamflix.feature.auth.common.ErrorCard
import com.asadraza.streamflix.feature.auth.toStringRes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(onNavigateToLogin: () -> Unit, viewModel: ForgotPasswordViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ForgotPasswordEffect.NavigateToLogin -> onNavigateToLogin()
                is ForgotPasswordEffect.ShowToast -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.forgot_password_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(ForgotPasswordEvent.OnBackToLoginClick) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .formWidth()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LockReset,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.reset_password_heading),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text =
                stringResource(if (state.emailSent) R.string.reset_link_sent else R.string.reset_instructions),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!state.emailSent) {
                // Email Field
                OutlinedTextField(
                    value = state.email,
                    onValueChange = { viewModel.onEvent(ForgotPasswordEvent.OnEmailChange(it)) },
                    label = { Text(stringResource(R.string.email)) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    isError = state.emailError != null,
                    supportingText = state.emailError?.let { err -> { Text(stringResource(err.toStringRes())) } },
                    keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions =
                    KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onEvent(ForgotPasswordEvent.OnSendResetLinkClick)
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Send Reset Link Button
                Button(
                    onClick = { viewModel.onEvent(ForgotPasswordEvent.OnSendResetLinkClick) },
                    enabled = !state.isLoading,
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.send_reset_link))
                    }
                }
            } else {
                // Success Icon
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Back to Login Button
                OutlinedButton(
                    onClick = { viewModel.onEvent(ForgotPasswordEvent.OnBackToLoginClick) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.back_to_login))
                }
            }

            // Error Display
            state.errorType?.let { errorType ->
                Spacer(modifier = Modifier.height(16.dp))
                ErrorCard(
                    errorType = errorType,
                    onDismiss = { viewModel.onEvent(ForgotPasswordEvent.OnErrorDismiss) }
                )
            }
        }
    }
}
