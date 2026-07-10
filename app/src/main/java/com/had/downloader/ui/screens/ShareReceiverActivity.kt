package com.had.downloader.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.had.downloader.data.model.DownloadMode
import com.had.downloader.service.HlsDownloader
import com.had.downloader.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    @Inject
    lateinit var hlsDetector: HlsDownloader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedUrl = extractUrlFromIntent(intent)
        if (sharedUrl == null) {
            Toast.makeText(this, "No URL found in share", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val detectedMode = when {
            hlsDetector.isHlsUrl(sharedUrl) -> DownloadMode.HLS
            sharedUrl.contains(".mpd", ignoreCase = true) -> DownloadMode.HTTP
            else -> DownloadMode.HTTP
        }

        vm.updateForm(
            vm.form.value.copy(url = sharedUrl, mode = detectedMode)
        )

        setContent {
            HADTheme {
                ShareBottomSheet(
                    url      = sharedUrl,
                    mode     = detectedMode,
                    onStart  = {
                        vm.startDownload()
                        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onQueue  = {
                        vm.addToQueue()
                        Toast.makeText(this, "Added to queue", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onOpen   = {
                        val mainIntent = Intent(this, Class.forName("com.had.downloader.MainActivity")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        startActivity(mainIntent)
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }

    private fun extractUrlFromIntent(intent: Intent?): String? {
        if (intent == null) return null

        return when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { text ->
                    extractUrlFromText(text)
                } ?: intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.toString()
            }
            Intent.ACTION_VIEW -> {
                intent.dataString
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("UNCHECKED_CAST")
                val texts = intent.getStringArrayListExtra(Intent.EXTRA_TEXT)
                texts?.firstNotNullOfOrNull { extractUrlFromText(it) }
            }
            else -> intent.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.let { extractUrlFromText(it) }
        }
    }

    private fun extractUrlFromText(text: String): String? {
        val urlRegex = Regex("""https?://[^\s"'<>]+""")
        val found = urlRegex.findAll(text).map { it.value }.toList()
        return found.firstOrNull { url ->
            url.contains(".mp4", ignoreCase = true) ||
                    url.contains(".m3u8", ignoreCase = true) ||
                    url.contains(".mkv", ignoreCase = true) ||
                    url.contains(".zip", ignoreCase = true) ||
                    url.contains(".rar", ignoreCase = true) ||
                    url.contains(".pdf", ignoreCase = true) ||
                    url.contains("download", ignoreCase = true)
        } ?: found.firstOrNull()
    }
}

@Composable
fun ShareBottomSheet(
    url: String,
    mode: DownloadMode,
    onStart: () -> Unit,
    onQueue: () -> Unit,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = SurfaceDark
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CyanPrimary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.CloudDownload, null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                        }
                        Column {
                            Text("Download with HAD", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (mode == DownloadMode.HLS) GreenSuccess.copy(alpha = 0.12f) else PurpleAccent.copy(alpha = 0.12f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        mode.name,
                                        color = if (mode == DownloadMode.HLS) GreenSuccess else PurpleAccent,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (mode == DownloadMode.HLS) {
                                    Text("Stream detected", color = GreenSuccess, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, null, tint = TextMuted)
                    }
                }

                HorizontalDivider(color = BorderColor, thickness = 0.5.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ElevatedSurf, RoundedCornerShape(10.dp))
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Link, null, tint = CyanDim, modifier = Modifier.size(16.dp))
                    Text(
                        url,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onQueue,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PurpleAccent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PurpleAccent.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.AddCircleOutline, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Queue", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }

                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SpaceBlack),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Start Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                TextButton(
                    onClick = onOpen,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open in HAD to configure", color = TextMuted, fontSize = 12.sp)
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}