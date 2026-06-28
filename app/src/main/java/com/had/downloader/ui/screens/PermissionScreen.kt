package com.had.downloader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.had.downloader.ui.theme.CyanPrimary
import com.had.downloader.ui.theme.SpaceBlack
import com.had.downloader.ui.theme.TextMuted
import com.had.downloader.ui.theme.TextPrimary
import com.had.downloader.ui.theme.TextSecondary

@Composable
fun PermissionScreen(
    onGrantClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpaceBlack),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = CyanPrimary,
                modifier = Modifier.size(90.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Storage Permission Required",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Allow file access to save downloads on your device.",
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onGrantClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanPrimary,
                    contentColor = SpaceBlack
                )
            ) {

                Icon(
                    imageVector = Icons.Default.LockOpen,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.size(8.dp))

                Text(
                    text = "Grant Permission"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    android.os.Process.killProcess(
                        android.os.Process.myPid()
                    )
                }
            ) {

                Text(
                    text = "Exit App",
                    color = TextMuted
                )
            }
        }
    }
}