package com.had.downloader

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelProvider
import com.had.downloader.ui.screens.MainScreen
import com.had.downloader.ui.screens.PermissionScreen
import com.had.downloader.ui.theme.HADTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.HiltAndroidApp
import com.had.downloader.ui.screens.MainViewModel
import androidx.activity.OnBackPressedCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltAndroidApp
class HadApplication : Application()

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private var lastBackPressTime = 0L
    private val BACK_PRESS_INTERVAL = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTime = System.currentTimeMillis()
                val hasPrevious = viewModel.goBack()
                if (hasPrevious) {
                    return
                }
                if (currentTime - lastBackPressTime < BACK_PRESS_INTERVAL) {
                    finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(
                        this@MainActivity,
                        "Press back again to exit",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })

        setContent {
            HADTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    StoragePermissionGate {
                        NotificationPermissionEffect()
                        MainScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationPermissionEffect() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val alreadyGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    if (alreadyGranted) return

    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasShownLauncher by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context as androidx.activity.ComponentActivity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (!shouldShowRationale) {
                showSettingsDialog = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasShownLauncher) {
            hasShownLauncher = true
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context as androidx.activity.ComponentActivity,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (shouldShowRationale) {
                showSettingsDialog = true
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Notification Permission") },
            text = { Text("Enable notifications to get download updates. You can enable it in Settings.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Skip")
                }
            }
        )
    }
}

@Composable
private fun StoragePermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                return true
            }
            try {
                val testFile = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    ".had_test.tmp"
                )
                testFile.createNewFile()
                testFile.delete()
                true
            } catch (e: Exception) {
                false
            }
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    var hasPermission by remember { mutableStateOf(checkPermission()) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasShownLauncher by remember { mutableStateOf(false) }

    val manageAllFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        scope.launch {
            delay(500)
            hasPermission = checkPermission()
            if (!hasPermission) {
                delay(1000)
                hasPermission = checkPermission()
            }
            if (!hasPermission) {
                showSettingsDialog = true
            }
        }
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (!hasPermission) {
            val shouldShowRationale = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                context as androidx.activity.ComponentActivity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            if (!shouldShowRationale) {
                showSettingsDialog = true
            }
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Storage Access Required") },
            text = {
                Text(
                    "HAD needs storage access to save downloads.\n\n" +
                            "If the permission dialog didn't appear:\n" +
                            "1. Go to Settings → Apps → HAD\n" +
                            "2. Tap Permissions\n" +
                            "3. Enable Storage"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Skip")
                }
            }
        )
    } else if (hasPermission) {
        content()
    } else {
        PermissionScreen(
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        manageAllFilesLauncher.launch(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        manageAllFilesLauncher.launch(intent)
                    }
                } else {
                    legacyPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }
            }
        )
    }
}