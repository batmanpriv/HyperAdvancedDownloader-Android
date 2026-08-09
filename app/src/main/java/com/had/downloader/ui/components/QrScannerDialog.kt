package com.had.downloader.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.had.downloader.ui.theme.*
import java.nio.ByteBuffer
import java.util.concurrent.Executors

@Composable
fun QrScannerDialog(
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit,
    onQueue: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedText by remember { mutableStateOf<String?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                if (bitmap != null) {
                    previewBitmap = bitmap
                    val width = bitmap.width
                    val height = bitmap.height
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    val source = RGBLuminanceSource(width, height, pixels)
                    val result = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source)))
                    scannedText = result.text
                    scanError = null
                } else {
                    scanError = t("Couldn't read that image", "نتونستم این عکس رو بخونم")
                }
            }
        }.onFailure {
            scanError = t("No QR code found in that image", "کد QR توی این عکس پیدا نشد")
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(SpaceBlack)) {
            when {
                scannedText != null -> QrResultView(
                    text = scannedText!!,
                    onDownload = { onDownload(scannedText!!) },
                    onQueue = { onQueue(scannedText!!) },
                    onRescan = { scannedText = null; scanError = null; previewBitmap = null },
                    onDismiss = onDismiss
                )
                hasCameraPermission -> QrCameraView(
                    lifecycleOwner = lifecycleOwner,
                    onDetected = { text -> scannedText = text },
                    onDismiss = onDismiss,
                    onPickImage = { galleryLauncher.launch("image/*") },
                    scanError = scanError
                )
                else -> QrPermissionDenied(
                    onDismiss = onDismiss,
                    onPickImage = { galleryLauncher.launch("image/*") },
                    onRetryPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    }
}

@Composable
private fun QrCameraView(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onDetected: (String) -> Unit,
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    scanError: String?
) {
    val context = LocalContext.current
    var detected by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                val analysisExecutor = Executors.newSingleThreadExecutor()
                val reader = MultiFormatReader()

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { imageAnalysis ->
                            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                                if (detected) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }
                                decodeQrFromImageProxy(imageProxy, reader)?.let { text ->
                                    detected = true
                                    onDetected(text)
                                }
                                imageProxy.close()
                            }
                        }
                    runCatching {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                        )
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(240.dp)
                .background(Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(SpaceBlack.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Scan a QR code", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextPrimary)
                }
            }
            Text("Point the camera at a QR code with a link in it", color = TextSecondary, fontSize = 12.sp)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(SpaceBlack.copy(alpha = 0.6f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (scanError != null) {
                Text(scanError, color = RedError, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            OutlinedButton(onClick = onPickImage) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(t("Choose image instead", "به‌جاش یه عکس انتخاب کن"))
            }
        }
    }
}

private fun decodeQrFromImageProxy(imageProxy: ImageProxy, reader: MultiFormatReader): String? {
    return try {
        val plane = imageProxy.planes[0]
        val buffer: ByteBuffer = plane.buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data)
        val source = PlanarYUVLuminanceSource(
            data, imageProxy.width, imageProxy.height,
            0, 0, imageProxy.width, imageProxy.height, false
        )
        val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
        result.text
    } catch (e: Exception) {
        null 
    } finally {
        reader.reset()
    }
}

@Composable
private fun QrResultView(
    text: String,
    onDownload: () -> Unit,
    onQueue: () -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit
) {
    val isUrl = text.startsWith("http://") || text.startsWith("https://")

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.PhotoLibrary,
            contentDescription = null,
            tint = if (isUrl) GreenSuccess else OrangeWarn,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("QR code found", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ElevatedSurf,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text,
                color = TextSecondary,
                fontSize = 13.sp,
                modifier = Modifier.padding(14.dp)
            )
        }

        if (!isUrl) {
            Spacer(Modifier.height(8.dp))
            Text(
                t("This doesn't look like a link - you can still try, but it may not work as a download.", "این شبیه یه لینک نیست - می‌تونی امتحان کنی ولی ممکنه به‌عنوان دانلود کار نکنه."),
                color = OrangeWarn,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SpaceBlack)
        ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(t("Download now", "دانلود همین حالا"))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onQueue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Queue, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(t("Add to queue", "افزودن به صف"))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onRescan) { Text(t("Scan again", "دوباره اسکن کن")) }
            TextButton(onClick = onDismiss) { Text(t("Cancel", "انصراف")) }
        }
    }
}

@Composable
private fun QrPermissionDenied(
    onDismiss: () -> Unit,
    onPickImage: () -> Unit,
    onRetryPermission: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera permission needed", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            t("To scan a QR code live, allow camera access. You can also just pick an image instead.", "برای اسکن زنده‌ی کد QR، دسترسی دوربین رو بده. یا می‌تونی فقط یه عکس انتخاب کنی."),
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetryPermission,
            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = SpaceBlack)
        ) { Text(t("Allow camera", "اجازه‌ی دوربین")) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onPickImage) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(t("Choose image instead", "به‌جاش یه عکس انتخاب کن"))
        }
        Spacer(Modifier.height(10.dp))
        TextButton(onClick = onDismiss) { Text(t("Cancel", "انصراف")) }
    }
}
