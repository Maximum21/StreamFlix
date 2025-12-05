package com.asadraza.streamflix.feature.auth.email_verification

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asadraza.streamflix.core.common.result.getUserMessage

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
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Email") },
                actions = {
                    TextButton(onClick = { viewModel.onEvent(EmailVerificationEvent.OnLogoutClick) }) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
            text = "Verify Your Email",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        // Email address
        if (state.email.isNotEmpty()) {
            Text(
                text = "We sent a verification link to:",
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
            text = "Please check your email and click the verification link. " +
                    "This page will automatically update when your email is verified.",
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
            Text("Resend Verification Email")
        }

        // Info card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 Tips:",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "• Check your spam folder",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• The link expires in 1 hour",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "• This page updates automatically",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Error display
        state.errorType?.let { errorType ->
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorType.getUserMessage(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error"
                        )
                    }
                }
            }
        }
    }
}