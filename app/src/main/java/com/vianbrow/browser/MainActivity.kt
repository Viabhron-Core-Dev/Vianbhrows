package com.vianbrow.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
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
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  var currentUrl by remember { mutableStateOf("") }
  val context = LocalContext.current
  val activity = context as? ComponentActivity

  BackHandler {
    if (webViewRef?.canGoBack() == true) {
      webViewRef?.goBack()
    } else {
      VianbrowLogger.i("WebView", "WebView: no back history")
      activity?.finish()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Black,
      topBar = {
        AddressBar(
            url = currentUrl,
            onNavigate = { url ->
              webViewRef?.loadUrl(url)
            }
        )
      },
      bottomBar = {
        BottomNavBar(
            onBack = {
              if (webViewRef?.canGoBack() == true) {
                webViewRef?.goBack()
              } else {
                VianbrowLogger.i("WebView", "WebView: no back history")
              }
            },
            onForward = {
              if (webViewRef?.canGoForward() == true) {
                webViewRef?.goForward()
              }
            },
            onRefresh = {
              webViewRef?.reload()
            },
            onHome = {
              webViewRef?.loadUrl("about:blank")
            },
            onMenu = {
              Toast.makeText(context, "Menu coming soon", Toast.LENGTH_SHORT).show()
            }
        )
      },
      floatingActionButton = {
          FloatingActionButton(onClick = { showLogViewer = true }) {
              Icon(Icons.Default.Info, contentDescription = "View Logs")
          }
      }
  ) { innerPadding ->
      Box(modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)) {
          
          BrowserWebView(
              onWebViewCreated = { webViewRef = it },
              onPageStarted = { url ->
                  currentUrl = if (url == "about:blank") "" else url
                  VianbrowLogger.i("WebView", "WebView: loading [$url]")
              },
              onPageFinished = { url ->
                  currentUrl = if (url == "about:blank") "" else url
                  VianbrowLogger.i("WebView", "WebView: loaded [$url]")
              },
              onReceivedError = { url, description ->
                  VianbrowLogger.e("WebView", "WebView: failed [$url] error:[$description]")
              }
          )
      }
  }

  if (showLogViewer) {
      LogViewerDialog(onDismiss = { showLogViewer = false })
  }
}

@Composable
fun AddressBar(
    url: String,
    onNavigate: (String) -> Unit
) {
    var text by remember(url) { mutableStateOf(url) }
    val focusManager = LocalFocusManager.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go
            ),
            keyboardActions = KeyboardActions(
                onGo = {
                    focusManager.clearFocus()
                    var finalUrl = text
                    if (finalUrl.isNotBlank() && !finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                        finalUrl = "https://$finalUrl"
                    }
                    onNavigate(finalUrl)
                }
            ),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.DarkGray,
                unfocusedContainerColor = Color.DarkGray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun BottomNavBar(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onHome: () -> Unit,
    onMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        IconButton(onClick = onForward) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward", tint = Color.White)
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
        }
        IconButton(onClick = onHome) {
            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
        }
        IconButton(onClick = onMenu) {
            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    modifier: Modifier = Modifier,
    initialUrl: String = "about:blank",
    onWebViewCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onReceivedError: (String, String) -> Unit
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onPageStarted(it) }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        url?.let { onPageFinished(it) }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val errorUrl = request.url?.toString() ?: ""
                            val description = error?.description?.toString() ?: "Unknown error"
                            onReceivedError(errorUrl, description)
                        }
                    }
                }
                onWebViewCreated(this)
                loadUrl(initialUrl)
            }
        },
        update = { webView ->
            // Keep reference updated if needed
        }
    )
}
