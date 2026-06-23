package com.vianbrow.browser
// debug-v15

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
import androidx.compose.ui.input.pointer.pointerInput
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

data class Tab(
    val id: Int,
    var url: String = "",
    var title: String = ""
)

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
    var tabs by remember { mutableStateOf(listOf(Tab(id = 0))) }
    var activeTabId by remember { mutableStateOf(0) }
    val currentUrl = tabs.find { it.id == activeTabId }?.url ?: ""
    val pageTitle = tabs.find { it.id == activeTabId }?.title ?: ""
    var isLoading by remember { mutableStateOf(false) }
    
    var showSitePanel by remember { mutableStateOf(false) }
    var showSiteConfig by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    
    val webViews = remember { mutableMapOf<Int, WebView>() }

    fun switchToTab(newTabId: Int) {
        if (newTabId == activeTabId) return
        activeTabId = newTabId
        VianbrowLogger.i("Tabs", "Tabs: switched to tab [$newTabId]")
    }
    
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    val qrScanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanned = result.data?.getStringExtra("SCAN_RESULT")
            scanned?.let {
                webViews[activeTabId]?.loadUrl(processUrlInput(it, context))
                VianbrowLogger.i("QR", "QR: scanned [$scanned]")
            }
        }
    }

    BackHandler {
        if (showSiteConfig) {
            showSiteConfig = false
        } else if (showSitePanel) {
            showSitePanel = false
        } else if (webViews[activeTabId]?.canGoBack() == true) {
            webViews[activeTabId]?.goBack()
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
                        webViews[activeTabId]?.loadUrl(url)
                    },
                    onStopLoading = {
                        webViews[activeTabId]?.stopLoading()
                    },
                    onReload = {
                        webViews[activeTabId]?.reload()
                    },
                    onSitePanelClick = {
                        showSitePanel = true
                        VianbrowLogger.i("SitePanel", "SitePanel: opened for [$currentUrl]")
                    },
                    onQrScan = {
                        try {
                            val intent = android.content.Intent("com.google.zxing.client.android.SCAN")
                            intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
                            qrScanLauncher.launch(intent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            Toast.makeText(context, "No QR scanner app found", Toast.LENGTH_SHORT).show()
                        }
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
                tabCount = tabs.size,
                onBack = {
                    if (webViews[activeTabId]?.canGoBack() == true) {
                        webViews[activeTabId]?.goBack()
                    } else {
                        VianbrowLogger.i("WebView", "WebView: no back history")
                    }
                },
                onForward = {
                    if (webViews[activeTabId]?.canGoForward() == true) {
                        webViews[activeTabId]?.goForward()
                    }
                },
                onHome = {
                    webViews[activeTabId]?.loadUrl("about:blank")
                },
                onTabCounter = { showTabSwitcher = true },
                onMenu = { showMenu = true }
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
            tabs.forEach { tab ->
                BrowserWebView(
                    modifier = if (tab.id == activeTabId)
                        Modifier.fillMaxSize()
                    else
                        Modifier.size(0.dp),
                    initialUrl = "about:blank",
                    onWebViewCreated = { webView ->
                        webViews[tab.id] = webView
                    },
                    onPageStarted = { url ->
                        if (tab.id == activeTabId) {
                            tabs = tabs.map { if (it.id == activeTabId) it.copy(url = if (url == "about:blank") "" else url) else it }
                            isLoading = true
                            VianbrowLogger.i("WebView", "WebView: loading [$url]")
                        }
                    },
                    onPageFinished = { url ->
                        if (tab.id == activeTabId) {
                            tabs = tabs.map { if (it.id == activeTabId) it.copy(url = if (url == "about:blank") "" else url) else it }
                            isLoading = false
                            VianbrowLogger.i("WebView", "WebView: loaded [$url]")
                        }
                    },
                    onTitleChanged = { title ->
                        tabs = tabs.map { if (it.id == tab.id) it.copy(title = title) else it }
                    },
                    onProgressChanged = { progress ->
                        if (tab.id == activeTabId && progress == 100) isLoading = false
                    },
                    onReceivedError = { url, description ->
                        if (tab.id == activeTabId) {
                            VianbrowLogger.e("WebView", "WebView: failed [$url] error:[$description]")
                            isLoading = false
                        }
                    }
                )
            }
        }
    }

    if (showLogViewer) {
        LogViewerDialog(onDismiss = { showLogViewer = false })
    }

    if (showTabSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showTabSwitcher = false },
            containerColor = Color(0xFF1A1A1A)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tabs", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        val newId = (tabs.maxOfOrNull { it.id } ?: 0) + 1
                        tabs = tabs + Tab(id = newId)
                        activeTabId = newId
                        showTabSwitcher = false
                        VianbrowLogger.i("Tabs", "Tabs: new tab created [$newId]")
                    }) {
                        Text("+ New Tab", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                tabs.forEach { tab ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showTabSwitcher = false
                                switchToTab(tab.id)
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                tab.title.ifBlank { "New Tab" },
                                color = if (tab.id == activeTabId) Color(0xFF4CAF50) else Color.White,
                                fontSize = 14.sp,
                                fontWeight = if (tab.id == activeTabId) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                tab.url.ifBlank { "about:blank" },
                                color = Color.Gray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (tabs.size > 1) {
                            IconButton(onClick = {
                                webViews[tab.id]?.destroy()
                                webViews.remove(tab.id)
                                tabs = tabs.filter { it.id != tab.id }
                                if (tab.id == activeTabId) {
                                    activeTabId = tabs.last().id
                                }
                                VianbrowLogger.i("Tabs", "Tabs: closed tab [${tab.id}]")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close tab", tint = Color.Gray)
                            }
                        }
                    }
                    HorizontalDivider(color = Color.DarkGray)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showMenu) {
        MenuBottomSheet(
            onDismiss = { showMenu = false },
            onSettings = {
                showMenu = false
                activity?.startActivity(android.content.Intent(activity, SettingsActivity::class.java))
            },
            onClearCache = {
                showMenu = false
                webViews[activeTabId]?.clearCache(true)
                Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                VianbrowLogger.i("Menu", "Menu: cache cleared")
            },
            onToggleDevMode = {
                val prefs = context.getSharedPreferences("vianbrow_settings", android.content.Context.MODE_PRIVATE)
                val current = prefs.getBoolean("setting_dev_mode", false)
                prefs.edit().putBoolean("setting_dev_mode", !current).apply()
                showMenu = false
                Toast.makeText(context, if (!current) "Dev Mode ON" else "Dev Mode OFF", Toast.LENGTH_SHORT).show()
                VianbrowLogger.i("Menu", "Menu: dev mode toggled to ${!current}")
            },
            onShare = {
                showMenu = false
                if (currentUrl.isNotBlank()) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, currentUrl)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Share URL"))
                }
                VianbrowLogger.i("Menu", "Menu: share tapped for [$currentUrl]")
            }
        )
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
                        webViews[activeTabId]?.clearCache(true)
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

fun processUrlInput(input: String, context: android.content.Context): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "about:blank"
    
    // Already a full URL
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    
    // Looks like a URL (contains a dot and no spaces)
    if (trimmed.contains(".") && !trimmed.contains(" ")) {
        return "https://$trimmed"
    }
    
    // Treat as search query
    val prefs = context.getSharedPreferences("vianbrow_settings", android.content.Context.MODE_PRIVATE)
    val engine = prefs.getString("setting_search_engine", "google")
    val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
    return when (engine) {
        "duckduckgo" -> "https://duckduckgo.com/?q=$encoded"
        "bing" -> "https://www.bing.com/search?q=$encoded"
        else -> "https://www.google.com/search?q=$encoded"
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
    onSitePanelClick: () -> Unit,
    onQrScan: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember(isEditing) { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    LaunchedEffect(isEditing) {
        if (isEditing) text = androidx.compose.ui.text.input.TextFieldValue(url, selection = androidx.compose.ui.text.TextRange(0, url.length))
    }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onSitePanelClick) {
            val icon = when {
                url.isBlank() || url == "about:blank" -> Icons.Default.Search
                url.startsWith("https://") -> Icons.Default.CheckCircle
                else -> Icons.Default.Warning
            }
            Icon(
                icon,
                contentDescription = "Site Info",
                tint = when {
                    url.startsWith("https://") -> Color(0xFF4CAF50)
                    url.isBlank() || url == "about:blank" -> Color.LightGray
                    else -> Color(0xFFFFA000)
                }
            )
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
                            val finalUrl = processUrlInput(text.text, context)
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
        
        when {
            isLoading -> {
                IconButton(onClick = onStopLoading) {
                    Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color.LightGray)
                }
            }
            isEditing -> {
                IconButton(onClick = { text = androidx.compose.ui.text.input.TextFieldValue("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.LightGray)
                }
            }
            url.isBlank() || url == "about:blank" -> {
                IconButton(onClick = onQrScan) {
                    Icon(Icons.Default.Search, contentDescription = "Scan QR", tint = Color.LightGray)
                }
            }
            else -> {
                IconButton(onClick = onReload) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reload", tint = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabCounter: () -> Unit,
    onMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    Text(tabCount.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onMenu) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
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
                settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    settings.safeBrowsingEnabled = true
                }
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.allowFileAccessFromFileURLs = false
                settings.allowUniversalAccessFromFileURLs = false
                settings.databaseEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(true)
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                }
                settings.mediaPlaybackRequiresUserGesture = false
                android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                VianbrowLogger.i("WebView", "WebView: settings applied v2")
                
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
                        val gmJs = """
                            (function() {
                              window.GM_setValue = function(k,v){ localStorage.setItem(k, JSON.stringify(v)); };
                              window.GM_getValue = function(k,d){ var v=localStorage.getItem(k); return v!==null?JSON.parse(v):d; };
                              window.GM_xmlhttpRequest = function(d){ fetch(d.url,{method:d.method||'GET',headers:d.headers||{}}).then(r=>r.text()).then(t=>d.onload&&d.onload({responseText:t})).catch(e=>d.onerror&&d.onerror(e)); };
                              window.GM_setClipboard = function(t){ navigator.clipboard&&navigator.clipboard.writeText(t); };
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(gmJs, null)
                        VianbrowLogger.i("Scripts", "Scripts: GM polyfill injected")
                        url?.let { onPageStarted(it) }
                    }
                    
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        swipeRefreshLayout.isRefreshing = false
                        url?.let {
                            onPageFinished(it)
                            val txtJs = """
                                (function() {
                                  if (document.getElementById('vianbrow-save-txt')) return;
                                  var btn = document.createElement('button');
                                  btn.id = 'vianbrow-save-txt';
                                  btn.innerText = 'TXT';
                                  btn.style.cssText = 'position:fixed;bottom:80px;left:8px;z-index:99999;background:#222;color:#fff;border:none;padding:6px 10px;border-radius:6px;font-size:13px;opacity:0.85;';
                                  btn.onclick = function() {
                                    var text = document.body.innerText;
                                    var title = document.title.replace(/[^a-z0-9]/gi,'_').toLowerCase();
                                    var a = document.createElement('a');
                                    a.href = 'data:text/plain;charset=utf-8,' + encodeURIComponent(text);
                                    a.download = title + '.txt';
                                    a.click();
                                  };
                                  document.body.appendChild(btn);
                                })();
                            """.trimIndent()
                            view?.evaluateJavascript(txtJs, null)
                            VianbrowLogger.i("Scripts", "Scripts: Save as TXT injected")
                            val prefs = context.getSharedPreferences("vianbrow_settings", android.content.Context.MODE_PRIVATE)
                            if (prefs.getBoolean("setting_dev_mode", false)) {
                                val js = """
                                    (function() {
                                      var script = document.createElement('script');
                                      script.src = 'https://cdn.jsdelivr.net/npm/eruda';
                                      script.onload = function() { eruda.init(); eruda.show(); };
                                      script.onerror = function() { console.error('Eruda CDN failed'); };
                                      document.head.appendChild(script);
                                    })();
                                """.trimIndent()
                                view?.evaluateJavascript(js, null)
                                VianbrowLogger.i("DevTools", "DevTools: Eruda injected for [$it]")
                            }
                        }
                    }

                    override fun onReceivedSslError(view: WebView?, handler: android.webkit.SslErrorHandler?, error: android.net.http.SslError?) {
                        handler?.cancel()
                        VianbrowLogger.e("WebView", "WebView: SSL error cancelled for [${error?.url}]")
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val errorUrl = request.url?.toString() ?: ""
                            val description = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                error?.description?.toString() ?: "Unknown error"
                            } else {
                                "Unknown error"
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuBottomSheet(
    onDismiss: () -> Unit,
    onSettings: () -> Unit,
    onClearCache: () -> Unit,
    onToggleDevMode: () -> Unit,
    onShare: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Menu",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuGridItem(icon = Icons.Default.Settings, label = "Settings", onClick = onSettings)
                MenuGridItem(icon = Icons.Default.Delete, label = "Clear Cache", onClick = onClearCache)
                MenuGridItem(icon = Icons.Default.Build, label = "Dev Mode", onClick = onToggleDevMode)
                MenuGridItem(icon = Icons.Default.Share, label = "Share", onClick = onShare)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MenuGridItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, color = Color.Gray, fontSize = 11.sp)
    }
}

