package com.asadraza.streamflix.feature.auth.emailverification

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.ui.components.formWidth
import com.asadraza.streamflix.core.ui.text.toUiText
import com.asadraza.streamflix.feature.auth.R

/**
 * Email Verification Screen - Composable UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: EmailVerificationViewModel = hiltViewModel()
) {
    // Collect state with lifecycle awareness
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Handle one-time effects (navigation, toasts)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is EmailVerificationEffect.NavigateToHome -> onNavigateToHome()
                is EmailVerificationEffect.NavigateToLogin -> onNavigateToLogin()
                is EmailVerificationEffect.ShowToast -> {
                    Toast.makeText(context, effect.message.asString(context), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.verify_email_title)) },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(EmailVerificationEvent.OnLogoutClick) }) {
                        Text(stringResource(R.string.logout))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .formWidth()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.isLoading) {
                // Show loading indicator
                CircularProgressIndicator()
            } else {
                // Show verification content
                EmailVerificationContent(
                    state = state,
                    onResendClick = { viewModel.onEvent(EmailVerificationEvent.OnResendVerification) },
                    onVerificationCompletedClick = { viewModel.onEvent(EmailVerificationEvent.OnCheckVerification) },
                    onDismissError = { viewModel.onEvent(EmailVerificationEvent.OnErrorDismiss) }
                )
            }
        }
    }
}

@Composable
private fun EmailVerificationContent(
    state: EmailVerificationState,
    onResendClick: () -> Unit,
    onVerificationCompletedClick: () -> Unit,
    onDismissError: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Icon or illustration
        Icon(
            imageVector = Icons.Default.Email,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        // Title
        Text(
            text = stringResource(R.string.verify_your_email),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        // Email address
        if (state.email.isNotEmpty()) {
            Text(
                text = stringResource(R.string.verification_sent_to),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = state.email,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        // Instructions
        Text(
            text =
            stringResource(R.string.verification_instructions),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Resend button
        OutlinedButton(
            onClick = onResendClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(stringResource(R.string.resend_verification))
        }

        // Resend button
        OutlinedButton(
            onClick = onVerificationCompletedClick,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text(stringResource(R.string.verification_completed))
        }

        // Info card
        Card(
            modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 16.dp),
            colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.tips_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.tip_spam),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.tip_expiry),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = stringResource(R.string.tip_auto),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Error display
        state.errorType?.let { errorType ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorType.toUiText().asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.dismiss_error)
                        )
                    }
                }
            }
        }
    }
}
