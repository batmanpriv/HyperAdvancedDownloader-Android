package com.had.downloader.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.had.downloader.ui.theme.*
import kotlinx.coroutines.delay
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private const val BTC_WALLET = "bc1q00h2kl4uvwcvzp7zdl80yx97f0p8jv450qdzwc"
private const val TRX_WALLET = "TL1417xeaNrvU6La3N5Vgpye1e47i4zHUv"
private const val DONATE_URL = "https://www.coffeete.ir/specter"
private const val GITHUB_URL = "https://github.com/batmanpriv"
private const val CONTACT_EMAIL = "spect3rog@gmail.com"

private enum class AppLang { FA, EN }

private data class L(val fa: String, val en: String)
private fun L.get(lang: AppLang) = if (lang == AppLang.FA) fa else en

private data class FeatureL(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: L,
    val desc: L,
    val color: Color
)

private val featuresL = listOf(
    FeatureL(
        Icons.Outlined.Speed,
        L("دانلود چند-رشته‌ای", "Multi-Thread Downloading"),
        L(
            "فایل به چند بخش تقسیم و هم‌زمان دانلود می‌شود تا سرعت دانلود به‌خصوص روی اینترنت پرسرعت چند برابر شود. تعداد رشته‌ها از تنظیمات قابل تغییر است.",
            "Splits a file into multiple parts and downloads them simultaneously for much faster speeds on fast connections. Thread count is adjustable in settings."
        ),
        CyanPrimary
    ),
    FeatureL(
        Icons.Outlined.Layers,
        L("دانلود محدوده‌ای (ادامه دانلود)", "Range Download (Resume)"),
        L(
            "فقط بخش‌های ناقص فایل دوباره دانلود می‌شوند. اگر دانلود به هر دلیلی قطع شد، HAD از همان نقطه ادامه می‌دهد، نه از صفر.",
            "Only missing parts of a file are re-downloaded. If a download stops for any reason, HAD resumes exactly where it left off instead of restarting."
        ),
        PurpleAccent
    ),
    FeatureL(
        Icons.Outlined.RestartAlt,
        L("ذخیره خودکار نشست", "Session Saving"),
        L(
            "پیشرفت دانلود به‌صورت خودکار ذخیره می‌شود. حتی با بستن اپ یا ریستارت گوشی، دانلودها از همان جا ادامه پیدا می‌کنند.",
            "Download progress is saved automatically. Even after closing the app or rebooting your phone, downloads resume from the exact point they stopped."
        ),
        GreenSuccess
    ),
    FeatureL(
        Icons.Outlined.Queue,
        L("صف دانلود هوشمند", "Smart Download Queue"),
        L(
            "چند دانلود را با هم به صف اضافه کن؛ HAD خودش مدیریت می‌کند که چند مورد هم‌زمان اجرا شود و بقیه در صف منتظر بمانند.",
            "Add multiple downloads to a queue and HAD manages them automatically — a set number run at once while the rest wait their turn."
        ),
        OrangeWarn
    ),
    FeatureL(
        Icons.Outlined.Hub,
        L("پشتیبانی از لینک‌های جایگزین (Mirror)", "Mirror Support"),
        L(
            "می‌توانی لینک‌های پشتیبان برای همان فایل اضافه کنی. اگر یک لینک کند یا خراب باشد، HAD به‌طور خودکار به جایگزین سریع‌تر سوییچ می‌کند.",
            "Add backup URLs for the same file. If one link is slow or broken, HAD automatically switches to a faster working mirror."
        ),
        CyanPrimary
    ),
    FeatureL(
        Icons.Outlined.Memory,
        L("بافر تطبیقی", "Adaptive Buffer"),
        L(
            "مصرف حافظه بر اساس سرعت دانلود تنظیم می‌شود؛ بافر کوچک برای اتصال کند و بافر بزرگ برای اتصال سریع — برای روان ماندن گوشی.",
            "Memory usage adjusts automatically to download speed — small buffers for slow connections, larger ones for fast connections, keeping your phone smooth."
        ),
        PurpleAccent
    ),
    FeatureL(
        Icons.Outlined.OndemandVideo,
        L("دانلود استریم HLS", "HLS Stream Downloader"),
        L(
            "دانلود ویدیوهای استریمی با فرمت HLS (پلی‌لیست .m3u8). همه‌ی بخش‌های ویدیو دانلود و در یک فایل واحد ادغام می‌شوند تا آفلاین قابل تماشا باشد.",
            "Download HLS video streams (.m3u8 playlists). All segments are downloaded and merged into a single file you can watch offline."
        ),
        GreenSuccess
    ),
    FeatureL(
        Icons.Outlined.VpnLock,
        L("پشتیبانی از پروکسی", "Proxy Support"),
        L(
            "دانلودها را از طریق پروکسی HTTP یا SOCKS5 عبور بده. می‌توانی برای هر دانلود پروکسی جدا تعیین کنی یا یک پروکسی پیش‌فرض برای همه.",
            "Route downloads through HTTP or SOCKS5 proxies. Set a different proxy per download, or one global default for everything."
        ),
        OrangeWarn
    ),
    FeatureL(
        Icons.Outlined.Verified,
        L("بررسی صحت فایل (Checksum)", "Checksum Verification"),
        L(
            "بعد از پایان دانلود، صحت و سلامت فایل به‌صورت خودکار بررسی می‌شود (MD5, SHA-1, SHA-256, SHA-512) تا مطمئن شوی فایل خراب یا دستکاری‌شده نیست.",
            "After download completes, HAD automatically verifies file integrity (MD5, SHA-1, SHA-256, SHA-512) to ensure it isn't corrupted or tampered with."
        ),
        CyanPrimary
    ),
    FeatureL(
        Icons.Outlined.Schedule,
        L("زمان‌بند دانلود", "Download Scheduler"),
        L(
            "یک بازه‌ی زمانی برای اجرای دانلودها تعیین کن — مثلاً فقط شب‌ها وقتی خواب هستی. خارج از این بازه دانلودها مکث می‌کنند و خودکار ادامه می‌یابند.",
            "Set a time window for downloads to run — e.g. only at night while you sleep. Downloads pause outside the window and resume automatically."
        ),
        PurpleAccent
    ),
    FeatureL(
        Icons.Outlined.TravelExplore,
        L("استخراج‌کننده لینک", "Link Scraper"),
        L(
            "همه‌ی لینک‌های قابل‌دانلود یک صفحه‌ی وب را استخراج کن. فقط آدرس صفحه را بده و HAD فایل‌های مدیا، اسناد و سایر محتوای قابل‌دانلود را پیدا می‌کند.",
            "Extract all downloadable links from any web page. Just paste a URL and HAD finds media files, documents, and other downloadable content."
        ),
        GreenSuccess
    ),
    FeatureL(
        Icons.Outlined.Cookie,
        L("کوکی و هدر سفارشی", "Cookies & Headers"),
        L(
            "کوکی و هدر HTTP دلخواه به دانلودها اضافه کن — مناسب برای سایت‌هایی که نیاز به ورود دارند یا برای شبیه‌سازی یک مرورگر خاص.",
            "Add custom cookies and HTTP headers to downloads — useful for sites requiring login or for mimicking a specific browser request."
        ),
        OrangeWarn
    ),
    FeatureL(
        Icons.Outlined.Language,
        L("آرشیو وب (مشاهده آفلاین صفحات)", "Web Archive (Offline Pages)"),
        L(
            "کل یک وب‌سایت را برای مشاهده‌ی آفلاین ذخیره کن — همراه با HTML، CSS، جاوااسکریپت و تصاویر. می‌توانی فقط یک صفحه یا کل سایت تا چند هزار صفحه را ذخیره کنی.",
            "Save an entire website for offline viewing — including HTML, CSS, JavaScript, and images. Save just one page or crawl up to thousands of pages."
        ),
        PurpleAccent
    ),
    FeatureL(
        Icons.Outlined.Wifi,
        L("سرور دانلود ریموت", "Remote Download Server"),
        L(
            "گوشی‌ات را به یک وب‌سرور کوچک تبدیل کن. آدرس را در مرورگر کامپیوتر (در همان وای‌فای) باز کن و هر لینک دانلودی را مستقیم به گوشی بفرست.",
            "Turn your phone into a small web server. Open the address in your PC browser (same Wi-Fi) and send any download link straight to your phone."
        ),
        GreenSuccess
    ),
    FeatureL(
        Icons.Outlined.Share,
        L("پشتیبانی از تورنت و مگنت", "Torrent & Magnet Support"),
        L(
            "فایل‌های تورنت و لینک‌های مگنت را دانلود کن. HAD به peer‌ها متصل می‌شود، قطعات را موازی دانلود می‌کند و فایل نهایی را می‌سازد. انتخاب فایل‌ها هم امکان‌پذیر است.",
            "Download torrent files and magnet links. HAD connects to peers, downloads pieces in parallel, and assembles the final file. You can also select which files to grab."
        ),
        OrangeWarn
    ),
    FeatureL(
        Icons.Outlined.BarChart,
        L("آنالیتیکس دانلود", "Download Analytics"),
        L(
            "تاریخچه‌ی دانلودها و آمار مفید را ببین: حجم کل دانلودشده، میانگین و بیشینه‌ی سرعت، درصد موفقیت و حتی ساعت‌هایی که بیشتر دانلود می‌کنی.",
            "View your download history and useful stats: total downloaded, average and peak speed, success rate, and even which hours you download the most."
        ),
        CyanPrimary
    ),
    FeatureL(
        Icons.Outlined.Language,
        L("مرورگر داخلی", "Built-in Browser"),
        L(
            "یک مرورگر کامل داخل اپ. هر سایتی را باز کن؛ HAD به‌طور خودکار فایل‌های قابل‌دانلود (ویدیو، موزیک، سند و...) را تشخیص می‌دهد و اعلان می‌دهد.",
            "A full web browser inside the app. Browse any site and HAD automatically detects downloadable files (video, music, documents, etc.) with a notification."
        ),
        PurpleAccent
    ),
    FeatureL(
        Icons.Outlined.ContentPaste,
        L("تشخیص خودکار کلیپ‌بورد", "Clipboard Detection"),
        L(
            "لینک‌های کپی‌شده در کلیپ‌بورد به‌صورت خودکار شناسایی می‌شوند. اگر چند لینک کپی کنی، یک پنجره برای انتخاب کدام‌ها باز می‌شود.",
            "URLs copied to clipboard are automatically detected. If you copy multiple links, a dialog lets you pick which ones to download."
        ),
        GreenSuccess
    ),
    FeatureL(
        Icons.Outlined.Folder,
        L("تشخیص فایل تکراری", "Duplicate Detection"),
        L(
            "قبل از دانلود بررسی می‌شود که فایل از قبل وجود دارد یا نه. در صورت تکراری بودن می‌توانی تغییر نام بدهی، فایل قبلی را جای‌گزین کنی یا دانلود را رد کنی.",
            "Checks if a file already exists before downloading. On duplicates you can rename, overwrite the old file, or skip the download entirely."
        ),
        OrangeWarn
    )
)

private val techStackL = listOf(
    L("Kotlin", "Kotlin") to L("زبان اصلی برنامه‌نویسی اپ", "Main programming language of the app"),
    L("Kotlin Coroutines", "Kotlin Coroutines") to L("کارهای پس‌زمینه و عملیات ناهم‌زمان", "Background tasks and async operations"),
    L("Jetpack Compose", "Jetpack Compose") to L("فریم‌ورک مدرن رابط کاربری", "Modern declarative UI framework"),
    L("Hilt / Dagger", "Hilt / Dagger") to L("تزریق وابستگی برای مدیریت اجزای اپ", "Dependency injection for managing app components"),
    L("Room", "Room") to L("پایگاه‌داده محلی برای تاریخچه دانلودها", "Local database for download history"),
    L("Kotlin Flows", "Kotlin Flows") to L("به‌روزرسانی لحظه‌ای پیشرفت و سرعت", "Real-time updates for progress and speed"),
    L("WebView", "WebView") to L("مرورگر داخلی با تشخیص خودکار لینک", "Built-in browser with automatic link detection"),
    L("DataStore", "DataStore") to L("ذخیره‌سازی تنظیمات کاربر", "Storing user preferences")
)

private data class UsageStepL(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: L,
    val desc: L
)

private val usageStepsL = listOf(
    UsageStepL(
        Icons.Outlined.AddCircleOutline,
        L("شروع یک دانلود ساده", "Start a basic download"),
        L(
            "روی دکمه‌ی + پایین صفحه بزن، لینک را در فیلد URL وارد کن، در صورت نیاز تعداد رشته و محل ذخیره را تغییر بده و روی «Start» بزن. اگر فقط بخواهی به صف اضافه شود، «Queue» را بزن.",
            "Tap the + button at the bottom, paste the URL, optionally adjust thread count and save folder, then tap Start. Tap Queue instead if you just want to add it to the waiting list."
        )
    ),
    UsageStepL(
        Icons.Outlined.ContentPaste,
        L("چسباندن سریع از کلیپ‌بورد", "Quick paste from clipboard"),
        L(
            "وقتی لینکی را کپی می‌کنی، آیکن چسباندن در بالای صفحه یا یک پنجره‌ی پیشنهادی ظاهر می‌شود. با یک ضربه می‌توانی همان‌جا دانلود را شروع کنی یا به صف اضافه کنی.",
            "When you copy a link, the paste icon at the top or a suggestion dialog appears. With one tap you can start the download immediately or queue it."
        )
    ),
    UsageStepL(
        Icons.Outlined.OndemandVideo,
        L("دانلود ویدیوی استریم (HLS)", "Downloading a stream (HLS)"),
        L(
            "اگر لینک شامل .m3u8 باشد، حالت HLS به‌طور خودکار انتخاب می‌شود. کافی است Start را بزنی؛ بقیه‌ی کار به‌صورت خودکار انجام می‌شود و یک فایل واحد قابل پخش به دست می‌آید.",
            "If the link contains .m3u8, HLS mode is selected automatically. Just tap Start — everything else happens automatically and you get a single playable file."
        )
    ),
    UsageStepL(
        Icons.Outlined.TravelExplore,
        L("استفاده از استخراج‌کننده لینک", "Using the link scraper"),
        L(
            "روی آیکن کره‌ی زمین (بالای دکمه‌ی +) بزن، آدرس صفحه را وارد کن و روی «Scrape Links» بزن. از لیست نتایج، لینک‌های دلخواه را انتخاب و دانلود یا به صف اضافه کن.",
            "Tap the globe icon (above the + button), enter the page URL, then tap Scrape Links. Select the links you want from the results and download or queue them."
        )
    ),
    UsageStepL(
        Icons.Outlined.Language,
        L("استفاده از مرورگر داخلی", "Using the built-in browser"),
        L(
            "از منوی کناری وارد بخش «Browser» شو. هر سایتی را باز کن؛ وقتی فایلی قابل‌دانلود تشخیص داده شود، نشانه‌ای (badge) ظاهر می‌شود. روی آن بزن تا لیست فایل‌های پیدا‌شده را ببینی و دانلود کنی.",
            "Open the Browser section from the side menu. Browse any site; when a downloadable file is detected, a badge appears. Tap it to see the list of found files and download them."
        )
    ),
    UsageStepL(
        Icons.Outlined.Schedule,
        L("زمان‌بندی دانلودها", "Scheduling downloads"),
        L(
            "هنگام افزودن دانلود، بخش Schedule را باز کن و تاریخ/ساعت شروع را تعیین کن، یا در Settings یک بازه‌ی زمانی پیش‌فرض برای همه‌ی دانلودها تنظیم کن.",
            "When adding a download, open the Schedule section and set a start date/time, or set a default time window for all downloads in Settings."
        )
    ),
    UsageStepL(
        Icons.Outlined.VpnLock,
        L("استفاده از پروکسی و هدرهای سفارشی", "Using proxy and custom headers"),
        L(
            "در بخش «Advanced Options» پنجره‌ی افزودن دانلود، پروکسی، کوکی، هدر و User-Agent دلخواه را وارد کن. این تنظیمات فقط روی همان دانلود اعمال می‌شود.",
            "In the Advanced Options of the add-download dialog, enter your proxy, cookies, headers, and custom User-Agent. These settings apply only to that download."
        )
    ),
    UsageStepL(
        Icons.Outlined.Share,
        L("افزودن تورنت یا مگنت", "Adding a torrent or magnet"),
        L(
            "از منوی کناری وارد بخش «Torrent» شو، فایل .torrent را انتخاب کن یا لینک مگنت را بچسبان، فایل‌های مورد نظر را تیک بزن و دانلود را شروع کن.",
            "Open the Torrent section from the side menu, pick a .torrent file or paste a magnet link, check the files you want, and start the download."
        )
    ),
    UsageStepL(
        Icons.Outlined.Wifi,
        L("ارسال لینک از کامپیوتر", "Sending links from your PC"),
        L(
            "از منوی کناری وارد «Remote Server» شو و سرور را روشن کن. آدرس نمایش‌داده‌شده را در مرورگر کامپیوتر (در همان وای‌فای گوشی) باز کن و لینک‌ها را مستقیم به گوشی بفرست.",
            "Open Remote Server from the side menu and start it. Open the shown address in your PC's browser (same Wi-Fi as your phone) and send links straight to your phone."
        )
    ),
    UsageStepL(
        Icons.Outlined.BarChart,
        L("مشاهده آمار و تاریخچه", "Checking stats and history"),
        L(
            "بخش «Analytics» در منوی کناری، آمار کلی، نمودار ماهانه، توزیع ساعتی دانلودها و تاریخچه‌ی اخیر را نمایش می‌دهد.",
            "The Analytics section in the side menu shows overall stats, a monthly chart, hourly distribution, and recent download history."
        )
    ),
    UsageStepL(
        Icons.Outlined.Settings,
        L("تنظیم پیش‌فرض‌های اپ", "Setting app defaults"),
        L(
            "از منوی کناری وارد «Settings» شو تا مقادیر پیش‌فرض رشته، پوشه‌ی ذخیره، تعداد دانلود هم‌زمان، پروکسی، سرعت محدود و بازه‌ی زمانی را تنظیم کنی.",
            "Open Settings from the side menu to configure default thread count, save folder, concurrent downloads, proxy, speed limit, and the default time window."
        )
    ),
    UsageStepL(
        Icons.Outlined.Delete,
        L("مدیریت دانلودها (حذف، توقف، ادامه)", "Managing downloads (delete, stop, resume)"),
        L(
            "با نگه‌داشتن انگشت روی یک دانلود، حالت انتخاب چندتایی فعال می‌شود. هر دانلود را می‌توانی متوقف، حذف (با یا بدون فایل) یا دوباره تلاش کنی.",
            "Long-press a download to enter multi-select mode. Each download can be stopped, deleted (with or without the file), or retried individually."
        )
    )
)

@Composable
fun AboutTab() {
    var lang by remember { mutableStateOf(AppLang.EN) }
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val isFa = lang == AppLang.FA

    val rotate by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "spin"
    )

    val layoutDir = if (isFa) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpaceBlack)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanGlow, Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.1f),
                        radius = size.width * 0.6f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurpleAccent.copy(alpha = 0.05f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.7f),
                        radius = size.width * 0.5f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                LanguageSwitcher(lang = lang, onChange = { lang = it })

                Spacer(Modifier.height(20.dp))

                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(110.dp)) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(CyanPrimary.copy(alpha = 0.8f), CyanPrimary.copy(alpha = 0f))
                            ),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .rotate(rotate)
                            .background(
                                Brush.linearGradient(listOf(CyanPrimary, PurpleAccent)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {}
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(SpaceBlack, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "H",
                            color = CyanPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "HAD",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    L("هایپر ادونسد دانلودر", "Hyper Advanced Downloader").get(lang),
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VersionBadge("v2.0.0", CyanPrimary)
                    VersionBadge(L("اندروید ۸ به بالا", "Android 8+").get(lang), GreenSuccess)
                    VersionBadge("Kotlin 100%", PurpleAccent)
                }

                Spacer(Modifier.height(24.dp))

                AuthorCard(lang = lang, context = context)

                Spacer(Modifier.height(20.dp))

                DonationCard(lang = lang)

                Spacer(Modifier.height(24.dp))

                SectionHeader(L("راهنمای استفاده از اپ", "HOW TO USE THE APP").get(lang))

                usageStepsL.forEachIndexed { i, step ->
                    UsageStepRow(step, lang, i)
                    if (i < usageStepsL.size - 1) {
                        HorizontalDivider(
                            color = BorderColor.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                SectionHeader(L("امکانات", "FEATURES").get(lang))

                featuresL.forEachIndexed { i, feature ->
                    FeatureRow(feature, lang, i)
                    if (i < featuresL.size - 1) {
                        HorizontalDivider(
                            color = BorderColor.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                TechStackSection(lang)

                Spacer(Modifier.height(24.dp))

                Text(
                    L("ساخته‌شده با ❤ به‌طور کامل با Kotlin و Jetpack Compose", "Built with ❤ entirely in Kotlin + Jetpack Compose").get(lang),
                    color = TextMuted,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    L("یک اپلیکیشن کاملاً اندرویدی و Kotlin-محور", "A fully Kotlin-based native Android app").get(lang),
                    color = TextMuted.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun LanguageSwitcher(lang: AppLang, onChange: (AppLang) -> Unit) {
    Row(
        modifier = Modifier
            .background(ElevatedSurf, RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(AppLang.FA to "فارسی", AppLang.EN to "English").forEach { (l, label) ->
            val selected = lang == l
            Box(
                modifier = Modifier
                    .clickable { onChange(l) }
                    .background(
                        if (selected) CyanPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    label,
                    color = if (selected) CyanPrimary else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun AuthorCard(lang: AppLang, context: Context) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            Brush.linearGradient(listOf(CyanPrimary, PurpleAccent)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("H", color = SpaceBlack, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        L("درباره HAD", "About HAD").get(lang),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        L("مدیر دانلود", "Download Manager").get(lang),
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        L("اپلیکیشن اندروید", "Android App").get(lang),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                L(
                    "HAD یک مدیر دانلود کامل برای اندروید است که به تو کمک می‌کند هر چیزی را از اینترنت دانلود کنی — ویدیو، فایل بزرگ، تورنت یا حتی یک وب‌سایت کامل؛ HAD همه را با سرعت و قابلیت اطمینان مدیریت می‌کند.\n\n" +
                            "تمام بخش‌های اپ با زبان Kotlin نوشته شده و از فناوری‌های مدرن اندروید مثل Coroutines برای پردازش پس‌زمینه، Compose برای رابط کاربری و Room برای مدیریت تاریخچه دانلودها استفاده می‌کند. هدف، ساخت اپی سریع، کم‌مصرف و ساده برای همه است.\n\n" +
                            "چه یک کاربر عادی باشی که فقط یک فیلم دانلود می‌کنی، چه یک کاربر حرفه‌ای که ده‌ها فایل را هم‌زمان مدیریت می‌کنی، HAD کنترل کامل هر دانلود را با یک رابط کاربری تمیز در دستت می‌گذارد.",
                    "HAD is a complete download manager for Android that helps you download anything from the internet — videos, large files, torrents, or even an entire website — all handled with speed and reliability.\n\n" +
                            "The entire app is written in Kotlin and uses modern Android technologies such as Coroutines for background processing, Compose for the user interface, and Room for managing download history. The goal is a fast, battery-friendly, and easy-to-use app for everyone.\n\n" +
                            "Whether you're a casual user grabbing a single movie or a power user juggling dozens of files at once, HAD gives you full control over every download through a clean, intuitive interface."
                ).get(lang),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 19.sp
            )

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SocialChip(
                    icon = Icons.Outlined.Code,
                    label = "GitHub",
                    url = GITHUB_URL,
                    context = context
                )
                SocialChip(
                    icon = Icons.Outlined.VolunteerActivism,
                    label = L("دونیت", "Donate").get(lang),
                    url = DONATE_URL,
                    context = context
                )
                SocialChip(
                    icon = Icons.Outlined.Email,
                    label = L("ارتباط با ما", "Contact").get(lang),
                    url = "mailto:$CONTACT_EMAIL",
                    context = context
                )
            }
        }
    }
}

@Composable
private fun DonationCard(lang: AppLang) {
    val context = LocalContext.current
    var copiedWallet by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedWallet) {
        if (copiedWallet != null) {
            delay(2000L)
            copiedWallet = null
        }
    }

    fun copyToClipboard(label: String, value: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, value))
        copiedWallet = label
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, OrangeWarn.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(OrangeWarn.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.VolunteerActivism, null, tint = OrangeWarn, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        L("حمایت ارزی از پروژه", "Support the project").get(lang),
                        color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold
                    )
                    Text(
                        L(
                            "اگه HAD به دردت خورد، می‌تونی با ارز دیجیتال حمایتش کنی",
                            "If HAD has been useful, you can support it with crypto"
                        ).get(lang),
                        color = TextMuted, fontSize = 11.sp
                    )
                }
            }

            WalletRow(
                label = "Bitcoin (BTC)",
                address = BTC_WALLET,
                color = OrangeWarn,
                copied = copiedWallet == "BTC",
                copyHint = L("کپی شد!", "Copied!").get(lang),
                tapHint = L("برای کپی ضربه بزن", "Tap to copy").get(lang),
                onCopy = { copyToClipboard("BTC", BTC_WALLET) }
            )

            WalletRow(
                label = "Tron (TRX)",
                address = TRX_WALLET,
                color = RedError,
                copied = copiedWallet == "TRX",
                copyHint = L("کپی شد!", "Copied!").get(lang),
                tapHint = L("برای کپی ضربه بزن", "Tap to copy").get(lang),
                onCopy = { copyToClipboard("TRX", TRX_WALLET) }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Info, null, tint = TextMuted, modifier = Modifier.size(12.dp))
                Text(
                    L(
                        "هر کمکی، هرچند کوچک، باعث ادامه‌ی توسعه‌ی این پروژه می‌شود.",
                        "Every contribution, however small, helps keep this project alive."
                    ).get(lang),
                    color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
private fun WalletRow(
    label: String,
    address: String,
    color: Color,
    copied: Boolean,
    copyHint: String,
    tapHint: String,
    onCopy: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy() },
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, color.copy(alpha = if (copied) 0.6f else 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.CurrencyBitcoin, null, tint = color, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    address,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            AnimatedContent(
                targetState = copied,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "walletCopyState"
            ) { isCopied ->
                if (isCopied) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Check, null, tint = GreenSuccess, modifier = Modifier.size(14.dp))
                        Text(copyHint, color = GreenSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.ContentCopy, null, tint = color.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Text(tapHint, color = color.copy(alpha = 0.7f), fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageStepRow(step: UsageStepL, lang: AppLang, index: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(CyanPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(step.icon, null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                step.title.get(lang),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                step.desc.get(lang),
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
        }
        Text(
            "%02d".format(index + 1),
            color = CyanPrimary.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun FeatureRow(feature: FeatureL, lang: AppLang, index: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(feature.color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(feature.icon, null, tint = feature.color, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                feature.title.get(lang),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                feature.desc.get(lang),
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }

        Text(
            "%02d".format(index + 1),
            color = TextMuted.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            fontFamily = FontFamily.Monospace
        )
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = BorderColor,
            thickness = 0.5.dp
        )
    }
}

@Composable
private fun TechStackSection(lang: AppLang) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ElevatedSurf,
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                L("فناوری‌های استفاده‌شده", "TECHNOLOGY").get(lang),
                color = TextMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(10.dp))

            techStackL.forEach { (tech, desc) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        tech.get(lang),
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        desc.get(lang),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(0.6f),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            Text(
                L(
                    "این اپ به‌طور کامل با Kotlin نوشته شده — هیچ بخشی از کد به زبان دیگری نیست. همه‌ی منطق برنامه، از مدیریت دانلودها تا رابط کاربری، در یک پروژه‌ی Kotlin خالص قرار دارد.",
                    "This app is written entirely in Kotlin — no other language is used anywhere in the codebase. Every part of the logic, from download management to the UI, lives in a single pure Kotlin project."
                ).get(lang),
                color = TextMuted,
                fontSize = 10.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun VersionBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun SocialChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    url: String,
    context: Context
) {
    Row(
        modifier = Modifier
            .background(ElevatedSurf, RoundedCornerShape(8.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, null, tint = CyanPrimary, modifier = Modifier.size(13.dp))
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}