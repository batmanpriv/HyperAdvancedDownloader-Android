package com.had.downloader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.had.downloader.ui.theme.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
private enum class GuideLang { FA, EN }
private data class GL(val fa: String, val en: String)
private fun GL.get(lang: GuideLang) = if (lang == GuideLang.FA) fa else en

private data class GuideStepL(val fa: String, val en: String)
private data class GuideSectionL(
    val title: GL,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color,
    val category: GL,
    val steps: List<GuideStepL>,
    val tip: GL? = null
)

private val guideSectionsL = listOf(

    GuideSectionL(
        title    = GL("دانلود ساده با HTTP", "Basic HTTP Download"),
        icon     = Icons.Outlined.CloudDownload,
        color    = Color(0xFF00D4FF),
        category = GL("شروع", "GETTING STARTED"),
        steps = listOf(
            GuideStepL(
                "روی دکمه‌ی + (گوشه‌ی پایین راست) ضربه بزن تا پنجره‌ی افزودن دانلود باز شود.",
                "Tap the + button (bottom-right corner) to open the New Download dialog."
            ),
            GuideStepL(
                "آدرس مستقیم فایل را در فیلد URL وارد یا پیست کن. HAD به محض وارد کردن آدرس، اطلاعات فایل مثل حجم و نام را به‌صورت خودکار می‌گیرد.",
                "Paste the direct file URL into the URL field. HAD automatically fetches file info (size, filename) as soon as you enter the link."
            ),
            GuideStepL(
                "حالت HTTP پیش‌فرض است. برای فایل‌های بزرگ حالت MULTI را انتخاب کن تا با چند رشته هم‌زمان دانلود شود.",
                "HTTP mode is selected by default. For large files, switch to MULTI mode to download with multiple parallel threads."
            ),
            GuideStepL(
                "تعداد رشته (thread) را تنظیم کن — پیش‌فرض ۴ رشته است. روی اتصال‌های سریع‌تر می‌توانی تا ۱۶ رشته استفاده کنی.",
                "Adjust the thread count — default is 4. On faster connections you can go up to 16 threads."
            ),
            GuideStepL(
                "پوشه‌ی ذخیره‌سازی را مشخص کن یا از آیکون پوشه برای انتخاب مستقیم استفاده کن.",
                "Set the save folder or tap the folder icon to pick a directory from your storage."
            ),
            GuideStepL(
                "روی Start بزن تا دانلود فوراً شروع شود، یا Queue تا به صف اضافه شود.",
                "Tap Start to begin immediately, or Queue to add it to the waiting list."
            ),
        ),
        tip = GL(
            "اگر HAD حجم فایل را نتوانست بخواند، هنوز هم دانلود کار می‌کند — فقط نوار پیشرفت درصد نشان نمی‌دهد.",
            "If HAD can't read the file size, download still works — the progress bar just won't show a percentage."
        )
    ),

    GuideSectionL(
        title    = GL("دانلود پیوسته (ادامه دانلود)", "Resumable Downloads"),
        icon     = Icons.Outlined.Restore,
        color    = Color(0xFF00FF88),
        category = GL("شروع", "GETTING STARTED"),
        steps = listOf(
            GuideStepL(
                "HAD به‌صورت خودکار چک می‌کند آیا سرور از ادامه‌ی دانلود (Range Request) پشتیبانی می‌کند یا خیر. این اطلاعات در کارت فایل نشان داده می‌شود (برچسب «Resumable»).",
                "HAD automatically checks if the server supports range requests. This appears in the file info card as a 'Resumable' badge."
            ),
            GuideStepL(
                "اگر دانلود به هر دلیلی (اینترنت قطع شد، اپ بسته شد، گوشی ریستارت شد) متوقف شد، فقط Retry بزن. HAD از همان نقطه‌ای که متوقف شد ادامه می‌دهد.",
                "If a download stops for any reason (no internet, app closed, phone rebooted), just tap Retry. HAD resumes exactly where it left off."
            ),
            GuideStepL(
                "فایل نشست (session) به‌صورت خودکار در حافظه‌ی داخلی ذخیره می‌شود. تا زمانی که دانلود کامل نشده، این فایل را پاک نکن.",
                "The session file is saved automatically in internal storage. Don't clear app data while a download is incomplete."
            ),
            GuideStepL(
                "گزینه‌ی «Resume on retry» در Advanced Options باید فعال باشد (پیش‌فرض: فعال). غیرفعال کردن آن دانلود را از صفر شروع می‌کند.",
                "The 'Resume on retry' toggle in Advanced Options must be enabled (on by default). Turning it off restarts from zero."
            ),
        ),
        tip = GL(
            "بعد از ریستارت گوشی، HAD به‌صورت خودکار دانلودهایی که در حال اجرا بودند را دوباره شروع می‌کند.",
            "After a phone reboot, HAD automatically restarts downloads that were running before the reboot."
        )
    ),

    GuideSectionL(
        title    = GL("دانلود استریم HLS / M3U8", "HLS / M3U8 Stream Download"),
        icon     = Icons.Outlined.OndemandVideo,
        color    = Color(0xFF9B59FF),
        category = GL("پیشرفته", "ADVANCED"),
        steps = listOf(
            GuideStepL(
                "اگر لینک حاوی .m3u8 باشد، HAD به‌صورت خودکار حالت HLS را انتخاب می‌کند. نیازی به تغییر دستی نیست.",
                "If the URL contains .m3u8, HAD auto-selects HLS mode. No manual switching needed."
            ),
            GuideStepL(
                "HAD ابتدا پلی‌لیست مستر را می‌خواند. اگر چند کیفیت مختلف وجود داشته باشد (۱۰۸۰p، ۷۲۰p و...)، به‌صورت خودکار بالاترین کیفیت را انتخاب می‌کند.",
                "HAD first reads the master playlist. If multiple qualities exist (1080p, 720p...), it automatically picks the highest bandwidth stream."
            ),
            GuideStepL(
                "تمام سگمنت‌های ویدیو به‌صورت موازی دانلود می‌شوند. در کارت دانلود تعداد سگمنت‌های دانلودشده نشان داده می‌شود (مثلاً ۴۷ / ۱۲۰ segs).",
                "All video segments are downloaded in parallel. The download card shows completed segment count (e.g. 47/120 segs)."
            ),
            GuideStepL(
                "بعد از اتمام دانلود، سگمنت‌ها به‌صورت خودکار ادغام می‌شوند (مرحله‌ی MERGING) و یک فایل .mp4 نهایی ساخته می‌شود. این مرحله ممکن است چند دقیقه طول بکشد.",
                "After all segments download, they're automatically merged (MERGING phase) into a single .mp4 file. This phase may take a few minutes."
            ),
            GuideStepL(
                "اگر ویدیو رمزگذاری شده باشد (AES-128 یا SAMPLE-AES)، HAD کلید رمزنگاری را می‌گیرد و هر سگمنت را قبل از ذخیره رمزگشایی می‌کند.",
                "If the stream is encrypted (AES-128 or SAMPLE-AES), HAD fetches the decryption key and decrypts each segment before saving."
            ),
            GuideStepL(
                "استریم‌های زنده (Live HLS) هم پشتیبانی می‌شوند. HAD سگمنت‌های جدید را به‌صورت پیوسته دانلود می‌کند تا وقتی که استریم به پایان برسد یا دانلود را متوقف کنی.",
                "Live HLS streams are supported too. HAD continuously downloads new segments until the stream ends or you stop the download."
            ),
        ),
        tip = GL(
            "اگر فایل نهایی قابل پخش نبود، HAD یک تبدیل جایگزین انجام می‌دهد. در بدترین حالت یک فایل .ts ذخیره می‌شود که اکثر پلیرها می‌توانند بازش کنند.",
            "If the final file isn't playable, HAD attempts an alternate conversion. Worst case it saves a .ts file that most media players support."
        )
    ),

    GuideSectionL(
        title    = GL("زمان‌بندی دانلود", "Download Scheduling"),
        icon     = Icons.Outlined.Schedule,
        color    = Color(0xFFFF8C42),
        category = GL("پیشرفته", "ADVANCED"),
        steps = listOf(
            GuideStepL(
                "هنگام افزودن دانلود، بخش Advanced Options را باز کن. فیلد Schedule را می‌بینی.",
                "When adding a download, expand Advanced Options and look for the Schedule section."
            ),
            GuideStepL(
                "تاریخ را در فرمت YYYY-MM-DD وارد کن (اختیاری) یا فقط ساعت شروع را بنویس.",
                "Enter a date in YYYY-MM-DD format (optional), or just set a start time."
            ),
            GuideStepL(
                "از چیپ‌های پیش‌تنظیم استفاده کن: «Tonight» (این شب)، «Midnight» (نیمه شب)، «Dawn» (سحر). بدون نیاز به تایپ دستی.",
                "Use the preset chips: Tonight, Midnight, Dawn — no manual typing needed."
            ),
            GuideStepL(
                "دانلود با زمان‌بندی در لیست با وضعیت QUEUED و نمایشگر شمارش معکوس نشان داده می‌شود.",
                "Scheduled downloads appear in the list with QUEUED status and a countdown timer."
            ),
            GuideStepL(
                "برای زمان‌بندی همه‌ی دانلودها، در Settings بازه‌ی زمانی پیش‌فرض را تنظیم کن (مثلاً ۲۲:۰۰ تا ۰۶:۰۰). دانلودهایی که خارج از این بازه باشند به‌صورت خودکار مکث می‌کنند.",
                "To schedule all downloads, set a default time window in Settings (e.g. 22:00 to 06:00). Downloads outside this window automatically pause."
            ),
            GuideStepL(
                "AlarmManager حتی در حالت Doze (خواب عمیق گوشی) هم کار می‌کند — دانلودهای زمان‌بندی‌شده در ساعت مقرر استارت می‌زنند.",
                "AlarmManager works even in Doze mode — scheduled downloads fire at the exact set time."
            ),
        ),
        tip = GL(
            "اگر گوشی در ساعت زمان‌بندی‌شده خاموش باشد، بعد از روشن شدن HAD به‌صورت خودکار دانلودهای باقیمانده را شروع می‌کند.",
            "If the phone is off at the scheduled time, HAD automatically starts missed downloads once the phone boots."
        )
    ),

    GuideSectionL(
        title    = GL("لینک‌های جایگزین (Mirror)", "Mirror / Fallback URLs"),
        icon     = Icons.Outlined.Hub,
        color    = Color(0xFF00D4FF),
        category = GL("پیشرفته", "ADVANCED"),
        steps = listOf(
            GuideStepL(
                "در Advanced Options بخش «Mirror URLs» را پیدا کن. یک لینک در هر خط وارد کن.",
                "In Advanced Options find the 'Mirror URLs' section. Enter one URL per line."
            ),
            GuideStepL(
                "HAD هنگام شروع دانلود، تمام لینک‌های اصلی و جایگزین را با هم تست می‌کند (latency probe) و سریع‌ترین را انتخاب می‌کند.",
                "At download start, HAD probes all mirrors simultaneously for latency and picks the fastest responding one."
            ),
            GuideStepL(
                "اگر لینک اصلی در حین دانلود کند یا قطع شود، HAD به‌صورت خودکار به سریع‌ترین جایگزین سوییچ می‌کند.",
                "If the primary URL becomes slow or unreachable mid-download, HAD automatically switches to the fastest available mirror."
            ),
            GuideStepL(
                "میرورها فقط برای دانلودهای HTTP/MULTI کار می‌کنند، نه برای HLS.",
                "Mirrors work for HTTP/MULTI downloads only, not for HLS streams."
            ),
        ),
        tip = GL(
            "از سایت‌هایی مثل archive.org یا cdnjs یا سرورهای مستقل می‌توانی آدرس‌های Mirror پیدا کنی.",
            "Look for mirror URLs on archive.org, cdnjs, or the project's official mirror list."
        )
    ),

    GuideSectionL(
        title    = GL("بررسی صحت فایل (Checksum)", "Checksum / Hash Verification"),
        icon     = Icons.Outlined.Verified,
        color    = Color(0xFF00FF88),
        category = GL("پیشرفته", "ADVANCED"),
        steps = listOf(
            GuideStepL(
                "در Advanced Options فیلدهای «Hash algo» و «Expected hash» را پیدا کن.",
                "In Advanced Options find the 'Hash algo' and 'Expected hash' fields."
            ),
            GuideStepL(
                "الگوریتم را وارد کن: MD5 ، SHA-1 ، SHA-256 یا SHA-512. بزرگ و کوچک بودن حروف مهم نیست.",
                "Enter the algorithm: MD5, SHA-1, SHA-256, or SHA-512. Case doesn't matter."
            ),
            GuideStepL(
                "هش مورد انتظار را که معمولاً در صفحه‌ی دانلود سایت درج است در فیلد Expected hash وارد کن.",
                "Enter the expected hash string — usually listed on the download page next to the file."
            ),
            GuideStepL(
                "بعد از پایان دانلود، وضعیت VERIFYING نشان داده می‌شود. اگر هش صحیح باشد COMPLETED، وگرنه FAILED با پیام CHECKSUM_FAIL نمایش داده می‌شود.",
                "After download, status shows VERIFYING. If the hash matches it becomes COMPLETED; otherwise FAILED with a CHECKSUM_FAIL message."
            ),
        ),
        tip = GL(
            "SHA-256 امن‌ترین و رایج‌ترین گزینه است. MD5 فقط برای سازگاری با سرورهای قدیمی استفاده کن.",
            "SHA-256 is the safest and most common choice. Use MD5 only for compatibility with legacy servers."
        )
    ),

    GuideSectionL(
        title    = GL("پروکسی و VPN", "Proxy & VPN"),
        icon     = Icons.Outlined.VpnLock,
        color    = Color(0xFFFF8C42),
        category = GL("پیشرفته", "ADVANCED"),
        steps = listOf(
            GuideStepL(
                "در Advanced Options فیلد Proxy را پیدا کن.",
                "In Advanced Options find the Proxy field."
            ),
            GuideStepL(
                "فرمت HTTP پروکسی:   http://host:port",
                "HTTP proxy format:   http://host:port"
            ),
            GuideStepL(
                "فرمت SOCKS5 پروکسی: socks5://host:port",
                "SOCKS5 proxy format: socks5://host:port"
            ),
            GuideStepL(
                "پروکسی فقط برای همان دانلود اعمال می‌شود. برای پروکسی پیش‌فرض روی همه‌ی دانلودها، در Settings > Network آن را تنظیم کن.",
                "Proxy applies only to that specific download. For a global default proxy, set it in Settings > Network."
            ),
            GuideStepL(
                "اگر از VPN سیستم اندروید استفاده می‌کنی، نیازی به تنظیم پروکسی در HAD نیست — ترافیک به‌صورت خودکار از VPN عبور می‌کند.",
                "If using Android's system-level VPN, no need to set a proxy in HAD — traffic routes through it automatically."
            ),
        ),
        tip = GL(
            "برای پروکسی‌هایی که نیاز به احراز هویت دارند: http://user:password@host:port",
            "For proxies with authentication: http://user:password@host:port"
        )
    ),

    GuideSectionL(
        title    = GL("کوکی و هدرهای سفارشی", "Cookies & Custom Headers"),
        icon     = Icons.Outlined.Cookie,
        color    = Color(0xFF9B59FF),
        category = GL("پیشرفته", "ADVANCED"),
        steps = listOf(
            GuideStepL(
                "کوکی‌ها برای سایت‌هایی که نیاز به لاگین دارند ضروری هستند (مثل سایت‌های اشتراکی).",
                "Cookies are essential for sites requiring login (streaming subscriptions, paywalled content)."
            ),
            GuideStepL(
                "برای گرفتن کوکی: در مرورگر کامپیوتر، DevTools را باز کن (F12) > Network > روی یکی از درخواست‌ها کلیک کن > هدر Cookie را کپی کن.",
                "To get cookies: open DevTools in PC browser (F12) → Network → click any request → copy the Cookie header value."
            ),
            GuideStepL(
                "کوکی‌ها را در فیلد Cookies به فرمت name=value; name2=value2 وارد کن.",
                "Paste cookies in the Cookies field using format: name=value; name2=value2"
            ),
            GuideStepL(
                "برای هدرهای سفارشی، هر هدر را در یک خط جداگانه بنویس: Authorization: Bearer xxx",
                "For custom headers, write each on a separate line: Authorization: Bearer xxx"
            ),
            GuideStepL(
                "User-Agent را می‌توانی برای شبیه‌سازی مرورگرهای مختلف تغییر دهی. بعضی سرورها فقط به User-Agent مرورگرهای خاص پاسخ می‌دهند.",
                "Change User-Agent to mimic different browsers. Some servers only respond to specific browser user agents."
            ),
        ),
        tip = GL(
            "مرورگر داخلی HAD کوکی‌ها را به‌صورت خودکار به دانلودهای شناسایی‌شده اضافه می‌کند.",
            "HAD's built-in browser automatically attaches cookies to intercepted download requests."
        )
    ),

    GuideSectionL(
        title    = GL("استخراج‌کننده‌ی لینک", "Link Scraper"),
        icon     = Icons.Outlined.TravelExplore,
        color    = Color(0xFF00D4FF),
        category = GL("ابزار", "TOOLS"),
        steps = listOf(
            GuideStepL(
                "روی آیکن کره‌ی زمین (بالای دکمه‌ی +) ضربه بزن تا پنجره‌ی Link Scraper باز شود.",
                "Tap the globe icon (above the + button) to open the Link Scraper dialog."
            ),
            GuideStepL(
                "آدرس صفحه‌ای که می‌خواهی از آن لینک استخراج کنی را وارد کن و Scrape Links را بزن.",
                "Enter the page URL you want to scrape and tap Scrape Links."
            ),
            GuideStepL(
                "HAD HTML صفحه را تجزیه می‌کند و همه‌ی لینک‌های قابل دانلود — ویدیو، موزیک، اسناد، آرشیو، HLS و... — را استخراج می‌کند.",
                "HAD parses the HTML and extracts all downloadable links — video, audio, documents, archives, HLS, torrents and more."
            ),
            GuideStepL(
                "می‌توانی از فیلترهای نوع (VIDEO، AUDIO، ARCHIVE و...) یا فیلتر پسوند استفاده کنی تا لینک‌های مورد نظر را سریع پیدا کنی.",
                "Use type filters (VIDEO, AUDIO, ARCHIVE...) or extension filter to quickly find specific links."
            ),
            GuideStepL(
                "لینک‌های مورد نظر را تیک بزن و Download یا Queue را بزن. HAD تعداد مشخصی را هم‌زمان دانلود می‌کند و بقیه را در صف نگه می‌دارد.",
                "Check the links you want and tap Download or Queue. HAD downloads up to the concurrent limit simultaneously and queues the rest."
            ),
            GuideStepL(
                "روی آیکن + کنار هر لینک بزن تا مستقیم به پنجره‌ی افزودن دانلود برود و بتوانی تنظیمات بیشتری انجام دهی.",
                "Tap the + icon next to any link to open it in the New Download dialog for custom configuration."
            ),
        ),
        tip = GL(
            "بعضی سایت‌ها لینک‌ها را با JavaScript بارگذاری می‌کنند. برای این‌گونه سایت‌ها از مرورگر داخلی HAD استفاده کن که JavaScript را اجرا می‌کند.",
            "Some sites load links via JavaScript. For these, use HAD's built-in browser which executes JavaScript and intercepts requests."
        )
    ),

    GuideSectionL(
        title    = GL("مرورگر داخلی", "Built-in Browser"),
        icon     = Icons.Outlined.Language,
        color    = Color(0xFF9B59FF),
        category = GL("ابزار", "TOOLS"),
        steps = listOf(
            GuideStepL(
                "از منوی کناری (آیکن ≡) وارد بخش Browser شو.",
                "Open the side menu (≡ icon) and tap Browser."
            ),
            GuideStepL(
                "هر سایتی را باز کن. HAD در پس‌زمینه همه‌ی درخواست‌های شبکه را رصد می‌کند و فایل‌های قابل دانلود را شناسایی می‌کند.",
                "Browse any site. HAD monitors all network requests in the background and identifies downloadable files."
            ),
            GuideStepL(
                "وقتی فایلی شناسایی شود، نشانه‌ی بنفش (↓) با تعداد فایل‌های پیداشده پایین صفحه ظاهر می‌شود.",
                "When files are detected, a purple badge (↓) appears at the bottom with the count of found files."
            ),
            GuideStepL(
                "روی نشانه بزن تا پنل «Detected Files» باز شود. می‌توانی هر فایل را جداگانه یا همه را با هم دانلود کنی.",
                "Tap the badge to open the Detected Files panel. Download each file individually or tap 'All ↓' to grab everything."
            ),
            GuideStepL(
                "حالت Incognito: تاریخچه، کوکی و کش ذخیره نمی‌شود. از منوی سه‌نقطه‌ای (⋮) فعال کن.",
                "Incognito mode: no history, cookies, or cache are saved. Enable via the three-dot menu (⋮)."
            ),
            GuideStepL(
                "Desktop Site: User-Agent به مرورگر کامپیوتر تغییر می‌کند. بعضی سایت‌ها فقط در نسخه‌ی دسکتاپ لینک دانلود نشان می‌دهند.",
                "Desktop Site: changes User-Agent to a desktop browser. Some sites only show download links in desktop mode."
            ),
            GuideStepL(
                "می‌توانی چند تب (tab) هم‌زمان داشته باشی. آیکن شماره‌ی تب‌ها را نشان می‌دهد. برای مدیریت تب‌ها روی آن ضربه بزن.",
                "Multiple tabs are supported. The tab count icon shows current tabs; tap it to manage or add tabs."
            ),
        ),
        tip = GL(
            "اگر صفحه‌ای از JavaScript سنگین استفاده می‌کند و فایل‌ها شناسایی نشدند، صفحه را کامل بارگذاری کن و چند ثانیه صبر کن.",
            "If a JS-heavy site doesn't trigger detections, let the page fully load and wait a few seconds for async requests to fire."
        )
    ),

    GuideSectionL(
        title    = GL("تورنت و مگنت", "Torrent & Magnet Links"),
        icon     = Icons.Outlined.Share,
        color    = Color(0xFFFF4057),
        category = GL("ابزار", "TOOLS"),
        steps = listOf(
            GuideStepL(
                "از منوی کناری وارد بخش Torrent شو.",
                "Open the side menu and tap Torrent."
            ),
            GuideStepL(
                "برای فایل .torrent: روی دکمه‌ی + بزن و فایل را از حافظه انتخاب کن.",
                "For .torrent files: tap the + button and select the file from your storage."
            ),
            GuideStepL(
                "برای لینک مگنت: روی آیکن زنجیر (🔗) بزن، لینک magnet: را پیست کن و تأیید کن.",
                "For magnet links: tap the chain icon (🔗), paste the magnet: link and confirm."
            ),
            GuideStepL(
                "HAD اطلاعات تورنت را نشان می‌دهد — نام، حجم کل، تعداد فایل‌ها. فایل‌هایی که می‌خواهی دانلود کنی را تیک بزن.",
                "HAD shows torrent info — name, total size, file count. Check the files you want to download."
            ),
            GuideStepL(
                "HAD به tracker ها متصل می‌شود، سپس به peer ها. تعداد peer های متصل و سرعت را در کارت تورنت می‌بینی.",
                "HAD connects to trackers, then to peers. You can see connected peer count and speed in the torrent card."
            ),
            GuideStepL(
                "بعد از دانلود کامل، فایل‌ها در پوشه‌ی تنظیم‌شده در Settings ذخیره می‌شوند. فایل‌های چند-بخشی در پوشه‌ی جداگانه‌ای به نام تورنت ذخیره می‌شوند.",
                "After completion, files are saved to the folder set in Settings. Multi-file torrents are organized in a named subfolder."
            ),
        ),
        tip = GL(
            "برای مگنت‌هایی که metadata ندارند، HAD ابتدا از DHT شبکه metadata را می‌گیرد. این ممکن است چند ثانیه طول بکشد.",
            "For magnets without metadata, HAD first fetches it from the DHT network. This may take a few seconds before downloading starts."
        )
    ),

    GuideSectionL(
        title    = GL("سرور دانلود ریموت", "Remote Download Server"),
        icon     = Icons.Outlined.Wifi,
        color    = Color(0xFF00FF88),
        category = GL("ابزار", "TOOLS"),
        steps = listOf(
            GuideStepL(
                "از منوی کناری وارد Remote Server شو و سرور را روشن کن.",
                "Open the side menu and tap Remote Server, then start the server."
            ),
            GuideStepL(
                "گوشی و کامپیوتر باید به همان شبکه‌ی Wi-Fi متصل باشند.",
                "Your phone and PC must be connected to the same Wi-Fi network."
            ),
            GuideStepL(
                "آدرس نمایش‌داده‌شده (مثلاً http://192.168.1.5:8080) را در مرورگر کامپیوتر باز کن.",
                "Open the displayed address (e.g. http://192.168.1.5:8080) in any PC browser."
            ),
            GuideStepL(
                "لینک دانلود را در فیلد URL وارد کن و Download Now یا Add to Queue بزن. دانلود روی گوشی شروع می‌شود.",
                "Paste a download URL in the field and tap Download Now or Add to Queue. The download starts on your phone."
            ),
            GuideStepL(
                "از قسمت Bulk Download می‌توانی چند لینک را هم‌زمان (هر کدام در یک خط) ارسال کنی.",
                "Use the Bulk Download section to send multiple URLs at once (one per line)."
            ),
            GuideStepL(
                "آخرین درخواست‌ها در تاریخچه نشان داده می‌شوند. می‌توانی لینک را دوباره دانلود کنی یا در صف قرار دهی.",
                "Recent requests appear in history. You can re-download or re-queue any link directly from there."
            ),
        ),
        tip = GL(
            "اگر IP گوشی نشان داده نمی‌شود، از اتصال وای‌فای مطمئن شو — سرور فقط روی شبکه‌ی محلی (LAN) کار می‌کند.",
            "If the phone IP isn't shown, verify Wi-Fi connection — the server only works on local network, not mobile data."
        )
    ),

    GuideSectionL(
        title    = GL("آرشیو وب", "Web Archive"),
        icon     = Icons.Outlined.Archive,
        color    = Color(0xFF9B59FF),
        category = GL("ابزار", "TOOLS"),
        steps = listOf(
            GuideStepL(
                "از منوی کناری وارد Web Archive شو و روی + بزن.",
                "Open the side menu and tap Web Archive, then tap +."
            ),
            GuideStepL(
                "آدرس سایت یا صفحه را وارد کن.",
                "Enter the website or page URL."
            ),
            GuideStepL(
                "Single Page: فقط همان صفحه ذخیره می‌شود (سریع‌تر). Full Site: همه‌ی صفحات به‌صورت بازگشتی دنبال می‌شوند (برای بایگانی کامل).",
                "Single Page: saves only that page (faster). Full Site: crawls all linked pages recursively (for complete backup)."
            ),
            GuideStepL(
                "حداکثر تعداد صفحات را در Full Site تعیین کن تا آرشیو خیلی بزرگ نشود.",
                "Set a max page limit in Full Site mode to prevent the archive from growing too large."
            ),
            GuideStepL(
                "تمام فایل‌های CSS، JavaScript، تصاویر و فونت‌ها هم ذخیره می‌شوند تا صفحه به‌صورت آفلاین کاملاً درست نشان داده شود.",
                "All CSS, JavaScript, images, and fonts are also saved so the page renders correctly offline."
            ),
            GuideStepL(
                "بعد از اتمام، از Open Folder به پوشه‌ی آرشیو در حافظه دسترسی پیدا کن. فایل index.html را در مرورگر باز کن.",
                "After completion, tap Open Folder to access the archive in storage. Open index.html in any browser."
            ),
        ),
        tip = GL(
            "برای سایت‌هایی که به لاگین نیاز دارند، کوکی‌های session را در Advanced Options وارد کن.",
            "For sites requiring login, paste session cookies into Advanced Options before starting the archive."
        )
    ),

    GuideSectionL(
        title    = GL("تشخیص خودکار کلیپ‌بورد", "Clipboard Auto-Detection"),
        icon     = Icons.Outlined.ContentPaste,
        color    = Color(0xFF00D4FF),
        category = GL("میانبرها", "SHORTCUTS"),
        steps = listOf(
            GuideStepL(
                "هر وقت یک لینک دانلودی را کپی کنی، روی آیکن «پیست» (📋) در نوار بالای HAD بزن.",
                "Whenever you copy a download link, tap the Paste icon (📋) in HAD's top bar."
            ),
            GuideStepL(
                "اگر یک لینک باشد: مستقیم در فیلد URL پنجره‌ی دانلود قرار می‌گیرد.",
                "If there's one link: it goes directly into the URL field of the download dialog."
            ),
            GuideStepL(
                "اگر چند لینک در کلیپ‌بورد باشد، یک پنجره‌ی انتخاب نشان داده می‌شود. تیک لینک‌هایی که می‌خواهی را بزن.",
                "If multiple links are in the clipboard, a selection dialog appears. Check the ones you want."
            ),
            GuideStepL(
                "«Download» برای شروع فوری، «Queue» برای اضافه کردن به صف.",
                "Tap Download to start immediately, or Queue to add to the waiting list."
            ),
        ),
        tip = GL(
            "HAD لینک‌های چندگانه را هم از یک متن معمولی استخراج می‌کند — حتی اگر بین متن باشند.",
            "HAD extracts multiple links from plain text too — even when URLs are embedded inside regular text."
        )
    ),

    GuideSectionL(
        title    = GL("اشتراک‌گذاری مستقیم از اپ‌های دیگر", "Share Directly from Other Apps"),
        icon     = Icons.Outlined.OpenInNew,
        color    = Color(0xFF00FF88),
        category = GL("میانبرها", "SHORTCUTS"),
        steps = listOf(
            GuideStepL(
                "در هر اپ (مرورگر، تلگرام، یوتیوب، فایل منیجر و...)، لینک یا فایل را Share کن.",
                "In any app (browser, Telegram, YouTube, file manager...), tap Share on any link or file."
            ),
            GuideStepL(
                "HAD را از لیست اشتراک‌گذاری انتخاب کن.",
                "Select HAD from the share sheet."
            ),
            GuideStepL(
                "یک sheet کوچک ظاهر می‌شود که لینک را نشان می‌دهد. روی Start Now یا Queue بزن.",
                "A small sheet appears showing the link. Tap Start Now or Queue."
            ),
            GuideStepL(
                "اگر بخواهی تنظیمات بیشتری انجام دهی (proxy، کوکی و...)، روی «Open in HAD to configure» بزن تا به پنجره‌ی کامل برود.",
                "For more configuration (proxy, cookies...), tap 'Open in HAD to configure' to go to the full dialog."
            ),
        ),
        tip = GL(
            "HAD به‌صورت خودکار حالت HLS را برای لینک‌های .m3u8 تشخیص می‌دهد حتی وقتی از طریق Share باز می‌شود.",
            "HAD auto-detects HLS mode for .m3u8 links even when opened via the share sheet."
        )
    ),

    GuideSectionL(
        title    = GL("تشخیص فایل تکراری", "Duplicate File Detection"),
        icon     = Icons.Outlined.ContentCopy,
        color    = Color(0xFFFF8C42),
        category = GL("میانبرها", "SHORTCUTS"),
        steps = listOf(
            GuideStepL(
                "قبل از شروع هر دانلود، HAD بررسی می‌کند آیا فایلی با همان نام در پوشه‌ی مقصد وجود دارد یا خیر.",
                "Before starting any download, HAD checks if a file with the same name already exists in the destination folder."
            ),
            GuideStepL(
                "اگر تکراری پیدا شد، یک دیالوگ با سه گزینه نشان داده می‌شود.",
                "If a duplicate is found, a dialog appears with three options."
            ),
            GuideStepL(
                "Save with new name: فایل با نام جدید (مثلاً video (1).mp4) ذخیره می‌شود.",
                "Save with new name: saves with a different name (e.g. video (1).mp4)."
            ),
            GuideStepL(
                "Overwrite: فایل قدیمی حذف می‌شود و از نو دانلود می‌شود.",
                "Overwrite: deletes the old file and re-downloads fresh."
            ),
            GuideStepL(
                "Skip: دانلود انجام نمی‌شود و فایل موجود حفظ می‌شود.",
                "Skip: download is cancelled and the existing file is kept."
            ),
        ),
        tip = GL(
            "اگر همیشه می‌خواهی فایل تکراری تغییر نام بگیرد، از Rename به‌صورت پیش‌فرض استفاده کن تا نگران حذف تصادفی نباشی.",
            "If you always want to rename duplicates, choosing Rename keeps you safe from accidentally overwriting files."
        )
    ),

    GuideSectionL(
        title    = GL("صف هوشمند", "Smart Queue Management"),
        icon     = Icons.Outlined.Queue,
        color    = Color(0xFF9B59FF),
        category = GL("مدیریت", "MANAGEMENT"),
        steps = listOf(
            GuideStepL(
                "HAD به‌صورت خودکار تعداد دانلودهای هم‌زمان را کنترل می‌کند. پیش‌فرض: ۲ دانلود هم‌زمان.",
                "HAD automatically manages concurrent downloads. Default: 2 simultaneous downloads."
            ),
            GuideStepL(
                "تعداد هم‌زمان را در Settings > Download Defaults > Max parallel تغییر بده.",
                "Change the concurrent limit in Settings > Download Defaults > Max parallel."
            ),
            GuideStepL(
                "وقتی یک دانلود تمام می‌شود، HAD به‌صورت خودکار دانلود بعدی در صف را شروع می‌کند.",
                "When a download completes, HAD automatically starts the next one in the queue."
            ),
            GuideStepL(
                "برای شروع همه‌ی آیتم‌های صف، روی نوار «Start All» (نمایش داده‌شده بالای لیست) ضربه بزن.",
                "To start all queued items at once, tap the 'Start All' bar shown above the download list."
            ),
            GuideStepL(
                "برای انتخاب چندگانه، انگشت را روی یک آیتم نگه دار. بعد از آن می‌توانی چند آیتم را انتخاب و حذف یا مدیریت کنی.",
                "For multi-select, long-press any item. Then tap others to select them for batch delete or management."
            ),
        ),
        tip = GL(
            "دانلودهایی که زمان‌بندی شده‌اند (scheduleFrom) به‌صورت خودکار از صف عبور نمی‌کنند — فقط در زمان مقرر اجرا می‌شوند.",
            "Scheduled downloads don't auto-promote from the queue — they only start at their set time."
        )
    ),

    GuideSectionL(
        title    = GL("محدودیت سرعت", "Speed Limiting"),
        icon     = Icons.Outlined.Speed,
        color    = Color(0xFF00D4FF),
        category = GL("مدیریت", "MANAGEMENT"),
        steps = listOf(
            GuideStepL(
                "در Advanced Options هنگام افزودن دانلود، فیلد «Max KB/s» را پیدا کن.",
                "When adding a download, find the 'Max KB/s' field in Advanced Options."
            ),
            GuideStepL(
                "مقدار ۰ یعنی بدون محدودیت. مقادیر دیگر پهنای باند دانلود را محدود می‌کنند.",
                "Value 0 means unlimited. Any other value caps download bandwidth."
            ),
            GuideStepL(
                "محدودیت سرعت از الگوریتم Token Bucket استفاده می‌کند — سرعت یکنواخت است و ضربه‌های ناگهانی ندارد.",
                "Speed limiting uses a Token Bucket algorithm — the rate is smooth without sudden bursts."
            ),
            GuideStepL(
                "برای محدودیت پیش‌فرض روی همه‌ی دانلودها، در Settings > Network > Max Speed (bytes/s) تنظیم کن.",
                "For a global default speed limit on all downloads, set it in Settings > Network > Max Speed (bytes/s)."
            ),
        ),
        tip = GL(
            "برای گوشی‌هایی با پلن اینترنت محدود، محدودیت سرعت کمک می‌کند دانلودهای بزرگ پهنای باند شبکه‌ی خانگی را کامل نگیرند.",
            "Speed limiting is useful on limited data plans or when you don't want large downloads to saturate your home network."
        )
    ),

    GuideSectionL(
        title    = GL("آنالیتیکس و آمار", "Analytics & Stats"),
        icon     = Icons.Outlined.BarChart,
        color    = Color(0xFF00FF88),
        category = GL("مدیریت", "MANAGEMENT"),
        steps = listOf(
            GuideStepL(
                "از منوی کناری وارد Analytics شو.",
                "Open the side menu and tap Analytics."
            ),
            GuideStepL(
                "بخش Overview آمار کلی: تعداد کل دانلودها، تعداد موفق، حجم کل ذخیره‌شده، میانگین و بیشینه‌ی سرعت.",
                "Overview section shows global stats: total downloads, successful count, total saved, average and peak speed."
            ),
            GuideStepL(
                "نمودار ماهانه: حجم دانلودشده در هر ماه را نشان می‌دهد. روی نوارها می‌توانی ببینی در کدام ماه بیشتر دانلود کردی.",
                "Monthly chart shows download volume per month — see which months you downloaded the most."
            ),
            GuideStepL(
                "نقشه‌ی حرارتی ساعتی: ساعت‌هایی از روز که بیشتر دانلود می‌کنی را نشان می‌دهد. برای تنظیم بهتر بازه‌ی زمانی مفید است.",
                "Hourly heatmap shows which hours of the day you download most — useful for setting the schedule window."
            ),
            GuideStepL(
                "تاریخچه‌ی اخیر: آخرین دانلودها با نام فایل، حجم، سرعت میانگین و وضعیت.",
                "Recent history shows last downloads with filename, size, average speed, and success/fail status."
            ),
            GuideStepL(
                "داده‌های قدیمی‌تر از ۳۰ روز به‌صورت خودکار پاک می‌شوند تا حافظه‌ی دیتابیس کنترل‌شده بماند.",
                "Data older than 30 days is automatically purged to keep the database size under control."
            ),
        ),
        tip = GL(
            "سرعت نمودار Speed History مربوط به دانلود در حال اجراست — بعد از اتمام نمودار پاک می‌شود.",
            "The Speed History chart shows live speed for the current download session — it clears after completion."
        )
    ),

    GuideSectionL(
        title    = GL("تنظیمات سراسری", "Global Settings"),
        icon     = Icons.Outlined.Settings,
        color    = Color(0xFFFF8C42),
        category = GL("پیکربندی", "CONFIGURATION"),
        steps = listOf(
            GuideStepL(
                "از منوی کناری روی Settings بزن.",
                "Open the side menu and tap Settings."
            ),
            GuideStepL(
                "Download Defaults: تعداد رشته پیش‌فرض، پوشه‌ی پیش‌فرض ذخیره، تعداد هم‌زمان، تعداد retry و timeout.",
                "Download Defaults: default thread count, save folder, max concurrent, retries, and timeout."
            ),
            GuideStepL(
                "Network: پروکسی پیش‌فرض و محدودیت سرعت پیش‌فرض برای همه‌ی دانلودها.",
                "Network: default proxy and default speed limit applied to all downloads."
            ),
            GuideStepL(
                "Schedule Window: بازه‌ی زمانی مجاز برای اجرای دانلودها. خارج از این بازه دانلودها مکث می‌کنند.",
                "Schedule Window: allowed time range for downloads to run. Outside this window downloads auto-pause."
            ),
            GuideStepL(
                "Options: Session Resume (ادامه دانلود)، Gzip، اعلان‌ها.",
                "Options: Session Resume toggle, Gzip support, and notification preferences."
            ),
            GuideStepL(
                "بعد از تغییر تنظیمات، روی Save بزن. تنظیمات فوراً برای دانلودهای جدید اعمال می‌شوند.",
                "After changing settings, tap Save. New settings apply immediately to new downloads."
            ),
        ),
        tip = GL(
            "پوشه‌ی پیش‌فرض را یک بار تنظیم کن تا هر بار نیازی به انتخاب مجدد نداشته باشی.",
            "Set your default folder once and you'll never need to pick it again for each download."
        )
    ),

    GuideSectionL(
        title    = GL("حل مشکلات رایج", "Troubleshooting Common Issues"),
        icon     = Icons.Outlined.BugReport,
        color    = Color(0xFFFF4057),
        category = GL("پشتیبانی", "SUPPORT"),
        steps = listOf(
            GuideStepL(
                "دانلود با خطای ۴۰۳ متوقف می‌شود: کوکی یا User-Agent مناسب اضافه کن. HAD به‌صورت خودکار چرخش User-Agent انجام می‌دهد.",
                "Download fails with 403: add cookies or a matching User-Agent. HAD auto-rotates User-Agent on each retry."
            ),
            GuideStepL(
                "دانلود با خطای ۴۲۹ (Too Many Requests) متوقف می‌شود: HAD تأخیرهای فزاینده اعمال می‌کند. صبر کن یا تعداد رشته‌ها را کاهش بده.",
                "Error 429 (Too Many Requests): HAD applies increasing delays automatically. Wait or reduce thread count."
            ),
            GuideStepL(
                "دانلود HLS کامل می‌شود ولی فایل قابل پخش نیست: HAD یک تبدیل جایگزین امتحان می‌کند. اگر باز هم نشد، یک پلیر مثل VLC را امتحان کن که .ts را پشتیبانی می‌کند.",
                "HLS download completes but file won't play: HAD tries an alternate conversion. If still broken, try VLC which supports .ts files."
            ),
            GuideStepL(
                "دانلود از صف خارج نمی‌شود: بررسی کن که تعداد دانلودهای فعال به حداکثر هم‌زمان نرسیده باشد. از Start All استفاده کن.",
                "Download stays in queue: check that active download count hasn't reached the concurrent limit. Tap Start All."
            ),
            GuideStepL(
                "حجم فایل نشان داده نمی‌شود: سرور از HEAD request پشتیبانی نمی‌کند. دانلود هنوز کار می‌کند، فقط درصد نمایش داده نخواهد شد.",
                "File size not showing: the server doesn't support HEAD requests. Download still works, just no percentage shown."
            ),
            GuideStepL(
                "تورنت بدون peer است: ترکر ممکن است آفلاین باشد. HAD DHT را هم امتحان می‌کند. چند دقیقه صبر کن تا peer پیدا شود.",
                "Torrent has no peers: tracker may be offline. HAD also tries DHT. Wait a few minutes for peers to be discovered."
            ),
            GuideStepL(
                "سرور ریموت IP نشان نمی‌دهد: مطمئن شو گوشی به Wi-Fi (نه داده‌ی موبایل) متصل است.",
                "Remote server shows no IP: make sure your phone is on Wi-Fi (not mobile data)."
            ),
        ),
        tip = GL(
            "اگر مشکلی داشتی که اینجا نبود، از صفحه‌ی About > Contact با ما در ارتباط باش.",
            "If you have an issue not listed here, contact us via the About > Contact section."
        )
    ),
)

private val categoryOrder = listOf(
    GL("شروع", "GETTING STARTED"),
    GL("پیشرفته", "ADVANCED"),
    GL("ابزار", "TOOLS"),
    GL("میانبرها", "SHORTCUTS"),
    GL("مدیریت", "MANAGEMENT"),
    GL("پیکربندی", "CONFIGURATION"),
    GL("پشتیبانی", "SUPPORT"),
)

@Composable
fun GuideTab() {
    var lang by remember { mutableStateOf(GuideLang.EN) }
    val scrollState = rememberScrollState()
    val isFa = lang == GuideLang.FA

    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    val grouped = remember(lang) {
        categoryOrder.map { cat ->
            cat to guideSectionsL.filter { sec ->
                sec.category.fa == cat.fa
            }
        }.filter { it.second.isNotEmpty() }
    }

    val layoutDir = if (isFa) androidx.compose.ui.unit.LayoutDirection.Rtl
    else androidx.compose.ui.unit.LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDir
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {

            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CyanPrimary.copy(alpha = 0.03f), Color.Transparent),
                        center = Offset(size.width * 0.5f, 0f),
                        radius = size.width * 0.9f
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (isFa) "راهنمای کامل HAD" else "HAD Complete Guide",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            if (isFa) "${guideSectionsL.size} بخش · همه‌ی امکانات"
                            else "${guideSectionsL.size} sections · every feature",
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    GuideLangSwitcher(lang = lang, onChange = { lang = it })
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))

                grouped.forEachIndexed { groupIdx, (category, sections) ->
                    if (groupIdx > 0) Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(14.dp)
                                .background(
                                    sections.firstOrNull()?.color ?: CyanPrimary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Text(
                            category.get(lang),
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 3.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    sections.forEachIndexed { idx, section ->
                        val globalIdx = guideSectionsL.indexOf(section)
                        val isExpanded = expandedIndex == globalIdx

                        GuideSectionCard(
                            section = section,
                            lang = lang,
                            expanded = isExpanded,
                            onClick = {
                                expandedIndex = if (isExpanded) null else globalIdx
                            }
                        )
                        if (idx < sections.size - 1) Spacer(Modifier.height(6.dp))
                    }

                    Spacer(Modifier.height(12.dp))
                    if (groupIdx < grouped.size - 1) {
                        HorizontalDivider(color = BorderColor.copy(alpha = 0.4f), thickness = 0.5.dp)
                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun GuideLangSwitcher(lang: GuideLang, onChange: (GuideLang) -> Unit) {
    Row(
        modifier = Modifier
            .background(ElevatedSurf, RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(GuideLang.FA to "فارسی", GuideLang.EN to "English").forEach { (l, label) ->
            val selected = lang == l
            Box(
                modifier = Modifier
                    .clickable { onChange(l) }
                    .background(
                        if (selected) CyanPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    color = if (selected) CyanPrimary else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun GuideSectionCard(
    section: GuideSectionL,
    lang: GuideLang,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val borderAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.5f else 0.15f,
        animationSpec = tween(200),
        label = "border"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (expanded) section.color.copy(alpha = 0.04f) else SurfaceDark,
        border = BorderStroke(
            if (expanded) 1.dp else 0.5.dp,
            section.color.copy(alpha = borderAlpha)
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(section.color.copy(alpha = if (expanded) 0.15f else 0.08f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(section.icon, null, tint = section.color, modifier = Modifier.size(18.dp))
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        section.title.get(lang),
                        color = if (expanded) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (expanded) FontWeight.Bold else FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                    Text(
                        "${section.steps.size} " + (if (lang == GuideLang.FA) "مرحله" else "steps"),
                        color = section.color.copy(alpha = if (expanded) 0.8f else 0.5f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (expanded) section.color.copy(alpha = 0.12f) else Color.Transparent,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        null,
                        tint = if (expanded) section.color else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    HorizontalDivider(
                        color = section.color.copy(alpha = 0.15f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    section.steps.forEachIndexed { i, step ->
                        GuideStepRow(
                            index = i,
                            text = if (lang == GuideLang.FA) step.fa else step.en,
                            color = section.color,
                            isLast = i == section.steps.size - 1
                        )
                    }

                    section.tip?.let { tip ->
                        Spacer(Modifier.height(10.dp))
                        TipCard(text = tip.get(lang), color = section.color, lang = lang)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideStepRow(
    index: Int,
    text: String,
    color: Color,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, color.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${index + 1}",
                    color = color,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(20.dp)
                        .padding(vertical = 2.dp)
                        .background(color.copy(alpha = 0.15f))
                )
            }
        }
        Text(
            text,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 8.dp)
        )
    }
}

@Composable
private fun TipCard(text: String, color: Color, lang: GuideLang) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "💡",
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 1.dp)
        )
        Column {
            Text(
                if (lang == GuideLang.FA) "نکته" else "Tip",
                color = color,
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text,
                color = color.copy(alpha = 0.85f),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}