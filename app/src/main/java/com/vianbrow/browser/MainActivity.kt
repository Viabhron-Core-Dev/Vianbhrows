package com.vianbrow.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebChromeClient
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var showLogViewer by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    var showSitePanel by remember { mutableStateOf(false) }
    var showSiteConfig by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    BackHandler {
        if (showSiteConfig) {
            showSiteConfig = false
        } else if (showSitePanel) {
            showSitePanel = false
        } else if (webViewRef?.canGoBack() == true) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                AddressBar(
                    url = currentUrl,
                    title = pageTitle,
                    isLoading = isLoading,
                    onNavigate = { url ->
                        webViewRef?.loadUrl(url)
                    },
                    onStopLoading = {
                        webViewRef?.stopLoading()
                    },
                    onReload = {
                        webViewRef?.reload()
                    },
                    onSitePanelClick = {
                        showSitePanel = true
                        VianbrowLogger.i("SitePanel", "SitePanel: opened for [$currentUrl]")
                    }
                )
                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.DarkGray
                    )
                }
            }
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
                onHome = {
                    webViewRef?.loadUrl("about:blank")
                },
                onTabCounter = {
                    Toast.makeText(context, "Tab manager coming soon", Toast.LENGTH_SHORT).show()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BrowserWebView(
                onWebViewCreated = { webViewRef = it },
                onPageStarted = { url ->
                    currentUrl = if (url == "about:blank") "" else url
                    pageTitle = ""
                    isLoading = true
                    VianbrowLogger.i("WebView", "WebView: loading [$url]")
                },
                onPageFinished = { url ->
                    currentUrl = if (url == "about:blank") "" else url
                    isLoading = false
                    VianbrowLogger.i("WebView", "WebView: loaded [$url]")
                },
                onTitleChanged = { title ->
                    pageTitle = title
                },
                onProgressChanged = { progress ->
                    if (progress == 100) {
                        isLoading = false
                    }
                },
                onReceivedError = { url, description ->
                    VianbrowLogger.e("WebView", "WebView: failed [$url] error:[$description]")
                    isLoading = false
                }
            )
        }
    }

    if (showLogViewer) {
        LogViewerDialog(onDismiss = { showLogViewer = false })
    }

    if (showSitePanel) {
        ModalBottomSheet(
            onDismissRequest = { 
                showSitePanel = false 
                showSiteConfig = false
            },
            containerColor = Color.DarkGray
        ) {
            if (showSiteConfig) {
                SiteConfigContent(
                    url = currentUrl,
                    onClearCache = {
                        webViewRef?.clearCache(true)
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        VianbrowLogger.i("SitePanel", "SitePanel: cache cleared for [$currentUrl]")
                    },
                    onPermissions = {
                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onBack = { showSiteConfig = false }
                )
            } else {
                SitePanelContent(
                    url = currentUrl,
                    title = pageTitle,
                    onViewCookies = {
                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    },
                    onSiteConfig = {
                        showSiteConfig = true
                    },
                    onViewScripts = {
                        Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun AddressBar(
    url: String,
    title: String,
    isLoading: Boolean,
    onNavigate: (String) -> Unit,
    onStopLoading: () -> Unit,
    onReload: () -> Unit,
    onSitePanelClick: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember(isEditing, url) { mutableStateOf(if (isEditing) url else (title.takeIf { it.isNotBlank() } ?: url)) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSitePanelClick) {
            val icon = if (url.startsWith("https")) Icons.Default.Lock else if (url.isBlank() || url == "about:blank") Icons.Default.Info else Icons.Default.Warning
            Icon(icon, contentDescription = "Site Info", tint = Color.LightGray)
        }
        
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
                .clickable {
                    isEditing = true
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (isEditing) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            focusManager.clearFocus()
                            isEditing = false
                            var finalUrl = text
                            if (finalUrl.isNotBlank() && !finalUrl.startsWith("http://") && !finalUrl.startsWith("https://")) {
                                finalUrl = "https://$finalUrl"
                            }
                            onNavigate(finalUrl)
                        }
                    ),
                    cursorBrush = SolidColor(Color.White)
                )
            } else {
                val displayText = if (title.isNotBlank()) title else if (url.isNotBlank() && url != "about:blank") url else ""
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (isLoading || isEditing) {
            IconButton(onClick = {
                if (isEditing) {
                    text = ""
                } else {
                    onStopLoading()
                }
            }) {
                Icon(Icons.Default.Close, contentDescription = "Stop or Clear", tint = Color.LightGray)
            }
        } else if (url.isNotBlank() && url != "about:blank") {
            IconButton(onClick = onReload) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.LightGray)
            }
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
fun BottomNavBar(
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabCounter: () -> Unit,
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
        IconButton(onClick = onHome) {
            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
        }
        IconButton(onClick = onTabCounter) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        IconButton(onClick = onMenu) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
        }
    }
}

@Composable
fun SitePanelContent(url: String, title: String, onViewCookies: () -> Unit, onSiteConfig: () -> Unit, onViewScripts: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        if (url.isBlank() || url == "about:blank") {
            Text("No page loaded", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Text(title.ifBlank { url }, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(url, color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(modifier = Modifier.height(8.dp))
            
            ListItem(
                headlineContent = { Text("View Cookies", color = Color.White) },
                modifier = Modifier.clickable { onViewCookies() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text("Site Configuration", color = Color.White) },
                modifier = Modifier.clickable { onSiteConfig() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            ListItem(
                headlineContent = { Text("View Scripts", color = Color.White) },
                modifier = Modifier.clickable { onViewScripts() },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SiteConfigContent(url: String, onClearCache: () -> Unit, onPermissions: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Site Configuration", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color.DarkGray)
        Spacer(modifier = Modifier.height(8.dp))
        
        ListItem(
            headlineContent = { Text("Clear Cache", color = Color.White) },
            modifier = Modifier.clickable { onClearCache() },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        ListItem(
            headlineContent = { Text("Permissions", color = Color.White) },
            modifier = Modifier.clickable { onPermissions() },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        Spacer(modifier = Modifier.height(24.dp))
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
    onTitleChanged: (String) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onReceivedError: (String, String) -> Unit
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val swipeRefreshLayout = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(context).apply {
                setColorSchemeColors(android.graphics.Color.WHITE)
                setProgressBackgroundColorSchemeColor(android.graphics.Color.DKGRAY)
            }
            val webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                
                webChromeClient = object : WebChromeClient() {
                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { onPageStarted(it) }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        swipeRefreshLayout.isRefreshing = false
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
            swipeRefreshLayout.addView(webView)
            swipeRefreshLayout.setOnRefreshListener {
                VianbrowLogger.i("WebView", "WebView: pull to refresh")
                webView.reload()
            }
            swipeRefreshLayout
        },
        update = { webView ->
            // Keep reference updated if needed
        }
    )
}

