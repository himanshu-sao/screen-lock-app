package com.touchfreeze.app.ui.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.touchfreeze.app.service.FloatingDotService

@Composable
fun HomeScreen(onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val isOverlayEnabled = Settings.canDrawOverlays(context)

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Touch Freeze",
                    style = MaterialTheme.typography.headlineLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap the floating dot to lock your screen\nwhile your child watches videos",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                if (!isOverlayEnabled) {
                    Button(
                        onClick = { requestOverlayPermission(context) },
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text("Grant Overlay Permission")
                    }
                } else {
                    Button(
                        onClick = { startFloatingDotService(context) },
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Activate Screen Lock")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { stopFloatingDotService(context) },
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Deactivate")
                }
            }

            IconButton(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }
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
