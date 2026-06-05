package com.vianbrow.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.vianbrow.browser.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    VianbrowLogger.i("MainActivity", "onCreate called")
    setContent {
      MyApplicationTheme {
        MainScreen()
      }
    }
  }
}

@Composable
fun MainScreen() {
  var showLogViewer by remember { mutableStateOf(false) }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Black,
      floatingActionButton = {
          FloatingActionButton(onClick = { showLogViewer = true }) {
              Icon(Icons.Default.Info, contentDescription = "View Logs")
          }
      }
  ) { innerPadding ->
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          // Empty shell for Vianbrow
      }
  }

  if (showLogViewer) {
      LogViewerDialog(onDismiss = { showLogViewer = false })
  }
}
