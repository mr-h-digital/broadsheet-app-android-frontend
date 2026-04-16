package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hildebrandtdigital.wpcbroadsheet.R
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.data.repository.UserRepository
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import kotlinx.coroutines.launch
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.LocalAppColors
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.appBackground

@Composable
fun LoginScreen(
    userRepository: UserRepository? = null,
    onLoginSuccess: () -> Unit,
) {
    val c = LocalAppColors.current
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var loading  by remember { mutableStateOf(false) }
    val scope    = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .appBackground(c)
    ) {
        // Subtle radial glow centered at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x554F8EF7), Color.Transparent),
                        radius = 700f,
                    )
                )
        )

        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {

            // ── Hero — centered, stacked ──────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                // Logo circle with glow halo
                Box(contentAlignment = Alignment.Center) {
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
                    Image(
                        painter            = painterResource(R.drawable.wpc_logo),
                        contentDescription = "WPC Broadsheet logo",
                        modifier           = Modifier
                            .size(96.dp)
                            .shadow(16.dp, CircleShape, ambientColor = Color(0x884F8EF7))
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xAA4F8EF7),
                                        Color(0xAAA04FF7),
                                        Color(0xAA4FF7C8),
                                        Color(0xAA4F8EF7),
                                    )
                                ),
                                shape = CircleShape,
                            )
                    )
                }

                Spacer(Modifier.height(22.dp))

                // App name — centered
                Text(
                    text      = "WPC.",
                    style     = MaterialTheme.typography.displayLarge.copy(
                        color         = c.textBright,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp,
                    ),
                    textAlign = TextAlign.Center,
                )
                Text(
                    text      = "Broadsheet Manager",
                    style     = MaterialTheme.typography.titleMedium.copy(
                        color         = c.textMuted,
                        fontSize      = 17.sp,
                        letterSpacing = 0.4.sp,
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(18.dp))

                // Tagline
                Text(
                    text      = "Daily meal tracking & billing for retirement village residents — reimagined.",
                    style     = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        color      = c.textMuted.copy(alpha = 0.75f),
                    ),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(48.dp))
            }

            // ── Login card ────────────────────────────────────────────────────
            Surface(
                shape    = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color    = c.surface1,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {

                    Text(
                        text  = "Welcome back 👋",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(24.dp))

                    // Email
                    Text("Email Address", style = fieldLabelStyle())
                    Spacer(Modifier.height(8.dp))
                    WpcTextField(
                        value        = email,
                        onChange     = { email = it; errorMsg = null },
                        placeholder  = "you@wpc.co.za",
                        keyboardType = KeyboardType.Email,
                        imeAction    = ImeAction.Next,
                        onImeAction  = { focusManager.moveFocus(FocusDirection.Next) },
                    )

                    Spacer(Modifier.height(14.dp))

                    // Password
                    Text("Password", style = fieldLabelStyle())
                    Spacer(Modifier.height(8.dp))
                    WpcTextField(
                        value                = password,
                        onChange             = { password = it; errorMsg = null },
                        placeholder          = "••••••••",
                        visualTransformation = if (showPass) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        imeAction            = ImeAction.Done,
                        onImeAction          = { focusManager.clearFocus() },
                        trailingIcon         = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    imageVector        = if (showPass) Icons.Rounded.VisibilityOff
                                    else Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint               = c.textMuted,
                                )
                            }
                        }
                    )

                    // Error message
                    AnimatedVisibility(visible = errorMsg != null) {
                        Column {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape    = RoundedCornerShape(8.dp),
                                color    = Color(0x22FF5252),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text     = errorMsg ?: "",
                                    style    = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFFF5252),
                                    ),
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Sign In
                    Button(
                        onClick  = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMsg = "Please enter your email and password."
                                return@Button
                            }
                            loading  = true
                            errorMsg = null
                            scope.launch {
                                val user = userRepository?.login(email.trim(), password)
                                loading  = false
                                if (user != null) {
                                    AppSession.login(user)
                                    onLoginSuccess()
                                } else if (userRepository == null) {
                                    // Demo/preview mode — log in as the first SampleData user
                                    // so AppSession.currentUserId is a stable real ID, not "system"
                                    AppSession.login(SampleData.users.first())
                                    onLoginSuccess()
                                } else {
                                    errorMsg = "Invalid email or password. Please try again."
                                }
                            }
                        },
                        enabled  = !loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Primary,
                            contentColor   = Color.White,
                        ),
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                color       = Color.White,
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text  = "Sign In  →",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text     = "Default password for all accounts: wpc2026",
                        style    = MaterialTheme.typography.bodySmall.copy(
                            color    = c.textDim,
                            fontSize = 11.sp,
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun WpcTextField(
    value                : String,
    onChange             : (String) -> Unit,
    placeholder          : String,
    keyboardType         : KeyboardType         = KeyboardType.Text,
    imeAction            : ImeAction             = ImeAction.Default,
    onImeAction          : () -> Unit            = {},
    visualTransformation : VisualTransformation = VisualTransformation.None,
    trailingIcon         : @Composable (() -> Unit)? = null,
) {
    val c = LocalAppColors.current
    OutlinedTextField(
        value                = value,
        onValueChange        = onChange,
        modifier             = Modifier.fillMaxWidth(),
        placeholder          = { Text(placeholder, color = c.textDim) },
        visualTransformation = visualTransformation,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions      = KeyboardActions(onAny = { onImeAction() }),
        trailingIcon         = trailingIcon,
        singleLine           = true,
        shape                = RoundedCornerShape(10.dp),
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = Primary,
            unfocusedBorderColor    = c.borderColor,
            focusedContainerColor   = c.surface2,
            unfocusedContainerColor = c.surface2,
            focusedTextColor        = c.textBright,
            unfocusedTextColor      = c.textBright,
            cursorColor             = Primary,
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textBright),
    )
}

@Composable
private fun fieldLabelStyle() = MaterialTheme.typography.labelMedium.copy(
    color         = LocalAppColors.current.textMuted,
    fontSize      = 12.sp,
    letterSpacing = 0.5.sp,
)