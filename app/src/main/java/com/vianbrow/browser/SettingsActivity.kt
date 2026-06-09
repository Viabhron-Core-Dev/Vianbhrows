package com.vianbrow.browser

import android.content.Context
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vianbrow.browser.ui.theme.MyApplicationTheme

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        VianbrowLogger.i("Settings", "Settings: opened")
        
        val prefs = getSharedPreferences("vianbrow_settings", Context.MODE_PRIVATE)

        setContent {
            MyApplicationTheme {
                var searchEngine by remember { mutableStateOf(prefs.getString("setting_search_engine", "google") ?: "google") }
                var devMode by remember { mutableStateOf(prefs.getBoolean("setting_dev_mode", false)) }
                var loggingEnabled by remember { mutableStateOf(VianbrowLogger.isEnabled) }
                
                var showClearCacheDialog by remember { mutableStateOf(false) }
                var showClearAllDataDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Settings", color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Black,
                                titleContentColor = Color.White
                            )
                        )
                    },
                    containerColor = Color.Black
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // SEARCH ENGINE
                        SectionHeader("SEARCH ENGINE")
                        SearchEngineRow("Google", "google", searchEngine) { 
                            searchEngine = "google"
                            prefs.edit().putString("setting_search_engine", "google").apply()
                            VianbrowLogger.i("Settings", "Settings: search engine changed to google")
                        }
                        SearchEngineRow("DuckDuckGo", "duckduckgo", searchEngine) {
                            searchEngine = "duckduckgo"
                            prefs.edit().putString("setting_search_engine", "duckduckgo").apply()
                            VianbrowLogger.i("Settings", "Settings: search engine changed to duckduckgo")
                        }
                        SearchEngineRow("Bing", "bing", searchEngine) {
                            searchEngine = "bing"
                            prefs.edit().putString("setting_search_engine", "bing").apply()
                            VianbrowLogger.i("Settings", "Settings: search engine changed to bing")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // PRIVACY
                        SectionHeader("PRIVACY")
                        ButtonRow("Clear Cache") { showClearCacheDialog = true }
                        ButtonRow("Clear All Data") { showClearAllDataDialog = true }

                        Spacer(modifier = Modifier.height(16.dp))

                        // DEVELOPER
                        SectionHeader("DEVELOPER")
                        ToggleRow("Dev Mode", devMode) {
                            devMode = it
                            prefs.edit().putBoolean("setting_dev_mode", it).apply()
                            VianbrowLogger.i("Settings", "Settings: dev mode ${if (it) "ON" else "OFF"}")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // LOGGING
                        SectionHeader("LOGGING")
                        ToggleRow("Logging Enabled", loggingEnabled) {
                            loggingEnabled = it
                            VianbrowLogger.isEnabled = it
                            prefs.edit().putBoolean("setting_logging_enabled", it).apply()
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ABOUT
                        SectionHeader("ABOUT")
                        Text(
                            text = "App name: Vianbrow\nVersion: 1.0",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 16.sp
                        )
                    }
                }

                if (showClearCacheDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearCacheDialog = false },
                        title = { Text("Clear Cache") },
                        text = { Text("Are you sure you want to clear the browser cache?") },
                        confirmButton = {
                            TextButton(onClick = {
                                WebView(this@SettingsActivity).clearCache(true)
                                VianbrowLogger.i("Settings", "Settings: cache cleared")
                                Toast.makeText(this@SettingsActivity, "Cache cleared", Toast.LENGTH_SHORT).show()
                                showClearCacheDialog = false
                            }) { Text("Clear") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                if (showClearAllDataDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearAllDataDialog = false },
                        title = { Text("Clear All Data") },
                        text = { Text("Are you sure you want to clear all data including cache and cookies?") },
                        confirmButton = {
                            TextButton(onClick = {
                                WebView(this@SettingsActivity).clearCache(true)
                                CookieManager.getInstance().removeAllCookies(null)
                                CookieManager.getInstance().flush()
                                WebView(this@SettingsActivity).clearFormData()
                                WebView(this@SettingsActivity).clearHistory()
                                VianbrowLogger.i("Settings", "Settings: all data cleared")
                                Toast.makeText(this@SettingsActivity, "All data cleared", Toast.LENGTH_SHORT).show()
                                showClearAllDataDialog = false
                            }) { Text("Clear") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearAllDataDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SearchEngineRow(label: String, value: String, selectedValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = (value == selectedValue),
            onClick = null,
            colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.Gray)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun ButtonRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 16.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.DarkGray,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}
