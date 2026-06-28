# HAD — Hyper Advanced Downloader

<div align="center">

![HAD Banner](https://img.shields.io/badge/HAD-Hyper%20Advanced%20Downloader-00D4FF?style=for-the-badge&logo=android&logoColor=white)

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![License](https://img.shields.io/badge/License-MIT-00FF88?style=flat-square)](LICENSE)
[![GitHub](https://img.shields.io/badge/GitHub-batmanpriv-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/batmanpriv)
[![Downloads](https://img.shields.io/badge/Downloads-1K%2B-blue?style=flat-square)]()

**A fully native Android download manager built entirely in Kotlin.**  
*Multi-thread • HLS Streams • Torrent • Web Archive • Remote Control*

</div>

---

## 📖 Table of Contents

- [✨ Features](#-features)
- [📱 Screenshots](#-screenshots)
- [🏗️ Architecture](#️-architecture)
- [🛠️ Tech Stack](#️-tech-stack)
- [🚀 Quick Start](#-quick-start)
- [⚙️ Configuration](#️-configuration)
- [📋 Requirements](#-requirements)
- [🤝 Contributing](#-contributing)
- [📄 License](#-license)
- [📬 Contact](#-contact)
- [💰 Support](#-support)

---

## ✨ Features

### ⚡ Core Downloading

| Feature | Description |
|---------|-------------|
| **Multi-thread HTTP** | Split files into parallel chunks for maximum speed (up to 16 threads) |
| **Resumable downloads** | Automatically resumes from exact byte position after interruption, app close, or device reboot |
| **Session saving** | Download progress persisted to disk; never lose progress |
| **Smart queue** | Configurable concurrent download limit; queue promotes automatically when a slot opens |
| **Mirror / fallback URLs** | Probes all mirrors on start, picks the fastest; auto-switches on failure |
| **Adaptive buffer** | Buffer size adjusts dynamically based on connection speed |
| **Speed limiting** | Token Bucket algorithm for smooth, configurable bandwidth cap per download |

### 📺 HLS / Stream Downloading

- Full **.m3u8 playlist** support (VOD and Live streams)
- Auto-selects highest quality variant from master playlist
- Parallel segment downloading with configurable thread count
- **AES-128 and SAMPLE-AES decryption** — fetches key automatically
- Segments merged and converted to a single **.mp4** file via FFmpeg
- Live stream recording — continuously downloads new segments until stream ends or manually stopped
- fMP4 / fragmented MP4 init segment support
- Audio and subtitle track extraction

### 🌊 Torrent & Magnet

| Feature | Description |
|---------|-------------|
| **.torrent file** | Full parsing and downloading |
| **Magnet links** | Support with DHT metadata fetching |
| **Multi-tracker** | HTTP + UDP tracker announce |
| **DHT discovery** | Bootstrap nodes for peer finding |
| **Piece verification** | SHA-1 checksum validation |
| **Per-file selection** | Choose exactly which files to download |
| **Peer management** | Choking/unchoking algorithm with rarest-first piece selection |
| **Endgame mode** | Optimized completion for near-finished downloads |

### 🌐 Built-in Browser

- Full WebView browser with **multi-tab** support
- **Automatic file interception** — detects downloadable files via WebView + JavaScript bridge
- Monitors XHR, fetch, video/audio/source elements in real time
- **Incognito mode** — no history, cookies, or cache saved
- **Desktop Site** toggle
- Paste & Go from clipboard
- Intercepted files panel with one-tap download
- Quick access to popular websites

### 🔍 Link Scraper

- Scrape any web page for downloadable links
- Detects: video, audio, archives, documents, HLS, DASH, torrents, subtitles, images, fonts, and more
- Filter by file type, extension, or filename keyword
- Select multiple links and batch download or queue
- Direct link to New Download dialog for custom configuration per link

### 📦 Web Archive

- Save any website for **offline viewing**
- **Single Page** mode (fast) or **Full Site** crawl mode (recursive)
- Downloads HTML, CSS, JavaScript, images, fonts, and all assets
- Rewrites all internal links for correct offline rendering
- Configurable: max pages, concurrency, asset size limit, retries
- Cookie and custom header support for authenticated pages
- Optional HTML minification
- iframe crawling and hash route (SPA) support

### 📡 Remote Download Server

- Turns your phone into a **local HTTP server** (port 8080)
- Control downloads from any PC browser on the same Wi-Fi
- Web UI with single URL download, bulk URL download, and request history
- Re-download or re-queue any past request from history
- Live server status and request counter

### 📋 Clipboard Detection

- Detects URLs copied to clipboard on demand
- Handles single URLs or multiple URLs in one paste
- Auto-detects download type (HTTP / HLS / torrent)
- Selection dialog for multi-URL clipboard content

### 🔄 Smart Retry Engine

| Status Code | Strategy |
|-------------|----------|
| **403** | Rotates User-Agent, adds browser-like headers |
| **429** | Exponential back-off (5s → 120s) |
| **503** | Increasing delay with UA rotation |
| **Timeout** | Increasing delay up to 60s |
| **500-599** | Exponential back-off with retry limit |

- Pool of 8 User-Agent strings (Chrome, Firefox, Safari, curl, wget, Python)
- Cookie refresh on auth failures

### ⏰ Download Scheduler

- Schedule any download to start at a specific date and time
- Global **time window** setting — downloads only run within allowed hours
- AlarmManager with `setExactAndAllowWhileIdle` — fires even in Doze mode
- Downloads auto-pause outside the window and resume when it opens
- Preset scheduling chips: Tonight, Midnight, Dawn

### 🔐 Advanced Options (per download)

| Option | Description |
|--------|-------------|
| **Proxy** | HTTP and SOCKS5 support |
| **Cookies** | Custom cookie header |
| **HTTP Headers** | Custom headers (one per line) |
| **User-Agent** | Custom user agent string |
| **Checksum** | MD5, SHA-1, SHA-256, SHA-512 |
| **HTTP Method** | GET, POST, etc. |
| **Thread Count** | Configurable per download |
| **Max Speed** | Bandwidth cap per download |
| **Retries** | Per-chunk retry count |
| **Timeout** | Connection timeout |

### 🗂️ Duplicate Detection

- Checks filesystem and download history before starting
- Options:
  - **Rename** — auto-increments filename (file (1).mp4)
  - **Overwrite** — replaces existing file
  - **Skip** — cancels download

### 📊 Analytics

| Metric | Description |
|--------|-------------|
| **Overview** | Total downloads, success rate, total saved, avg and peak speed |
| **Monthly chart** | Download volume per month |
| **Hourly heatmap** | Download distribution by hour of day |
| **Speed history** | Real-time speed graph for current download |
| **Recent history** | Last 200 downloads with status, size, and speed |
| **Auto-purge** | Data older than 30 days automatically removed |

### 🎨 UI & Design

- **Dark space theme** with electric cyan accent (`#00D4FF`)
- Built entirely with **Jetpack Compose + Material3**
- Animated progress bars with glow effects
- Thread visualizer — 3 modes: Segment Bar, Waveform, Grid
- Countdown badge for scheduled downloads
- Multi-select mode with batch delete
- Status badges with pulse animation
- Smooth animated transitions throughout

---

## 📱 Screenshots

> *Screenshots coming soon — stay tuned!*

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
│   ├── SmartDownloader.kt             ← Core multi-thread HTTP engine
│   ├── HlsDownloader.kt               ← HLS/M3U8 stream downloader
│   ├── TorrentEngine.kt               ← BitTorrent client
│   ├── ScraperEngine.kt               ← Web page link extractor
│   ├── VideoDetectionEngine.kt        ← Stream/media URL detector
│   ├── WebArchiveEngine.kt            ← Website archiver/crawler
│   ├── RemoteDownloadServer.kt        ← Local HTTP control server
│   ├── ForegroundDownloadService.kt   ← Android foreground service
│   ├── DownloadSchedulerService.kt    ← Scheduled download service
│   ├── SmartRetryEngine.kt            ← Intelligent retry logic
│   ├── DuplicateDetector.kt           ← Duplicate file detection
│   ├── FileSizeFetcher.kt             ← HEAD/GET file info fetcher
│   ├── ClipboardMonitor.kt            ← Clipboard URL detection
│   ├── BrowserInterceptor.kt          ← WebView request interceptor
│   ├── Scheduler.kt                   ← AlarmManager scheduling
│   ├── BootReceiver.kt                ← Resume downloads after reboot
│   ├── DownloadAlarmReceiver.kt       ← Alarm trigger handler
│   └── TriggerReceiver.kt             ← Internal broadcast receiver
│
└── ui/
    ├── theme/
    │   └── Theme.kt                   ← Colors, typography, Material3 scheme
    ├── components/
    │   ├── Components.kt              ← Shared UI components
    │   ├── ThreadVisualizer.kt        ← Download thread visualization
    │   └── HlsProgressCard.kt         ← HLS segment progress display
    └── screens/
        ├── MainScreen.kt              ← Main UI + navigation drawer
        ├── MainViewModel.kt           ← Central ViewModel
        ├── AnalyticsTab.kt            ← Analytics dashboard
        ├── BrowserTab.kt              ← Built-in browser
        ├── EnhancedBrowserTab.kt      ← Browser implementation
        ├── TorrentTab.kt              ← Torrent management UI
        ├── RemoteServerTab.kt         ← Remote server control UI
        ├── WebArchiveTab.kt           ← Web archive UI
        ├── SettingsDialog.kt          ← App settings
        ├── AboutTab.kt                ← About + donate
        ├── GuideTab.kt                ← Full in-app user guide
        ├── SchedulePicker.kt          ← Schedule time picker
        ├── PermissionScreen.kt        ← Storage permission gate
        ├── ShareReceiverActivity.kt   ← Share intent handler
        └── ChunkSessionManager.kt     ← Download session persistence
```

---

## 🛠️ Tech Stack

| Technology | Purpose |
|-----------|---------|
| **Kotlin** | 100% of the codebase |
| **Jetpack Compose** | Declarative UI framework |
| **Material3** | Design system |
| **Hilt / Dagger** | Dependency injection |
| **Room** | Local database for download history |
| **Kotlin Coroutines + Flow** | Async operations and reactive state |
| **FFmpeg Kit** | HLS segment merging and MP4 conversion |
| **AlarmManager** | Exact scheduled downloads (Doze-safe) |
| **WebView** | Built-in browser with JS bridge |
| **SharedPreferences** | User preferences storage |
| **Foreground Service** | Background download execution |
| **Broadcast Receiver** | Boot completion and alarm triggers |

---

## 🚀 Quick Start

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 36

### Steps

```bash
git clone https://github.com/batmanpriv/HAD.git
cd HAD
```

Open in Android Studio:
```
File → Open → select the HAD folder → wait for Gradle sync
```

Run on device:
```
Run → Run 'app'
```

Build release APK:
```
Build → Generate Signed Bundle/APK → APK
```

---

## ⚙️ Configuration

All settings are configurable from the in-app **Settings** screen:

| Setting | Default | Description |
|---------|---------|-------------|
| Default threads | 4 | Parallel chunks per download |
| Max concurrent | 2 | Simultaneous active downloads |
| Default proxy | — | HTTP or SOCKS5 proxy |
| Max speed | Unlimited | Bandwidth cap in bytes/s |
| Retries | 5 | Max retry attempts per chunk |
| Timeout | 30s | Connection timeout |
| Schedule window | Disabled | Allowed download hours |
| Save folder | Downloads/HAD | Default output directory |
| Gzip | Disabled | Enable gzip compression |
| Session resume | Enabled | Resume interrupted downloads |

---

## 📋 Requirements

| Requirement | Details |
|-------------|---------|
| **Android Version** | 8.0+ (API 26+) |
| **Architecture** | arm64-v8a |
| **Storage Permission** | `MANAGE_EXTERNAL_STORAGE` (Android 11+) or `WRITE_EXTERNAL_STORAGE` (Android 10 and below) |
| **Network** | Internet access for downloads |
| **Optional** | Wi-Fi for Remote Server |

---

## 🤝 Contributing

Contributions are welcome! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Guidelines

- Follow Kotlin coding conventions
- Write meaningful commit messages
- Update documentation for user-facing changes
- Test on multiple Android versions when possible

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 📬 Contact

| Platform | Link |
|----------|------|
| **GitHub** | [@batmanpriv](https://github.com/batmanpriv) |
| **Email** | spect3rog@gmail.com |
| **Donate** | [coffeete.ir/specter](https://www.coffeete.ir/specter) |

---

## 💰 Support

If HAD has been useful to you, consider supporting its development:

### Bitcoin (BTC)
```
bc1q00h2kl4uvwcvzp7zdl80yx97f0p8jv450qdzwc
```

### Tron (TRX)
```
TL1417xeaNrvU6La3N5Vgpye1e47i4zHUv
```

[![Donate](https://img.shields.io/badge/Donate-coffeete.ir-FF8C42?style=for-the-badge)](https://www.coffeete.ir/specter)

---

<div align="center">

**Built with ❤️ entirely in Kotlin + Jetpack Compose**

*A fully Kotlin-based native Android app*

</div>