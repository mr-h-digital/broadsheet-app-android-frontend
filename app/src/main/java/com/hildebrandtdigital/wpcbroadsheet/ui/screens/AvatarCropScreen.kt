package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Full-screen crop editor.
 *
 * Shows the source [bitmap] with a circular crop window. The user can
 * pinch-to-zoom and drag to position the image. Tapping ✓ crops a square
 * region matching the circle and calls [onCropped] with the result.
 */
@Composable
fun AvatarCropScreen(
    bitmap    : Bitmap,
    onCropped : (Bitmap) -> Unit,
    onCancel  : () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // Crop circle radius in dp — fixed at 140dp
    val cropRadiusDp = 140.dp
    val density      = LocalDensity.current
    val cropRadiusPx = with(density) { cropRadiusDp.toPx() }

    // Transform state: offset of image centre relative to screen centre, and scale
    var scale     by remember { mutableFloatStateOf(1f) }
    var offsetX   by remember { mutableFloatStateOf(0f) }
    var offsetY   by remember { mutableFloatStateOf(0f) }
    var isCropping by remember { mutableStateOf(false) }

    // Compute initial scale to fill the crop circle
    val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    LaunchedEffect(bitmap) {
        val minFill = (cropRadiusPx * 2f) / min(bitmap.width, bitmap.height).toFloat()
        scale = max(minFill, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Draggable / zoomable image ─────────────────────────────────────────
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale   = (scale * zoom).coerceIn(0.5f, 6f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            val bitmapW  = bitmap.width  * scale
            val bitmapH  = bitmap.height * scale
            val drawLeft = center.x - bitmapW / 2f + offsetX
            val drawTop  = center.y - bitmapH / 2f + offsetY

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF(drawLeft, drawTop, drawLeft + bitmapW, drawTop + bitmapH),
                    null,
                )
            }

            // Darken overlay outside the circle
            drawRect(
                color = Color.Black.copy(alpha = 0.55f),
                size  = this.size,
            )

            // Punch out the crop circle using Porter-Duff
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                }
                canvas.nativeCanvas.drawCircle(center.x, center.y, cropRadiusPx, paint)
            }

            // Re-draw the bitmap inside the circle (undarked)
            drawIntoCanvas { canvas ->
                val bitmapW2  = bitmap.width  * scale
                val bitmapH2  = bitmap.height * scale
                val drawLeft2 = center.x - bitmapW2 / 2f + offsetX
                val drawTop2  = center.y - bitmapH2 / 2f + offsetY

                canvas.nativeCanvas.save()
                canvas.nativeCanvas.clipPath(android.graphics.Path().also { path ->
                    path.addCircle(center.x, center.y, cropRadiusPx, android.graphics.Path.Direction.CW)
                })
                canvas.nativeCanvas.drawBitmap(
                    bitmap,
                    null,
                    android.graphics.RectF(drawLeft2, drawTop2, drawLeft2 + bitmapW2, drawTop2 + bitmapH2),
                    null,
                )
                canvas.nativeCanvas.restore()
            }

            // Circle border
            drawCircle(
                color  = Color.White.copy(alpha = 0.8f),
                radius = cropRadiusPx,
                center = center,
                style  = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            )

            // Grid lines inside circle (rule of thirds)
            val third = cropRadiusPx * 2 / 3
            for (i in 1..2) {
                val x = center.x - cropRadiusPx + i * third
                val y = center.y - cropRadiusPx + i * third
                drawLine(Color.White.copy(0.2f), Offset(x, center.y - cropRadiusPx), Offset(x, center.y + cropRadiusPx), strokeWidth = 1f)
                drawLine(Color.White.copy(0.2f), Offset(center.x - cropRadiusPx, y), Offset(center.x + cropRadiusPx, y), strokeWidth = 1f)
            }
        }

        // ── Top bar ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Cancel", tint = Color.White)
            }
            Text("Crop Photo", style = MaterialTheme.typography.headlineSmall.copy(color = Color.White))
            Spacer(Modifier.width(48.dp))
        }

        // ── Hint ───────────────────────────────────────────────────────────────
        Text(
            "Pinch to zoom  ·  Drag to reposition",
            style    = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(0.6f)),
            modifier = Modifier.align(Alignment.Center).offset(y = (cropRadiusDp + 20.dp)),
        )

        // ── Confirm button ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp),
        ) {
            if (isCropping) {
                CircularProgressIndicator(color = Primary, modifier = Modifier.size(56.dp))
            } else {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            isCropping = true
                            val cropped = cropBitmap(bitmap, scale, offsetX, offsetY, cropRadiusPx)
                            isCropping = false
                            onCropped(cropped)
                        }
                    },
                    containerColor = Primary,
                    contentColor   = Color.White,
                    shape          = CircleShape,
                    modifier       = Modifier.size(64.dp),
                ) {
                    Icon(Icons.Rounded.Check, "Confirm crop", modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

/**
 * Crops a square from [source] corresponding to the circle the user sees,
 * then scales the result to a standard 512×512 output.
 */
private suspend fun cropBitmap(
    source    : Bitmap,
    scale     : Float,
    offsetX   : Float,
    offsetY   : Float,
    circlePx  : Float,
): Bitmap = withContext(Dispatchers.Default) {
    // The crop window is a square inscribed in the circle.
    // We need to map from screen-space back to bitmap-space.
    val diameter   = (circlePx * 2f).toInt()
    val bitmapW    = source.width  * scale
    val bitmapH    = source.height * scale

    // Centre of the circle in screen-space relative to image top-left
    // (screen centre + user offset, then subtract image top-left)
    // Because we always draw the image centred, image top-left in screen is:
    //   screenCentreX - bitmapW/2 + offsetX
    // Circle centre in screen is screenCentreX, so relative to image top-left:
    //   circleLeftInImage  = bitmapW/2 - offsetX - circlePx
    //   circleTopInImage   = bitmapH/2 - offsetY - circlePx

    val circleLeftInScaled  = bitmapW / 2f - offsetX - circlePx
    val circleTopInScaled   = bitmapH / 2f - offsetY - circlePx

    // Convert to original bitmap coordinates
    val srcLeft   = (circleLeftInScaled / scale).toInt().coerceIn(0, source.width  - 1)
    val srcTop    = (circleTopInScaled  / scale).toInt().coerceIn(0, source.height - 1)
    val srcSize   = ((circlePx * 2f) / scale).toInt()
        .coerceAtLeast(1)
        .let { minOf(it, source.width - srcLeft, source.height - srcTop) }

    // Crop the square region
    val cropped = Bitmap.createBitmap(source, srcLeft, srcTop, srcSize, srcSize)

    // Scale to 512×512 output
    val output = Bitmap.createScaledBitmap(cropped, 512, 512, true)
    if (cropped !== output) cropped.recycle()
    output
}
