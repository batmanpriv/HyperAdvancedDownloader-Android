package com.had.downloader.ui.screens

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import com.had.downloader.service.InterceptedRequest
import com.had.downloader.service.LinkType
import com.had.downloader.service.ScraperEngine
import com.had.downloader.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val quickLinks = listOf(
    Triple("Google", "https://www.google.com", Icons.Outlined.Search),
    Triple("YouTube", "https://m.youtube.com", Icons.Outlined.OndemandVideo),
    Triple("GitHub", "https://github.com", Icons.Outlined.Code),
    Triple("Archive", "https://archive.org", Icons.Outlined.Archive),
    Triple("Wikipedia", "https://en.m.wikipedia.org", Icons.Outlined.MenuBook),
    Triple("Reddit", "https://old.reddit.com", Icons.Outlined.Forum)
)

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserTabScreen(vm: MainViewModel) {
    val state by vm.browserUiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val activeTab = state.tabs.find { it.id == state.activeTabId }
        ?: state.tabs.firstOrNull() ?: BrowserTab()

    val webViews = remember { mutableStateMapOf<String, WebView>() }

    var addressText by remember(activeTab.id, activeTab.url) {
        mutableStateOf(TextFieldValue(activeTab.url))
    }

    var incognitoMode by remember { mutableStateOf(false) }
    var desktopMode by remember { mutableStateOf(false) }

    val interceptedCount = activeTab.interceptedRequests.size

    DisposableEffect(Unit) {
        onDispose {
            webViews.values.forEach { it.destroy() }
            webViews.clear()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {

            BrowserTopBar(
                tab = activeTab,
                tabCount = state.tabs.size,
                addressText = addressText,
                incognito = incognitoMode,
                interceptedCount = interceptedCount,
                onAddressChange = { addressText = it },
                onNavigate = { url ->
                    val resolved = resolveUrl(url)
                    addressText = TextFieldValue(resolved)
                    webViews[activeTab.id]?.loadUrl(resolved)
                    vm.updateBrowserTab(activeTab.id) { it.copy(url = resolved, isLoading = true) }
                },
                onBack = { webViews[activeTab.id]?.goBack() },
                onForward = { webViews[activeTab.id]?.goForward() },
                onRefresh = { webViews[activeTab.id]?.reload() },
                onStop = { webViews[activeTab.id]?.stopLoading() },
                onNewTab = { vm.addBrowserTab() },
                onShowTabs = { vm.toggleTabSwitcher() },
                onShowIntercepted = { vm.toggleInterceptedPanel() },
                onToggleIncognito = { incognitoMode = !incognitoMode },
                onToggleDesktop = {
                    desktopMode = !desktopMode
                    val ua = if (desktopMode)
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/124.0.0.0 Safari/537.36"
                    else
                        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"
                    webViews[activeTab.id]?.settings?.userAgentString = ua
                    webViews[activeTab.id]?.reload()
                },
                onPasteAndGo = { url ->
                    if (url.isNotBlank()) {
                        val resolved = resolveUrl(url)
                        addressText = TextFieldValue(resolved)
                        webViews[activeTab.id]?.loadUrl(resolved)
                        vm.updateBrowserTab(activeTab.id) { it.copy(url = resolved, isLoading = true) }
                    }
                }
            )

            if (activeTab.isLoading) {
                LinearProgressIndicator(
                    progress = { activeTab.progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = if (incognitoMode) PurpleAccent else CyanPrimary,
                    trackColor = Color.Transparent
                )
            }

            if (incognitoMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PurpleAccent.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.VisibilityOff, null, tint = PurpleAccent, modifier = Modifier.size(14.dp))
                    Text("Incognito · History & cookies not saved", color = PurpleAccent, fontSize = 11.sp)
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                state.tabs.forEach { tab ->
                    key(tab.id) {
                        val scraperEngine = remember { ScraperEngine() }
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (tab.id == activeTab.id) 1f else 0f),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setupWebView(
                                        scraperEngine = scraperEngine,
                                        incognito = incognitoMode,
                                        onPageStarted = { url, _ ->
                                            vm.updateBrowserTab(tab.id) {
                                                it.copy(
                                                    url = url ?: it.url,
                                                    isLoading = true,
                                                    progress = 0.1f,
                                                    canGoBack = canGoBack(),
                                                    canGoFwd = canGoForward()
                                                )
                                            }
                                            if (tab.id == activeTab.id)
                                                addressText = TextFieldValue(url ?: "")
                                        },
                                        onPageFinished = { url ->
                                            vm.updateBrowserTab(tab.id) {
                                                it.copy(
                                                    title = this.title ?: it.title,
                                                    url = url ?: it.url,
                                                    isLoading = false,
                                                    progress = 1f,
                                                    canGoBack = canGoBack(),
                                                    canGoFwd = canGoForward()
                                                )
                                            }
                                        },
                                        onProgress = { p ->
                                            vm.updateBrowserTab(tab.id) { it.copy(progress = p / 100f) }
                                        },
                                        onIntercept = { req ->
                                            vm.addInterceptedRequest(tab.id, req)
                                        },
                                        onTitleReceived = { title ->
                                            vm.updateBrowserTab(tab.id) { it.copy(title = title) }
                                        }
                                    )
                                    webViews[tab.id] = this
                                    if (tab.url.isNotBlank() && tab.url != "about:blank")
                                        loadUrl(tab.url)
                                }
                            },
                            update = {}
                        )
                    }
                }

                if (activeTab.url.isBlank() || activeTab.url == "about:blank") {
                    BrowserHomePage(
                        interceptedCount = interceptedCount,
                        onNavigate = { url ->
                            val resolved = resolveUrl(url)
                            webViews[activeTab.id]?.loadUrl(resolved)
                            vm.updateBrowserTab(activeTab.id) {
                                it.copy(url = resolved, isLoading = true)
                            }
                            addressText = TextFieldValue(resolved)
                        },
                        onShowIntercepted = { vm.toggleInterceptedPanel() }
                    )
                }

                if (interceptedCount > 0 && !state.showIntercepted) {
                    InterceptedFloatingBadge(
                        count = interceptedCount,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 72.dp),
                        onClick = { vm.toggleInterceptedPanel() }
                    )
                }
            }

            BrowserBottomBar(
                tab = activeTab,
                interceptedCount = interceptedCount,
                onBack = { webViews[activeTab.id]?.goBack() },
                onForward = { webViews[activeTab.id]?.goForward() },
                onHome = {
                    webViews[activeTab.id]?.loadUrl("about:blank")
                    vm.updateBrowserTab(activeTab.id) { it.copy(url = "", title = "New Tab") }
                    addressText = TextFieldValue("")
                },
                onShowIntercepted = { vm.toggleInterceptedPanel() },
                onShare = {}
            )
        }

        if (state.showTabSwitcher) {
            TabSwitcherOverlay(
                tabs = state.tabs,
                activeTabId = activeTab.id,
                onSelectTab = { tabId ->
                    vm.switchBrowserTab(tabId)
                    val newTab = state.tabs.find { it.id == tabId }
                    addressText = TextFieldValue(newTab?.url ?: "")
                },
                onCloseTab = { tabId ->
                    webViews[tabId]?.destroy(); webViews.remove(tabId)
                    vm.closeBrowserTab(tabId)
                },
                onNewTab = { vm.addBrowserTab() },
                onDismiss = { vm.toggleTabSwitcher() }
            )
        }

        if (state.showIntercepted) {
            InterceptedRequestsPanel(
                requests = activeTab.interceptedRequests,
                onDownload = { req -> vm.downloadIntercepted(req) },
                onDownloadAll = { requests -> requests.forEach { vm.downloadIntercepted(it) } },
                onCopyLink = { url ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("HAD Link", url))
                },
                onClear = { vm.clearIntercepted(activeTab.id) },
                onDismiss = { vm.toggleInterceptedPanel() }
            )
        }
    }
}

@Composable
private fun InterceptedFloatingBadge(count: Int, modifier: Modifier, onClick: () -> Unit) {
    val pulse by rememberInfiniteTransition(label = "ibadge").animateFloat(
        initialValue = 0.75f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ibadgep"
    )
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(8.dp, CircleShape)
            .background(
                Brush.linearGradient(listOf(PurpleAccent, CyanPrimary)),
                CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Download, null, tint = SpaceBlack, modifier = Modifier.size(24.dp))
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .background(RedError.copy(alpha = pulse), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (count > 9) "9+" else "$count",
                color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Black
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(
    scraperEngine: ScraperEngine,
    incognito: Boolean,
    onPageStarted: WebView.(String?, Bitmap?) -> Unit,
    onPageFinished: WebView.(String?) -> Unit,
    onProgress: (Int) -> Unit,
    onIntercept: (InterceptedRequest) -> Unit,
    onTitleReceived: (String) -> Unit
) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = !incognito
        loadWithOverviewMode = true
        useWideViewPort = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        allowFileAccess = false
        mediaPlaybackRequiresUserGesture = false
        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = if (incognito) WebSettings.LOAD_NO_CACHE else WebSettings.LOAD_DEFAULT
        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        if (incognito) {
            setGeolocationEnabled(false)
            @Suppress("DEPRECATION")
            saveFormData = false
        }
    }

    if (incognito) {
        CookieManager.getInstance().setAcceptCookie(false)
        clearCache(true)
        clearHistory()
    } else {
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
    }

    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            view.onPageStarted(url, favicon)
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            view.onPageFinished(url)
            view.evaluateJavascript(buildJsInterceptor(), null)
        }

        override fun shouldInterceptRequest(
            view: WebView,
            request: WebResourceRequest
        ): WebResourceResponse? {
            val url = request.url.toString()
            val headers = request.requestHeaders ?: emptyMap()
            val method = request.method ?: "GET"
            val linkType = scraperEngine.classifyUrl(url)

            val isDownloadable = linkType != LinkType.OTHER &&
                    linkType != LinkType.CODE &&
                    linkType != LinkType.IMAGE
            if (isDownloadable || isMediaExtension(url)) {
                val cookies = CookieManager.getInstance().getCookie(url)
                val intercepted = InterceptedRequest(
                    url = url,
                    headers = headers,
                    method = method,
                    referer = headers["Referer"],
                    cookies = cookies,
                    userAgent = headers["User-Agent"],
                    contentType = null,
                    contentLength = -1L,
                    linkType = linkType
                )
                onIntercept(intercepted)
            }
            return null
        }

        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            handler.proceed()
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) { onProgress(newProgress) }
        override fun onReceivedTitle(view: WebView, title: String?) {
            title?.let { onTitleReceived(it) }
        }
    }

    addJavascriptInterface(object {
        @android.webkit.JavascriptInterface
        fun intercept(url: String) {
            if (url.isNotBlank() && isMediaExtension(url)) {
                val linkType = scraperEngine.classifyUrl(url)
                onIntercept(
                    InterceptedRequest(
                        url = url,
                        headers = emptyMap(),
                        method = "GET",
                        referer = null,
                        cookies = CookieManager.getInstance().getCookie(url),
                        userAgent = null,
                        contentType = null,
                        contentLength = -1L,
                        linkType = linkType
                    )
                )
            }
        }
    }, "HAD")
}

private fun buildJsInterceptor(): String = """
(function() {
  if (window.__had_injected) return;
  window.__had_injected = true;

  function reportUrl(url) {
    if (!url || typeof url !== 'string') return;
    if (!isMedia(url)) return;
    try { window.HAD.intercept(url); } catch(e) {}
  }

  var origXhrOpen = XMLHttpRequest.prototype.open;
  XMLHttpRequest.prototype.open = function(method, url) {
    reportUrl(url);
    return origXhrOpen.apply(this, arguments);
  };

  var origFetch = window.fetch;
  window.fetch = function(input) {
    var url = typeof input === 'string' ? input : (input && input.url) || '';
    reportUrl(url);
    return origFetch.apply(this, arguments);
  };

  document.querySelectorAll('video, audio, source').forEach(function(el) {
    if (el.src) reportUrl(el.src);
    if (el.currentSrc) reportUrl(el.currentSrc);
  });

  var observer = new MutationObserver(function(muts) {
    muts.forEach(function(m) {
      m.addedNodes.forEach(function(n) {
        if (n.tagName === 'VIDEO' || n.tagName === 'AUDIO' || n.tagName === 'SOURCE') {
          if (n.src) reportUrl(n.src);
        }
        if (n.querySelectorAll) {
          n.querySelectorAll('video, audio, source').forEach(function(el) {
            if (el.src) reportUrl(el.src);
          });
        }
      });
    });
  });
  observer.observe(document.body || document.documentElement, { childList: true, subtree: true });

  function isMedia(url) {
    return /\.(mp4|mkv|webm|m3u8|mpd|mp3|flac|aac|m4a|zip|rar|7z|pdf|apk|torrent|ts|avi|mov|ogg|opus|epub|iso|dmg|exe|deb)(\?|#|$)/i.test(url)
      || url.includes('.m3u8') || url.includes('.mpd') || url.includes('download') || url.includes('stream');
  }
})();
""".trimIndent()

private fun isMediaExtension(url: String): Boolean {
    val clean = url.substringBefore('?').substringBefore('#')
    val ext = clean.substringAfterLast('.').lowercase()
    return ext in setOf(
        "mp4", "mkv", "webm", "avi", "mov", "m3u8", "mpd", "ts", "m2ts",
        "mp3", "flac", "aac", "ogg", "m4a", "opus",
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz",
        "apk", "pdf", "epub", "torrent", "exe", "deb", "iso", "dmg"
    )
}

private fun resolveUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        trimmed.contains('.') && !trimmed.contains(' ') -> "https://$trimmed"
        else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserTopBar(
    tab: BrowserTab,
    tabCount: Int,
    addressText: TextFieldValue,
    incognito: Boolean,
    interceptedCount: Int,
    onAddressChange: (TextFieldValue) -> Unit,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onNewTab: () -> Unit,
    onShowTabs: () -> Unit,
    onShowIntercepted: () -> Unit,
    onToggleIncognito: () -> Unit,
    onToggleDesktop: () -> Unit,
    onPasteAndGo: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        color = if (incognito) Color(0xFF12082A) else SurfaceDark,
        shadowElevation = 4.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onBack, enabled = tab.canGoBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack, null,
                        tint = if (tab.canGoBack) CyanPrimary else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = addressText,
                    onValueChange = onAddressChange,
                    modifier = Modifier.weight(1f).height(46.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            if (incognito) Icons.Outlined.VisibilityOff
                            else if (tab.url.startsWith("https://")) Icons.Outlined.Lock
                            else Icons.Outlined.Language,
                            null,
                            modifier = Modifier.size(14.dp),
                            tint = when {
                                incognito -> PurpleAccent
                                tab.url.startsWith("https://") -> GreenSuccess
                                else -> TextMuted
                            }
                        )
                    },
                    trailingIcon = {
                        if (tab.isLoading) {
                            IconButton(onClick = onStop, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Filled.Close, null, tint = OrangeWarn, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            IconButton(onClick = onRefresh, modifier = Modifier.size(30.dp)) {
                                Icon(Icons.Filled.Refresh, null, tint = CyanPrimary, modifier = Modifier.size(14.dp))
                            }
                        }
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Go
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onGo = { onNavigate(addressText.text) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (incognito) PurpleAccent else CyanPrimary,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary,
                        unfocusedContainerColor = ElevatedSurf,
                        focusedContainerColor = ElevatedSurf,
                        cursorColor = CyanPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    placeholder = { Text("Search or URL...", color = TextMuted, fontSize = 11.sp) }
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onShowTabs() }
                        .background(ElevatedSurf, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$tabCount",
                        color = CyanPrimary, fontSize = 13.sp,
                        fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.MoreVert, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(SurfaceDark)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Detected Files", color = PurpleAccent, fontSize = 13.sp)
                                    if (interceptedCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .background(PurpleAccent.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("$interceptedCount", color = PurpleAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            },
                            leadingIcon = { Icon(Icons.Outlined.Download, null, tint = PurpleAccent, modifier = Modifier.size(16.dp)) },
                            onClick = { showMenu = false; onShowIntercepted() }
                        )
                        DropdownMenuItem(
                            text = { Text("Paste & Go", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Outlined.ContentPaste, null, tint = CyanPrimary, modifier = Modifier.size(16.dp)) },
                            onClick = {
                                showMenu = false
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                                onPasteAndGo(text)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (incognito) "Exit Incognito" else "Incognito", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Outlined.VisibilityOff, null, tint = if (incognito) PurpleAccent else TextMuted, modifier = Modifier.size(16.dp)) },
                            onClick = { showMenu = false; onToggleIncognito() }
                        )
                        DropdownMenuItem(
                            text = { Text("Desktop Site", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Outlined.DesktopWindows, null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                            onClick = { showMenu = false; onToggleDesktop() }
                        )
                        DropdownMenuItem(
                            text = { Text("New Tab", color = TextPrimary, fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Filled.Add, null, tint = TextMuted, modifier = Modifier.size(16.dp)) },
                            onClick = { showMenu = false; onNewTab() }
                        )
                    }
                }
            }

            if (tab.title.isNotBlank() && tab.title != "New Tab" && tab.url.isNotBlank()) {
                Text(
                    tab.title,
                    color = TextMuted, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BrowserBottomBar(
    tab: BrowserTab,
    interceptedCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onShowIntercepted: () -> Unit,
    onShare: () -> Unit
) {
    Surface(color = SurfaceDark, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BotBtn(Icons.Filled.ArrowBack, tab.canGoBack, onBack)
            BotBtn(Icons.Filled.ArrowForward, tab.canGoFwd, onForward)
            BotBtn(Icons.Outlined.Home, true, onHome)
            Box {
                BotBtn(Icons.Outlined.Download, true, onShowIntercepted,
                    tint = if (interceptedCount > 0) PurpleAccent else TextMuted)
                if (interceptedCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(18.dp)
                            .background(PurpleAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (interceptedCount > 9) "9+" else "$interceptedCount",
                            color = SpaceBlack, fontSize = 8.sp, fontWeight = FontWeight.Black
                        )
                    }
                }
            }
            BotBtn(Icons.Outlined.Share, true, onShare)
        }
    }
}

@Composable
private fun BotBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    tint: Color = CyanPrimary
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(44.dp)) {
        Icon(icon, null, tint = if (enabled) tint else TextMuted, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun BrowserHomePage(
    interceptedCount: Int,
    onNavigate: (String) -> Unit,
    onShowIntercepted: () -> Unit
) {
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Text("HAD Browser", color = CyanPrimary, fontSize = 22.sp,
            fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, letterSpacing = 3.sp)
        Text("Download anything from the web", color = TextMuted, fontSize = 12.sp)

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = CyanPrimary) },
            trailingIcon = {
                if (searchText.isNotBlank()) {
                    IconButton(onClick = { onNavigate(searchText); searchText = "" }) {
                        Icon(Icons.Filled.ArrowForward, null, tint = CyanPrimary)
                    }
                }
            },
            placeholder = { Text("Search or enter URL", color = TextMuted) },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = androidx.compose.ui.text.input.ImeAction.Go
            ),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onGo = { onNavigate(searchText); searchText = "" }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary, unfocusedBorderColor = BorderColor,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                unfocusedContainerColor = ElevatedSurf, focusedContainerColor = ElevatedSurf
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val text = cm.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""
                    if (text.isNotBlank()) {
                        onNavigate(text)
                        searchText = ""
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = ElevatedSurf, contentColor = CyanPrimary),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Outlined.ContentPaste, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Paste & Go", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        if (interceptedCount > 0) {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { onShowIntercepted() },
                shape = RoundedCornerShape(12.dp),
                color = PurpleAccent.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Download, null, tint = PurpleAccent, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("$interceptedCount file${if (interceptedCount > 1) "s" else ""} detected",
                            color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Tap to review and download", color = TextMuted, fontSize = 11.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = PurpleAccent.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                }
            }
        }

        Text("QUICK ACCESS", color = TextMuted, fontSize = 9.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
            modifier = Modifier.height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            userScrollEnabled = false
        ) {
            items(quickLinks.size) { idx ->
                val (name, url, icon) = quickLinks[idx]
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(url) },
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(icon, null, tint = CyanPrimary, modifier = Modifier.size(22.dp))
                        Text(name, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = ElevatedSurf,
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.15f))
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Outlined.Info, null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
                    Text("Smart interception active", color = CyanPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                listOf(
                    "Browse any site — videos, files, music",
                    "Content detected via WebView + JS bridge",
                    "Use Paste & Go to quickly open copied URLs",
                    "Tap ↓ button or download badge to download"
                ).forEach { step ->
                    Text("• $step", color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TabSwitcherOverlay(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack.copy(alpha = 0.93f))
            .clickable { onDismiss() }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TABS (${tabs.size})", color = TextPrimary, fontSize = 14.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onNewTab(); onDismiss() },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SpaceBlack),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, null, tint = TextMuted)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tabs, key = { it.id }) { tab ->
                    TabCard(
                        tab = tab,
                        isActive = tab.id == activeTabId,
                        onSelect = { onSelectTab(tab.id); onDismiss() },
                        onClose = { onCloseTab(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabCard(tab: BrowserTab, isActive: Boolean, onSelect: () -> Unit, onClose: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() }
            .border(1.dp, if (isActive) CyanPrimary.copy(alpha = 0.6f) else BorderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) SurfaceDark else ElevatedSurf
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(32.dp)
                    .background(if (isActive) CyanPrimary.copy(alpha = 0.15f) else BorderColor, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (tab.isLoading)
                    CircularProgressIndicator(color = CyanPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                else
                    Icon(Icons.Outlined.Language, null, tint = if (isActive) CyanPrimary else TextMuted, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(tab.title.ifBlank { "New Tab" }, color = if (isActive) TextPrimary else TextSecondary,
                    fontSize = 13.sp, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (tab.url.isNotBlank() && tab.url != "about:blank")
                    Text(tab.url, color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (tab.interceptedRequests.isNotEmpty()) {
                Box(modifier = Modifier.background(PurpleAccent.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text("${tab.interceptedRequests.size}↓", color = PurpleAccent, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun InterceptedRequestsPanel(
    requests: List<InterceptedRequest>,
    onDownload: (InterceptedRequest) -> Unit,
    onDownloadAll: (List<InterceptedRequest>) -> Unit,
    onCopyLink: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val grouped = remember(requests) {
        requests.groupBy { it.linkType }.toSortedMap(compareBy { it.name })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack.copy(alpha = 0.85f))
            .clickable { onDismiss() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .align(Alignment.BottomCenter)
                .clickable { },
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = SurfaceDark
        ) {
            Column(Modifier.padding(16.dp)) {
                Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp)
                    .background(BorderColor, RoundedCornerShape(2.dp)))
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.Download, null, tint = PurpleAccent, modifier = Modifier.size(18.dp))
                        Text("Detected (${requests.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (requests.isNotEmpty()) {
                            TextButton(onClick = { onDownloadAll(requests) }) {
                                Text("All ↓", color = CyanPrimary, fontSize = 11.sp)
                            }
                            TextButton(onClick = onClear) {
                                Text("Clear", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Filled.Close, null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                HorizontalDivider(color = BorderColor, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                if (requests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.SearchOff, null, tint = TextMuted, modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No files detected yet", color = TextMuted, fontSize = 13.sp)
                            Text("Browse to a page with media or files", color = TextMuted.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        grouped.forEach { (type, items) ->
                            item {
                                val typeColor = interceptedTypeColor(type)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)) {
                                    Box(Modifier.size(6.dp).background(typeColor, CircleShape))
                                    Text("${type.name}  (${items.size})", color = typeColor, fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                            items(items, key = { it.url }) { req ->
                                InterceptedCard(
                                    request = req,
                                    onDownload = { onDownload(req) },
                                    onCopyLink = { onCopyLink(req.url) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InterceptedCard(
    request: InterceptedRequest,
    onDownload: () -> Unit,
    onCopyLink: () -> Unit
) {
    val typeColor = interceptedTypeColor(request.linkType)
    val filename = request.url.substringAfterLast('/').substringBefore('?').ifBlank { request.url.take(30) }
    val ext = filename.substringAfterLast('.').lowercase().take(4)

    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = ElevatedSurf,
        border = BorderStroke(1.dp, typeColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(36.dp)
                    .background(typeColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(interceptedTypeIcon(request.linkType), null, tint = typeColor, modifier = Modifier.size(18.dp))
            }

            Column(Modifier.weight(1f)) {
                Text(filename, color = TextPrimary, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (ext.isNotBlank()) {
                        Box(modifier = Modifier.background(typeColor.copy(alpha = 0.1f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)) {
                            Text(".$ext", color = typeColor, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                    if (request.contentLength > 0) {
                        Text(request.contentLength.toHumanSizeLocal(), color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            IconButton(
                onClick = {
                    onCopyLink()
                    copied = true
                    scope.launch { delay(1500L); copied = false }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    if (copied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                    null,
                    tint = if (copied) GreenSuccess else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDownload,
                modifier = Modifier.size(36.dp)
                    .background(CyanPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.Filled.Download, null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun interceptedTypeColor(type: LinkType): Color = when (type) {
    LinkType.VIDEO, LinkType.HLS, LinkType.DASH -> GreenSuccess
    LinkType.AUDIO -> PurpleAccent
    LinkType.ARCHIVE -> OrangeWarn
    LinkType.DOCUMENT, LinkType.EBOOK -> CyanPrimary
    LinkType.TORRENT, LinkType.MAGNET -> RedError
    LinkType.EXECUTABLE -> RedError
    LinkType.IMAGE -> PurpleAccent
    else -> TextSecondary
}

@Composable
private fun interceptedTypeIcon(type: LinkType): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    LinkType.VIDEO, LinkType.HLS, LinkType.DASH -> Icons.Outlined.OndemandVideo
    LinkType.AUDIO -> Icons.Outlined.MusicNote
    LinkType.ARCHIVE -> Icons.Outlined.FolderZip
    LinkType.DOCUMENT -> Icons.Outlined.Description
    LinkType.EBOOK -> Icons.Outlined.MenuBook
    LinkType.TORRENT, LinkType.MAGNET -> Icons.Outlined.CloudDownload
    LinkType.IMAGE -> Icons.Outlined.Image
    else -> Icons.Outlined.Download
}

private fun Long.toHumanSizeLocal(): String = when {
    this < 0 -> "?"
    this < 1_024 -> "$this B"
    this < 1_048_576 -> "%.1f KB".format(this / 1_024f)
    this < 1_073_741_824 -> "%.1f MB".format(this / 1_048_576f)
    else -> "%.2f GB".format(this / 1_073_741_824f)
}