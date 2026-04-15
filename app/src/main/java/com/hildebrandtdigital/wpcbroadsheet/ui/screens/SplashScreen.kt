package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.R
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.delay
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val c = LocalAppColors.current

    // ── Animation state machine ───────────────────────────────────────────────
    // Phase 0 → logo pops in  (0–600ms)
    // Phase 1 → ring pulses   (600–1200ms)
    // Phase 2 → text fades in (1000–1600ms)
    // Phase 3 → tagline fades (1400–2000ms)
    // Phase 4 → hold          (2000–2800ms)
    // Phase 5 → whole screen fades out → onFinished()

    var phase by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        delay(100);  phase = 1   // logo in
        delay(500);  phase = 2   // ring pulse
        delay(400);  phase = 3   // wordmark
        delay(400);  phase = 4   // tagline
        delay(800)                // hold
        phase = 5                 // fade out
        delay(600)
        onFinished()
    }

    // Logo scale — springs up from 0.4
    val logoScale by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow,
        ),
        label = "logoScale",
    )

    // Logo alpha
    val logoAlpha by animateFloatAsState(
        targetValue    = if (phase in 1..4) 1f else 0f,
        animationSpec  = tween(400),
        label          = "logoAlpha",
    )

    // Infinite ring pulse
    val ringPulse = rememberInfiniteTransition(label = "ring")
    val ringScale by ringPulse.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.18f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringScale",
    )

    // Wordmark fade + slide
    val wordmarkAlpha by animateFloatAsState(
        targetValue   = if (phase >= 3) 1f else 0f,
        animationSpec = tween(500),
        label         = "wordmarkAlpha",
    )
    val wordmarkSlide by animateFloatAsState(
        targetValue   = if (phase >= 3) 0f else 24f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "wordmarkSlide",
    )

    // Tagline fade + slide
    val taglineAlpha by animateFloatAsState(
        targetValue   = if (phase >= 4) 1f else 0f,
        animationSpec = tween(500),
        label         = "taglineAlpha",
    )
    val taglineSlide by animateFloatAsState(
        targetValue   = if (phase >= 4) 0f else 16f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label         = "taglineSlide",
    )

    // Overall screen fade out
    val screenAlpha by animateFloatAsState(
        targetValue   = if (phase >= 5) 0f else 1f,
        animationSpec = tween(500),
        label         = "screenAlpha",
    )

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha }
            .background(
                if (c.isDark)
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF0D1428), c.bgDeep),
                        radius = 1200f,
                    )
                else
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0D47A1),  // deep royal blue
                            Color(0xFF1565C0),  // mid royal blue
                            Color(0xFF1976D2),  // bright royal blue
                        ),
                    )
            )
    ) {
        // Background glow blobs
        Box(
            modifier = Modifier
                .size(340.dp)
                .offset(y = (-60).dp)
                .clip(CircleShape)
                .background(
                    if (c.isDark)
                        Brush.radialGradient(colors = listOf(Color(0x334F8EF7), Color.Transparent))
                    else
                        Brush.radialGradient(colors = listOf(Color(0x554F8EF7), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(y = 120.dp, x = 80.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x22A04FF7), Color.Transparent)
                    )
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── Logo with pulsing ring ────────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha  = logoAlpha
                    }
            ) {
                // Outer pulsing ring
                if (phase >= 2) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0x664F8EF7),
                                        Color(0x66A04FF7),
                                        Color(0x664FF7C8),
                                        Color(0x664F8EF7),
                                    )
                                ),
                                shape = CircleShape,
                            )
                    )
                }

                // Mid glow halo
                Box(
                    modifier = Modifier
                        .size(128.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0x554F8EF7),
                                    Color(0x22A04FF7),
                                    Color.Transparent,
                                )
                            )
                        )
                )

                // App logo image
                Image(
                    painter            = painterResource(R.drawable.wpc_logo),
                    contentDescription = "WPC Broadsheet",
                    modifier           = Modifier
                        .size(104.dp)
                        .shadow(24.dp, CircleShape, ambientColor = Color(0x884F8EF7))
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xCC4F8EF7),
                                    Color(0xCCA04FF7),
                                    Color(0xCC4FF7C8),
                                    Color(0xCC4F8EF7),
                                )
                            ),
                            shape = CircleShape,
                        )
                )
            }

            Spacer(Modifier.height(28.dp))

            // ── Wordmark ──────────────────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha        = wordmarkAlpha
                    translationY = wordmarkSlide
                }
            ) {
                Text(
                    text      = "WPC.",
                    style     = MaterialTheme.typography.displayLarge.copy(
                        color         = if (c.isDark) c.textBright else Color.White,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-1.5).sp,
                        fontSize      = 56.sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text      = "Broadsheet Manager",
                    style     = MaterialTheme.typography.titleMedium.copy(
                        color         = if (c.isDark) c.textMuted else Color.White.copy(alpha = 0.80f),
                        fontSize      = 17.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight    = FontWeight.Normal,
                    ),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Tagline ───────────────────────────────────────────────────────
            Text(
                text      = "Daily meal tracking & billing\nfor retirement villages — reimagined.",
                style     = MaterialTheme.typography.bodyMedium.copy(
                    color      = if (c.isDark) c.textDim else Color.White.copy(alpha = 0.65f),
                    lineHeight = 22.sp,
                ),
                textAlign = TextAlign.Center,
                modifier  = Modifier
                    .padding(horizontal = 40.dp)
                    .graphicsLayer {
                        alpha        = taglineAlpha
                        translationY = taglineSlide
                    }
            )

            Spacer(Modifier.height(60.dp))

            // ── Loading dots ──────────────────────────────────────────────────
            if (phase in 2..4) {
                LoadingDots(alpha = taglineAlpha)
            }
        }

        // Developer branding at bottom
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .graphicsLayer { alpha = taglineAlpha }
        ) {
            Image(
                painter            = painterResource(R.drawable.mrhdigital_logo),
                contentDescription = "Mr. H Digital",
                modifier           = Modifier
                    .height(28.dp)
                    .graphicsLayer { alpha = if (c.isDark) 0.75f else 0.65f },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text     = "v1.0.0",
                style    = MaterialTheme.typography.labelSmall.copy(
                    color    = if (c.isDark) c.textDim else Color.White.copy(alpha = 0.45f),
                    fontSize = 10.sp,
                ),
            )
        }
    }
}

@Composable
private fun LoadingDots(alpha: Float) {
    val c = LocalAppColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    ) {
        repeat(3) { index ->
            val dotAlpha by infiniteTransition.animateFloat(
                initialValue  = 0.2f,
                targetValue   = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot$index",
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .graphicsLayer { this.alpha = dotAlpha }
                    .background(if (c.isDark) Primary else Color.White)
            )
        }
    }
}