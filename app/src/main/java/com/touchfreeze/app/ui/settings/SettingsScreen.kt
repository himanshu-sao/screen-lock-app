package com.touchfreeze.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState.errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Unlock PIN", style = MaterialTheme.typography.titleLarge)
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.isPinSet) {
                        Text("PIN is currently set.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showPinDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isLoading
                            ) {
                                Text("Change PIN")
                            }
                            OutlinedButton(
                                onClick = { viewModel.clearPin() },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isLoading
                            ) {
                                Text("Remove PIN")
                            }
                        }
                    } else {
                        Text(
                            "No PIN is set. Set one to enable touch freezing.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showPinDialog = true },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !uiState.isLoading
                        ) {
                            Text("Set PIN")
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("About", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Touch Freeze v1.1.0")
                    Text(
                        "Designed for parents to temporarily disable touch interaction while children watch videos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        PinSetupDialog(
            currentPinSet = uiState.isPinSet,
            onDismiss = { showPinDialog = false },
            onPinSaved = { savedPin ->
                showPinDialog = false
                viewModel.setPin(savedPin)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinSetupDialog(
    currentPinSet: Boolean,
    onDismiss: () -> Unit,
    onPinSaved: (String) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var pinInput by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentPinSet) "Change PIN" else "Set PIN") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text(if (step == 0) "Enter PIN" else "Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = errorMessage != null
                )

                if (step == 1) {
                    OutlinedTextField(
                        value = confirmedPin,
                        onValueChange = {
                            confirmedPin = it.filter { c -> c.isDigit() }
                            errorMessage = null
                        },
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = errorMessage != null
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when (step) {
                        0 -> {
                            if (pinInput.length < 4) {
                                errorMessage = "PIN must be at least 4 digits"
                            } else {
                                confirmedPin = pinInput
                                pinInput = ""
                                step = 1
                            }
                        }
                        1 -> {
                            if (pinInput != confirmedPin) {
                                errorMessage = "PINs do not match"
                            } else {
                                onPinSaved(pinInput)
                            }
                        }
                    }
                }
            ) {
                Text(if (step == 0) "Next" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
