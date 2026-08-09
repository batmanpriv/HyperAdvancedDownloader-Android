package com.had.downloader.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.had.downloader.ui.theme.*
import com.had.downloader.ui.utils.QRCodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun QRCodeDialog(
    url: String,
    filename: String = "",
    onDismiss: () -> Unit,
    onDownload: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSharing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            qrBitmap = QRCodeGenerator.generateQRCodeWithColor(
                content = url,
                width = 600,
                height = 600,
                foreground = Color.parseColor("#00D4FF"),
                background = Color.parseColor("#0F1420"),
                margin = 20
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(listOf(CyanPrimary, PurpleAccent)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.QrCodeScanner,
                            null,
                            tint = SpaceBlack,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            t("QR Code", "کد QR"),
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            t("Scan to download", "برای دانلود اسکن کن"),
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, null, tint = TextMuted)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedSurf,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Link,
                            null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (filename.isNotBlank()) filename else url.take(50),
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                qrBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .shadow(16.dp, RoundedCornerShape(16.dp))
                            .background(SurfaceDark, RoundedCornerShape(16.dp))
                            .border(1.dp, CyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(280.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                                .background(SurfaceDark, CircleShape)
                                .border(2.dp, CyanPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "H",
                                color = CyanPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } ?: run {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .background(ElevatedSurf, RoundedCornerShape(16.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                saveQRCode(context, qrBitmap, url)
                                isSaving = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = qrBitmap != null && !isSaving,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = CyanPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Outlined.Save, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(t("Save", "ذخیره"), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isSharing = true
                                shareQRCode(context, qrBitmap, url, filename)
                                isSharing = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = qrBitmap != null && !isSharing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = SpaceBlack
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isSharing) {
                            CircularProgressIndicator(
                                color = SpaceBlack,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(t("Share", "اشتراک‌گذاری"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurpleAccent,
                        contentColor = SpaceBlack
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t("Download File", "دانلود فایل"), fontWeight = FontWeight.Bold)
                }

                Text(
                    t("Powered by HAD Downloader", "قدرت‌گرفته از HAD Downloader"),
                    color = TextMuted.copy(alpha = 0.5f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

private suspend fun saveQRCode(
    context: Context,
    bitmap: Bitmap?,
    url: String
) = withContext(Dispatchers.IO) {
    if (bitmap == null) return@withContext
    try {
        val folder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "HAD/QR Codes"
        )
        folder.mkdirs()
        val filename = "HAD_QR_${System.currentTimeMillis()}.png"
        val file = File(folder, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private suspend fun shareQRCode(
    context: Context,
    bitmap: Bitmap?,
    url: String,
    filename: String
) = withContext(Dispatchers.IO) {
    if (bitmap == null) return@withContext
    try {
        val cacheDir = context.cacheDir
        val qrFile = File(cacheDir, "HAD_QR_${System.currentTimeMillis()}.png")
        FileOutputStream(qrFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val shareText = buildString {
            appendLine("📥 Download this file with HAD")
            appendLine()
            appendLine("🔗 $url")
            appendLine()
            appendLine("📁 " + (if (filename.isNotBlank()) filename else t("File", "فایل")))
            appendLine()
            appendLine("---")
            appendLine("🚀 Download faster with HAD Downloader")
            appendLine("✨ Multi-thread • Resume • HLS • Torrent")
            appendLine("📱 Available on Android")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            qrFile
        )
        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            type = "image/png"
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        withContext(Dispatchers.Main) {
            context.startActivity(
                android.content.Intent.createChooser(
                    shareIntent,
                    t("Share QR Code with HAD", "اشتراک‌گذاری کد QR با HAD")
                )
            )
        }
        qrFile.deleteOnExit()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}