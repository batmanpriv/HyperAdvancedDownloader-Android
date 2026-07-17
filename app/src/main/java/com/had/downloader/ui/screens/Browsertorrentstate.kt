package com.had.downloader.ui.screens

import com.had.downloader.service.InterceptedRequest
import com.had.downloader.service.TorrentProgress
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "New Tab",
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoFwd: Boolean = false,
    val favicon: android.graphics.Bitmap? = null,
    val interceptedRequests: List<InterceptedRequest> = emptyList()
)

data class BrowserUiState(
    val tabs: List<BrowserTab> = listOf(BrowserTab()),
    val activeTabId: String = "",
    val showTabSwitcher: Boolean = false,
    val showIntercepted: Boolean = false
) {
    val activeTab: BrowserTab
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull() ?: BrowserTab()
}

data class BrowserState(
    val currentUrl: String = "",
    val pageTitle: String = "",
    val loading: Boolean = false,
    val progress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoFwd: Boolean = false
)

data class TorrentUiState(
    val active: List<TorrentProgress> = emptyList(),
    val completed: List<TorrentProgress> = emptyList(),
    val failed: List<TorrentProgress> = emptyList()
)