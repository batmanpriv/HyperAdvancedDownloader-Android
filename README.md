# HAD — Hyper Advanced Downloader

<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=Caveat&weight=600&size=28&pause=1000&color=00D4FFFF&center=true&vCenter=true&random=false&width=700&height=70&lines=Hyper+Advanced+Downloader;TORRENT+%26+MAGNET+SUPPORT;HLS+STREAMING+DOWNLOADER;SMART+BROWSER+INTERCEPTOR;WEB+ARCHIVE+OFFLINE;REMOTE+CONTROL+SERVER;LINK+SCRAPER+PRO;SCHEDULE+DOWNLOADS" alt="Typing SVG" />
</p>

**English** | [**فارسی**](https://github.com/batmanpriv/HyperAdvancedDownloader-Android/blob/main/READMEFA.md)

<div align="center">
  <a href="https://github.com/batmanpriv/HyperAdvancedDownloader-Android">
    <img src="https://github.com/user-attachments/assets/0fc8e78b-e3d9-44b2-8d85-7ba91ece0ef0" alt="HAD Banner" width="100%">
  </a>
</div>

<div align="center">

![HAD Banner](https://img.shields.io/badge/HAD-Hyper%20Advanced%20Downloader-00D4FF?style=for-the-badge&logo=android&logoColor=white)

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Version](https://img.shields.io/badge/Version-3.6.8-00FF88?style=flat-square)](https://github.com/batmanpriv/HyperAdvancedDownloader-Android)
[![Downloads](https://img.shields.io/badge/Downloads-50%2B-blue?style=flat-square)]()

**A fully native Android download manager built entirely in Kotlin.**  
*Multi-thread • HLS Streams • Torrent • Web Archive • Remote Control • QR Code*

</div>

---

## 📖 Table of Contents

- [🔥 What is HAD?](#-what-is-had)
- [🚀 Features Deep Dive](#-features-deep-dive)
  - [⚡ Core Download Engine](#-core-download-engine)
  - [📺 HLS & Stream Downloader](#-hls--stream-downloader)
  - [🌊 Torrent & Magnet Client](#-torrent--magnet-client)
  - [🌐 Built-in Smart Browser](#-built-in-smart-browser)
  - [🔍 Link Scraper Pro](#-link-scraper-pro)
  - [📦 Web Archive (Offline Pages)](#-web-archive-offline-pages)
  - [📡 Remote Download Server](#-remote-download-server)
  - [📋 Clipboard Intelligence](#-clipboard-intelligence)
  - [🔄 Smart Retry Engine](#-smart-retry-engine)
  - [⏰ Download Scheduler](#-download-scheduler)
  - [🔐 Advanced Options](#-advanced-options)
  - [📊 Analytics Dashboard](#-analytics-dashboard)
  - [🎨 UI & Experience](#-ui--experience)
- [📱 Screenshots](#-screenshots)
- [🏗️ Architecture](#️-architecture)
- [🛠️ Tech Stack](#️-tech-stack)
- [🚀 Quick Start](#-quick-start)
- [⚙️ Configuration Guide](#️-configuration-guide)
- [📋 Requirements](#-requirements)
- [📄 License](#-license)
- [📬 Contact & Support](#-contact--support)

---

## 🔥 What is HAD?

**HAD (Hyper Advanced Downloader)** is a premium, fully-native Android download manager that brings **PC-grade download capabilities** to your mobile device. Built entirely in Kotlin with Jetpack Compose, it combines enterprise-level features with a sleek, modern interface.

Whether you're downloading 4K movies, streaming HLS video, torrenting Linux ISOs, archiving entire websites, or controlling downloads from your PC browser — HAD does it all with **speed, reliability, and elegance.**

---

## 🚀 Features Deep Dive

### ⚡ Core Download Engine

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Multi-thread Download** | Set thread count in New Download dialog (1-32). Default: 4 | Large files (1GB+) on fast connections; increases speed by splitting file into parallel chunks |
| **Resumable Downloads** | Enabled by default. Tap Retry on any paused/failed download | When internet drops, app closes, or phone reboots — resume from exact byte position, not from zero |
| **Session Persistence** | Automatic. Progress saved every few seconds | Never lose progress; even after force-stop or system crash |
| **Smart Queue** | Set Max Concurrent in Settings → Download Defaults (default: 2) | When you have many downloads and want to control bandwidth/performance |
| **Mirror URLs** | Add fallback URLs in Advanced Options (one per line) | When primary server is slow or unreliable — HAD auto-switches to fastest mirror |
| **Adaptive Buffer** | Automatic based on connection speed | Optimizes memory usage; small buffer for slow connections, large for fast ones |
| **Speed Limiting** | Set Max KB/s per download (0 = unlimited) | On limited data plans or to prevent downloads from saturating your network |

### 📺 HLS & Stream Downloader

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Auto-detect HLS** | Paste .m3u8 URL — mode switches to HLS automatically | Downloading video streams from sites like YouTube, Vimeo, or any HLS-powered platform |
| **Quality Selection** | Master playlist detected; auto-selects highest quality | Watching offline — get the best quality available |
| **Parallel Segments** | Configurable threads in Advanced Options | Faster download of segmented streams |
| **AES-128 / SAMPLE-AES Decryption** | Automatic key fetch from playlist | Downloading DRM-protected streams (common in premium content) |
| **Live Stream Recording** | Start download on live .m3u8 URL | Recording live events, sports, or streams — continuously downloads new segments |
| **fMP4 Support** | Handles fragmented MP4 init segments | Downloading modern streaming formats |
| **Audio/Subtitle Extraction** | Automatic track extraction (where available) | Getting multi-language content for offline playback |

### 🌊 Torrent & Magnet Client

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **.torrent Files** | Open Torrent tab → tap + → select .torrent file | Downloading from tracker-based torrents |
| **Magnet Links** | Tap chain icon in Torrent tab → paste magnet: link | Quick download without downloading .torrent file |
| **DHT Metadata Fetching** | Automatic when magnet link has no metadata | Getting metadata for magnet links from the DHT network |
| **Multi-Tracker Support** | Automatic UDP + HTTP announce | Faster peer discovery from multiple trackers |
| **Per-File Selection** | Check/uncheck files in TorrentStartDialog | Downloading only specific files from a multi-file torrent |
| **Piece Verification** | SHA-1 checksum automatically verified | Ensuring downloaded data is not corrupted |
| **Peer Management** | Choking/unchoking with rarest-first selection | Optimizing download speed and network efficiency |
| **Endgame Mode** | Automatic for near-complete downloads | Parallel downloading of remaining pieces for fastest completion |

### 🌐 Built-in Smart Browser

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Multi-Tab Support** | Tap tab count → New Tab | Browsing multiple sites simultaneously |
| **Auto File Interception** | Browse normally; files detected automatically | Finding downloadable files on any website without manual URL extraction |
| **JavaScript Bridge** | HAD injects interceptor script automatically | Detecting dynamically loaded content (XHR, fetch, video elements) |
| **Incognito Mode** | Three-dot menu → Incognito | Private browsing — no history, cookies, or cache saved |
| **Desktop Site** | Three-dot menu → Desktop Site | Accessing desktop-only download links or content |
| **Paste & Go** | Three-dot menu → Paste & Go | Quick navigation from clipboard URLs |
| **Intercepted Files Panel** | Tap purple badge on bottom right | Reviewing and downloading all detected files in one place |

**Pro Tip:** HAD's browser is smarter than regular browsers — it monitors network requests in real-time and catches download URLs that other browsers miss.

### 🔍 Link Scraper Pro

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Scrape Any Page** | Globe icon → paste URL → Scrape Links | Finding all downloadable content on a page without manual searching |
| **Type Filtering** | Filter by VIDEO, AUDIO, ARCHIVE, DOCUMENT, etc. | Focusing on specific content types |
| **Extension Filter** | Filter by .mp4, .mp3, .zip, .pdf, etc. | Finding files with specific extensions |
| **Quality Filter** | Filter by 4K, 1080p, 720p, etc. | Selecting only high-quality video streams |
| **Batch Download** | Select multiple links → Download All | Grabbing many files at once |
| **Batch Queue** | Select multiple links → Queue All | Adding many files to queue without starting |
| **Direct Add** | Tap + on any link → opens New Download dialog | Configuring specific settings per link |
| **Cache System** | Results cached; Re-scrape button available | Revisiting scraped pages without re-downloading |

**Pro Tip:** Great for downloading entire playlists, episode packs, or all documents from a page in one go!

### 📦 Web Archive (Offline Pages)

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Single Page Mode** | Web Archive → + → select "Single Page" | Saving a single article, recipe, or page for offline reading |
| **Full Site Crawl** | Select "Full Site" → set max pages | Archiving entire websites for offline browsing |
| **Asset Download** | CSS, JS, images, fonts automatically saved | Complete offline rendering — everything included |
| **Link Rewriting** | All internal links rewritten automatically | Pages work offline exactly as online |
| **External Asset Support** | Toggle "Download external assets" | Including content from other domains |
| **Hash Route Support** | Toggle for SPA (Single Page Apps) | Archiving modern JavaScript-heavy websites |
| **Minification** | Toggle to minify HTML output | Smaller archive size |
| **Authentication Support** | Add cookies in Advanced Options | Archiving pages behind login |

**Pro Tip:** Perfect for saving documentation, news articles, tutorials, or any content you want to access offline.

### 📡 Remote Download Server

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Start Server** | Side menu → Remote Server → toggle on | Controlling downloads from your PC while phone is on same Wi-Fi |
| **Single URL Download** | Paste URL in web UI → Download Now | Sending individual download links from PC to phone |
| **Bulk Download** | Paste multiple URLs (one per line) → Download All | Sending many links at once |
| **Queue Support** | Download Now or Add to Queue | Choosing immediate download or queue scheduling |
| **Request History** | View all past requests in web UI | Re-downloading or re-queuing previous URLs |
| **Live Status** | Request count, connected clients, last request | Monitoring server activity |

**Pro Tip:** The server address appears on your phone — just type it in your PC browser and start controlling downloads from your desk!

### 📋 Clipboard Intelligence

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Single URL** | Copy URL → tap paste icon | Quick download of copied links |
| **Multiple URLs** | Copy multiple URLs → selection dialog appears | Batch adding multiple links from copied text |
| **Auto Detection** | HAD reads clipboard on demand | Whenever you copy a download link |
| **Smart Mode Detection** | Auto-detects HTTP, HLS, or Torrent | No need to manually set download mode |

### 🔄 Smart Retry Engine

| Status Code | Strategy | When You'll See It |
|-------------|----------|-------------------|
| **403 Forbidden** | Rotates User-Agent → adds browser headers | Sites blocking download managers; HAD mimics real browser |
| **429 Rate Limit** | Exponential backoff (5s → 120s) | When downloading too many files from the same server |
| **503 Service Unavailable** | Increasing delay + UA rotation | Server overloaded; HAD waits and retries |
| **Timeout** | Increasing delay up to 60s | Slow connections or network issues |
| **500-599 Errors** | Exponential backoff with retry limit | Server errors; HAD retries intelligently |

### ⏰ Download Scheduler

| Feature | How to Use | When to Use |
|---------|-----------|-------------|
| **Per-Download Schedule** | Advanced Options → schedule date/time | Starting specific downloads at a later time |
| **Global Time Window** | Settings → Schedule Window → set hours | Running downloads only during specific hours (e.g., overnight) |
| **Preset Chips** | Tonight, Midnight, Dawn in scheduler | Quick scheduling without manual typing |
| **Auto-Pause/Resume** | Downloads pause outside window, resume when it opens | Saving bandwidth during peak hours or using off-peak data |

**Pro Tip:** Set a global schedule (e.g., 23:00 → 06:00) to run all your large downloads overnight while you sleep!

### 🔐 Advanced Options

| Option | How to Use | When to Use |
|--------|-----------|-------------|
| **Proxy** | `socks5://host:port` or `http://host:port` | Using a VPN, bypassing geo-restrictions, or corporate networks |
| **Cookies** | `name=value; name2=value2` | Sites requiring login (streaming platforms, paid content) |
| **Custom Headers** | `Header: value` (one per line) | Adding authentication tokens, referers, or custom requirements |
| **User-Agent** | Custom UA string | Sites blocking download managers; simulate any browser |
| **Checksum** | MD5, SHA-1, SHA-256, SHA-512 | Verifying file integrity after download (critical for important files) |
| **HTTP Method** | GET, POST, etc. | APIs or endpoints that require specific methods |
| **Thread Count** | 1-32 per download | Tuning performance per connection |
| **Max Speed** | KB/s limit per download | Bandwidth management |

### 📊 Analytics Dashboard

| Metric | How to Use | When to Use |
|--------|-----------|-------------|
| **Overview Stats** | Side menu → Analytics → Overview | Getting summary of all downloads |
| **Monthly Chart** | View monthly download volume | Understanding usage patterns |
| **Hourly Heatmap** | View hourly distribution | Optimizing schedule window |
| **Speed History** | Real-time speed graph during downloads | Monitoring current download performance |
| **Recent History** | Last 200 downloads with details | Troubleshooting or tracking specific downloads |
| **Auto-Purge** | Data > 30 days auto-removed | Keeping database manageable |

### 🎨 UI & Experience

| Feature | How to Use | Why It's Great |
|---------|-----------|----------------|
| **Dark Space Theme** | Automatic; no option needed | Eye-friendly, looks premium, saves battery |
| **Glow Progress Bars** | Animated with electric cyan | Visual feedback that feels alive |
| **Thread Visualizer** | 3 modes: Segment Bar, Waveform, Grid | Visualizing download progress at a glance |
| **Status Badges** | Animated pulse for active states | Instant status recognition |
| **Swipe Actions** | Swipe left → Stop/Delete, Swipe right → Pause/Resume | Quick actions without opening menus |
| **Countdown Badge** | Shows remaining time for scheduled downloads | Know exactly when downloads start |
| **Multi-Select** | Long press → select multiple → batch delete | Managing many downloads efficiently |
| **Mini Player** | Minimized progress bar at bottom | Keep track of downloads while browsing |
| **QR Code Support** | Generate/Share QR codes for download URLs | Sharing download links easily |

---

## 📱 Screenshots

<div align="center">
  <table>
    <tr>
      <td><img src="https://github.com/user-attachments/assets/4cd9f819-76fb-4d0f-8b7e-38a9e6ed100e" width="200" alt="Main Screen"/></td>
      <td><img src="https://github.com/user-attachments/assets/ea94798d-efda-4264-a4f8-94290fe94a4e" width="200" alt="Download Manager"/></td>
      <td><img src="https://github.com/user-attachments/assets/88d4088e-8017-4e26-8401-5b079e262572" width="200" alt="HLS Download"/></td>
      <td><img src="https://github.com/user-attachments/assets/e38134c4-c530-4ba6-b3b8-1972ceb17fa6" width="200" alt="Torrent Client"/></td>
    </tr>
    <tr>
      <td align="center"><b>Download Page</b></td>
      <td align="center"><b>Download Manager</b></td>
      <td align="center"><b>HLS Stream Download</b></td>
      <td align="center"><b>Main</b></td>
    </tr>
  </table>
</div>

---

## 🏗️ Architecture

```
app/src/main/java/com/had/downloader/
│
├── MainActivity.kt                    ← Entry point + permission gates
├── AppModule.kt                       ← Hilt dependency injection
│
├── data/
│   ├── model/
│   │   └── Models.kt                  ← Data classes, enums, extension functions
│   └── repository/
│       ├── Database.kt                ← Room DB, DAOs, type converters
│       ├── AnalyticsDao.kt            ← Analytics queries
│       └── AnalyticsRepository.kt     ← Analytics business logic
│
├── service/
│   ├── SmartDownloader.kt             ← Core multi-thread HTTP engine (16 threads max)
│   ├── HlsDownloader.kt               ← HLS/M3U8 stream downloader with FFmpeg merge
│   ├── TorrentEngine.kt               ← BitTorrent client with DHT and tracker support
│   ├── ScraperEngine.kt               ← Web page link extractor (30+ link types)
│   ├── VideoDetectionEngine.kt        ← Stream/media URL detector
│   ├── WebArchiveEngine.kt            ← Website archiver/crawler (full site support)
│   ├── RemoteDownloadServer.kt        ← Local HTTP control server on port 8080
│   ├── ForegroundDownloadService.kt   ← Android foreground service with notifications
│   ├── DownloadSchedulerService.kt    ← Scheduled download service
│   ├── SmartRetryEngine.kt            ← Intelligent retry logic (8 User-Agents pool)
│   ├── DuplicateDetector.kt           ← Duplicate file detection
│   ├── FileSizeFetcher.kt             ← HEAD/GET file info fetcher
│   ├── ClipboardMonitor.kt            ← Clipboard URL detection
│   ├── BrowserInterceptor.kt          ← WebView request interceptor
│   ├── Scheduler.kt                   ← AlarmManager scheduling (Exact + Doze-safe)
│   ├── BootReceiver.kt                ← Resume downloads after reboot
│   ├── DownloadAlarmReceiver.kt       ← Alarm trigger handler
│   └── TriggerReceiver.kt             ← Internal broadcast receiver
│
├── ui/
│   ├── theme/
│   │   └── Theme.kt                   ← Colors (#00D4FF), typography, Material3 scheme
│   ├── components/
│   │   ├── Components.kt              ← Shared UI components (DownloadCard, etc.)
│   │   ├── ThreadVisualizer.kt        ← Download thread visualization (3 modes)
│   │   ├── HlsProgressCard.kt         ← HLS segment progress display
│   │   ├── QRCodeDialog.kt            ← QR Code generation and sharing
│   │   └── QRCodeGenerator.kt         ← QR Code generation engine
│   └── screens/
│       ├── MainScreen.kt              ← Main UI + navigation drawer
│       ├── MainViewModel.kt           ← Central ViewModel with all business logic
│       ├── AnalyticsTab.kt            ← Analytics dashboard with charts
│       ├── BrowserTab.kt              ← Built-in browser
│       ├── EnhancedBrowserTab.kt      ← Browser implementation with interception
│       ├── TorrentTab.kt              ← Torrent management UI
│       ├── RemoteServerTab.kt         ← Remote server control UI
│       ├── WebArchiveTab.kt           ← Web archive UI
│       ├── SettingsDialog.kt          ← App settings dialog
│       ├── AboutTab.kt                ← About + donate
│       ├── GuideTab.kt                ← Full in-app user guide (EN/FA)
│       ├── SchedulePicker.kt          ← Schedule time picker
│       ├── PermissionScreen.kt        ← Storage permission gate
│       ├── ShareReceiverActivity.kt   ← Share intent handler
│       └── ChunkSessionManager.kt     ← Download session persistence
│
├── res/
│   ├── xml/
│   │   └── file_paths.xml             ← FileProvider paths for QR sharing
│   └── values/
│       └── strings.xml                ← App strings
│
└── AndroidManifest.xml                ← Permissions and component declarations
```

---

## 🛠️ Tech Stack

| Technology | Purpose | Version |
|-----------|---------|---------|
| **Kotlin** | 100% of the codebase | 1.9.0 |
| **Jetpack Compose** | Declarative UI framework | 2024.02.00 |
| **Material3** | Design system | 1.2.0 |
| **Hilt / Dagger** | Dependency injection | 2.48 |
| **Room** | Local database for download history | 2.6.0 |
| **Kotlin Coroutines + Flow** | Async operations and reactive state | 1.7.3 |
| **FFmpeg Kit** | HLS segment merging and MP4 conversion | 6.0-1 |
| **AlarmManager** | Exact scheduled downloads (Doze-safe) | Android Framework |
| **WebView** | Built-in browser with JS bridge | Android Framework |
| **SharedPreferences** | User preferences storage | Android Framework |
| **Foreground Service** | Background download execution | Android Framework |
| **Broadcast Receiver** | Boot completion and alarm triggers | Android Framework |
| **ZXing** | QR Code generation | 3.5.3 |

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 36

### Steps

```bash
git clone https://github.com/batmanpriv/HyperAdvancedDownloader-Android.git
cd HyperAdvancedDownloader-Android
```

Open in Android Studio:
```
File → Open → select the HyperAdvancedDownloader-Android folder → wait for Gradle sync
```

Run on device:
```
Run → Run 'app' (select your device or emulator)
```

Build release APK:
```
Build → Generate Signed Bundle/APK → APK → follow the signing wizard
```

---

## ⚙️ Configuration Guide

All settings are configurable from the in-app **Settings** screen:

| Setting | Default | Description | How to Change |
|---------|---------|-------------|---------------|
| **Default Threads** | 4 | Parallel chunks per download (1-32) | Settings → Download Defaults → Threads |
| **Max Concurrent** | 2 | Simultaneous active downloads (1-10) | Settings → Download Defaults → Max parallel |
| **Default Proxy** | None | HTTP or SOCKS5 proxy for all downloads | Settings → Network → Default Proxy |
| **Max Speed** | Unlimited | Bandwidth cap in bytes/s | Settings → Network → Max Speed |
| **Retries** | 5 | Max retry attempts per chunk (1-20) | Settings → Download Defaults → Retries |
| **Timeout** | 30s | Connection timeout in seconds (5-300) | Settings → Download Defaults → Timeout |
| **Schedule Window** | Disabled | Allowed download hours (e.g., 23:00-06:00) | Settings → Schedule Window |
| **Save Folder** | Downloads/HAD | Default output directory | Settings → Download Defaults → Save Folder |
| **Gzip** | Disabled | Enable gzip compression | Settings → Options → Enable Gzip |
| **Session Resume** | Enabled | Resume interrupted downloads | Settings → Options → Save Session |
| **Notifications** | Enabled | Show download notifications | Settings → Options → Show Notifications |

---

## 📋 Requirements

| Requirement | Details |
|-------------|---------|
| **Android Version** | 8.0+ (API 26+) |
| **Architecture** | arm64-v8a (ARM64) |
| **Storage Permission** | `MANAGE_EXTERNAL_STORAGE` (Android 11+) or `WRITE_EXTERNAL_STORAGE` (Android 10 and below) |
| **Network** | Internet access for downloads |
| **Optional** | Wi-Fi for Remote Server, Camera for QR Code scanning |

---

### Guidelines

- Follow Kotlin coding conventions
- Write meaningful commit messages
- Update documentation for user-facing changes
- Test on multiple Android versions when possible

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 📬 Contact & Support

| Platform | Link |
|----------|------|
| **GitHub** | [@batmanpriv](https://github.com/batmanpriv) |
| **Email** | spect3rog@gmail.com |
| **Donate** | [coffeete.ir/specter](https://www.coffeete.ir/specter) |

### Crypto Donations

| Currency | Address |
|----------|---------|
| **Bitcoin (BTC)** | `bc1q7rags3da9a549u22e8t9fmw7j94kgxwflfy2f8` |
| **Tron (TRX)** | `TQsUASZzfcKg4AckFFv1YjKgU8QCniUwhv` |

[![Donate](https://img.shields.io/badge/Donate-coffeete.ir-FF8C42?style=for-the-badge)](https://www.coffeete.ir/specter)

---

<div align="center">

**Built with ❤️ entirely in Kotlin + Jetpack Compose**

*A fully Kotlin-based native Android app*

</div>
