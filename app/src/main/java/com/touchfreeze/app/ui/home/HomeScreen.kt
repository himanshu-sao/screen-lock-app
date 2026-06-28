package com.touchfreeze.app.ui.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.touchfreeze.app.service.FloatingDotService
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val isOverlayEnabled = Settings.canDrawOverlays(context)
    var isServiceRunning by remember { mutableStateOf(false) }
    var showToast by remember { mutableStateOf<String?>(null) }

    // Check service status periodically
    LaunchedEffect(Unit) {
        while (true) {
            isServiceRunning = isServiceActive(context)
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Screen Lock for Kids") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Card
            StatusCard(
                isOverlayEnabled = isOverlayEnabled,
                isServiceRunning = isServiceRunning,
                onGrantPermission = { requestOverlayPermission(context) },
                onActivate = { startFloatingDotService(context) },
                onDeactivate = { stopFloatingDotService(context); isServiceRunning = false }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // How It Works
            HowItWorksSection()

            Spacer(modifier = Modifier.height(16.dp))

            // Tips
            TipsSection()

            // Toast
            if (showToast != null) {
                LaunchedEffect(showToast) {
                    delay(2000)
                    showToast = null
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    isOverlayEnabled: Boolean,
    isServiceRunning: Boolean,
    onGrantPermission: () -> Unit,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon with background
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        if (isServiceRunning) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = if (isServiceRunning) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isServiceRunning) "Screen Lock Active" else "Screen Lock Inactive",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when {
                    !isOverlayEnabled -> "Grant overlay permission to use Screen Lock"
                    isServiceRunning -> "The floating dot is visible. Tap it to lock/unlock the screen."
                    else -> "Tap Activate to show the floating lock button"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            when {
                !isOverlayEnabled -> {
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Grant Overlay Permission")
                    }
                }
                isServiceRunning -> {
                    Button(
                        onClick = onDeactivate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Stop Screen Lock")
                    }
                }
                else -> {
                    Button(
                        onClick = onActivate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Activate Screen Lock")
                    }
                }
            }
        }
    }
}

@Composable
private fun HowItWorksSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "How It Works",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            StepItem(
                number = "1",
                title = "Activate",
                description = "Tap the button above to show the floating lock button",
                icon = Icons.Default.PlayArrow
            )

            StepItem(
                number = "2",
                title = "Open Any App",
                description = "Open YouTube, WhatsApp, or any video your child wants to watch",
                icon = Icons.Default.TouchApp
            )

            StepItem(
                number = "3",
                title = "Tap to Lock",
                description = "Tap the floating dot to lock the screen. All touch is blocked!",
                icon = Icons.Default.Lock
            )

            StepItem(
                number = "4",
                title = "Tap to Unlock",
                description = "Tap the floating dot again to restore touch when you're ready",
                icon = Icons.Default.LockOpen
            )
        }
    }
}

@Composable
private fun StepItem(number: String, title: String, description: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Number circle
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Tips",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TipItem("Drag the floating dot to move it anywhere on screen")
            TipItem("Works with any app: YouTube, Netflix, WhatsApp, and more")
            TipItem("Set a PIN in Settings for extra security")
            TipItem("The dot stays visible even when you switch apps")
        }
    }
}

@Composable
private fun TipItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isServiceActive(context: Context): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    return manager.getRunningServices(Integer.MAX_VALUE)
        .any { it.service.className == FloatingDotService::class.java.name }
}

private fun startFloatingDotService(context: Context) {
    val intent = Intent(context, FloatingDotService::class.java).apply {
        action = FloatingDotService.ACTION_SHOW
    }
    context.startService(intent)
}

private fun stopFloatingDotService(context: Context) {
    val intent = Intent(context, FloatingDotService::class.java).apply {
        action = FloatingDotService.ACTION_HIDE
    }
    context.startService(intent)
}

private fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        android.net.Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}
