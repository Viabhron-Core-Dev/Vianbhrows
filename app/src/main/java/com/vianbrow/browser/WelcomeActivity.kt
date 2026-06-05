package com.vianbrow.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vianbrow.browser.ui.theme.MyApplicationTheme

class WelcomeActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            proceedToMain()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        VianbrowLogger.i("WelcomeActivity", "onCreate called")
        
        val prefs = getSharedPreferences("vianbrow_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = !prefs.getBoolean("first_launch_complete", false)
        
        if (!isFirstLaunch) {
            VianbrowLogger.i("WelcomeActivity", "First launch already complete, skipping")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Vianbrow",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Setting up your browser",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
        
        requestPermissions()
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            VianbrowLogger.i("WelcomeActivity", "Requesting permissions")
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            VianbrowLogger.i("WelcomeActivity", "No permissions required to request")
            proceedToMain()
        }
    }

    private fun proceedToMain() {
        val prefs = getSharedPreferences("vianbrow_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("first_launch_complete", true).apply()
        
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
