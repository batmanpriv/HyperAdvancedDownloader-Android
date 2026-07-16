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
import androidx.compose.material3.Surface
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
    val alreadyGranted = ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

    if (alreadyGranted) return

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun StoragePermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    var hasPermission by remember { mutableStateOf(checkPermission()) }

    val manageAllFilesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasPermission = checkPermission()
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
    }

    if (hasPermission) {
        content()
    } else {
        PermissionScreen(
            onGrantClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${context.packageName}")
                    )
                    manageAllFilesLauncher.launch(intent)
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