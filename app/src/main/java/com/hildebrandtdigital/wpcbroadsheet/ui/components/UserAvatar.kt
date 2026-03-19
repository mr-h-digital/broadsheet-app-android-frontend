package com.hildebrandtdigital.wpcbroadsheet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import java.io.File

/**
 * Renders either a photo avatar (if [avatarPath] is non-null) or a polished
 * initials avatar derived from [name].
 *
 * @param avatarVersion  Increment this after saving a new avatar to bust
 *                       Coil's disk/memory cache (path stays the same but
 *                       the file contents change).
 */
@Composable
fun UserAvatar(
    name          : String,
    avatarPath    : String?  = null,
    avatarVersion : Long     = 0L,
    primaryColor  : Color,
    secondaryColor: Color    = primaryColor,
    size          : Dp       = 80.dp,
    fontSize      : TextUnit = 28.sp,
    borderColor   : Color?   = null,
    borderWidth   : Dp       = 2.dp,
    modifier      : Modifier = Modifier,
) {
    val c        = LocalAppColors.current
    val initials = extractInitials(name)
    val border   = borderColor ?: if (c.isDark)
        primaryColor.copy(alpha = 0.6f)
    else
        primaryColor  // full-saturation ring in light mode

    if (avatarPath != null) {
        PhotoAvatar(
            path          = avatarPath,
            avatarVersion = avatarVersion,
            size          = size,
            borderColor   = border,
            borderWidth   = borderWidth,
            modifier      = modifier,
            fallback      = {
                InitialsCircle(
                    initials, primaryColor, secondaryColor,
                    size, fontSize, border, borderWidth, modifier,
                )
            },
        )
    } else {
        InitialsCircle(
            initials, primaryColor, secondaryColor,
            size, fontSize, border, borderWidth, modifier,
        )
    }
}

@Composable
private fun InitialsCircle(
    initials    : String,
    primary     : Color,
    secondary   : Color,
    size        : Dp,
    fontSize    : TextUnit,
    borderColor : Color,
    borderWidth : Dp,
    modifier    : Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.85f),
                        secondary.copy(alpha = 0.65f),
                    )
                )
            )
            .border(borderWidth, borderColor, CircleShape),
    ) {
        // Subtle inner ring for depth
        Box(
            modifier = Modifier
                .size(size - 4.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
        )
        Text(
            text  = initials,
            style = MaterialTheme.typography.headlineMedium.copy(
                color    = Color.White,
                fontSize = fontSize,
                shadow   = androidx.compose.ui.graphics.Shadow(
                    color      = Color.Black.copy(alpha = 0.25f),
                    blurRadius = 4f,
                ),
            ),
        )
    }
}

@Composable
private fun PhotoAvatar(
    path         : String,
    avatarVersion: Long,
    size         : Dp,
    borderColor  : Color,
    borderWidth  : Dp,
    modifier     : Modifier,
    fallback     : @Composable () -> Unit,
) {
    val context = LocalContext.current

    // Rebuild the request whenever path or version changes, busting Coil's cache.
    // avatarVersion is included in both cache keys so Coil always treats a new
    // save as a distinct image even though the file path stays the same.
    val request = remember(path, avatarVersion) {
        ImageRequest.Builder(context)
            .data(File(path))
            .diskCacheKey("$path:$avatarVersion")
            .memoryCacheKey("$path:$avatarVersion")
            .build()
    }

    // SubcomposeAsyncImage is the correct Coil 2 API for conditional slot rendering.
    // Unlike rememberAsyncImagePainter, it starts loading immediately when
    // placed in the composition — no chicken-and-egg state problem.
    SubcomposeAsyncImage(
        model              = request,
        contentDescription = "Profile photo",
        contentScale       = ContentScale.Crop,
        modifier           = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape),
        loading = { fallback() },
        error   = { fallback() },
    )
}

/** Extract up to two initials. "Lee Hildebrandt" → "LH" */
fun extractInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else            -> "${parts.first()[0]}${parts.last()[0]}".uppercase()
    }
}