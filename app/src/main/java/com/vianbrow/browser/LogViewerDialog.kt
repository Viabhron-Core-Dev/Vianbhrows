package com.vianbrow.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var isEnabled by remember { mutableStateOf(VianbrowLogger.isEnabled) }
    var selectedHours by remember { mutableStateOf(24) }
    var showCustomPicker by remember { mutableStateOf(false) }
    
    // We update this whenever filter changes or force refresh
    var logs by remember { mutableStateOf(VianbrowLogger.getLogs(selectedHours)) }
    
    fun refreshLogs() {
        logs = VianbrowLogger.getLogs(selectedHours)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                TopAppBar(
                    title = { Text("App Logs") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Logging enabled", style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = isEnabled,
                                onCheckedChange = {
                                    isEnabled = it
                                    VianbrowLogger.isEnabled = it
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                )

                // Filters
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(selected = selectedHours == 1, onClick = { selectedHours = 1; refreshLogs() }, label = { Text("1h") })
                    FilterChip(selected = selectedHours == 6, onClick = { selectedHours = 6; refreshLogs() }, label = { Text("6h") })
                    FilterChip(selected = selectedHours == 12, onClick = { selectedHours = 12; refreshLogs() }, label = { Text("12h") })
                    FilterChip(selected = !listOf(1, 6, 12, 24).contains(selectedHours) && !showCustomPicker && selectedHours != 24, onClick = { showCustomPicker = true }, label = { Text(if (!listOf(1, 6, 12, 24).contains(selectedHours)) "${selectedHours}h" else "Custom") })
                    FilterChip(selected = selectedHours == 24, onClick = { selectedHours = 24; refreshLogs() }, label = { Text("24h") })
                }
                
                if (showCustomPicker) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var customHourText by remember { mutableStateOf(selectedHours.toString()) }
                        OutlinedTextField(
                            value = customHourText,
                            onValueChange = { customHourText = it },
                            label = { Text("Hours") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val h = customHourText.toIntOrNull()
                            if (h != null && h > 0) {
                                selectedHours = h
                                refreshLogs()
                                showCustomPicker = false
                            }
                        }) {
                            Text("Set")
                        }
                    }
                }

                // Log List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    items(logs) { log ->
                        val color = when (log.level) {
                            "ERROR" -> MaterialTheme.colorScheme.errorContainer
                            "WARN" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = color)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = log.toFormattedString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (log.level == "ERROR") FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Bottom Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            VianbrowLogger.exportLogs(context, logs)
                            Toast.makeText(context, "Logs saved to Downloads", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Download")
                        }
                        
                        Button(onClick = {
                            val text = logs.joinToString("\n") { it.toFormattedString() }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Vianbrow Logs", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("Copy to Clipboard")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            VianbrowLogger.clearLogs()
                            refreshLogs()
                            Toast.makeText(context, "All logs cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear All Logs")
                    }
                }
            }
        }
    }
}
