package com.hildebrandtdigital.wpcbroadsheet.ui.components

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AvatarManager
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors

/**
 * Bottom sheet that lets the user pick a new avatar from:
 *  - The device gallery (photo picker — no permission needed on API 33+)
 *  - The camera
 *  - Or remove the current avatar
 *
 * After picking, [onBitmapReady] is called with the decoded bitmap so the
 * caller can push it through the crop UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerSheet(
    hasAvatar    : Boolean,
    onBitmapReady: (Bitmap) -> Unit,
    onRemove     : () -> Unit,
    onDismiss    : () -> Unit,
) {
    val c = LocalAppColors.current
    val context    = LocalContext.current
    val scope      = rememberCoroutineScope()
    var isLoading  by remember { mutableStateOf(false) }

    // ── Camera temp URI ───────────────────────────────────────────────────────
    val cameraUri = remember { AvatarManager.createCameraUri(context) }

    // ── Photo picker (API 33+ native picker, no READ_MEDIA permission needed) ─
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                val bmp = AvatarManager.decodeBitmap(context, uri)
                isLoading = false
                if (bmp != null) onBitmapReady(bmp) else onDismiss()
            }
        }
    }

    // ── Camera launcher ───────────────────────────────────────────────────────
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            scope.launch {
                isLoading = true
                val bmp = AvatarManager.decodeBitmap(context, cameraUri)
                isLoading = false
                if (bmp != null) onBitmapReady(bmp) else onDismiss()
            }
        }
    }

    // ── Camera permission ─────────────────────────────────────────────────────
    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) cameraLauncher.launch(cameraUri)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = c.surface1,
        dragHandle       = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(c.borderColor)
            )

            Spacer(Modifier.height(20.dp))

            Text("Update Profile Photo", style = MaterialTheme.typography.headlineMedium)
            Text("Choose a source for your avatar", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(20.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
            } else {
                // ── Options ───────────────────────────────────────────────────
                AvatarOption(
                    icon    = Icons.Rounded.PhotoLibrary,
                    iconBg  = Color(0x1A4F8EF7),
                    iconTint= Primary,
                    title   = "Choose from Gallery",
                    sub     = "Pick a photo from your device",
                    onClick = {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                )

                Spacer(Modifier.height(2.dp))
                HorizontalDivider(color = c.borderColor, modifier = Modifier.padding(start = 62.dp))
                Spacer(Modifier.height(2.dp))

                AvatarOption(
                    icon    = Icons.Rounded.CameraAlt,
                    iconBg  = Color(0x1A4FF7C8),
                    iconTint= Accent,
                    title   = "Take a Photo",
                    sub     = "Use your camera",
                    onClick = {
                        if (hasCameraPermission) {
                            cameraLauncher.launch(cameraUri)
                        } else {
                            cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                )

                if (hasAvatar) {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = c.borderColor)
                    Spacer(Modifier.height(12.dp))
                    AvatarOption(
                        icon    = Icons.Rounded.DeleteOutline,
                        iconBg  = Color(0x1AF74F6B),
                        iconTint= Danger,
                        title   = "Remove Photo",
                        sub     = "Revert to initials avatar",
                        onClick = {
                            onRemove()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarOption(
    icon    : ImageVector,
    iconBg  : Color,
    iconTint: Color,
    title   : String,
    sub     : String,
    onClick : () -> Unit,
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(sub,   style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = c.textDim, modifier = Modifier.size(18.dp))
    }
}
