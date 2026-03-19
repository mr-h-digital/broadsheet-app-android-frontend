package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hildebrandtdigital.wpcbroadsheet.data.model.User
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AvatarManager
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.data.repository.UserRepository
import com.hildebrandtdigital.wpcbroadsheet.ui.components.AvatarPickerSheet
import com.hildebrandtdigital.wpcbroadsheet.ui.components.UserAvatar
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.CameraAlt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userRepository: UserRepository,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit
) {
    val user = AppSession.currentUser
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val userId = AppSession.currentUserId

    var name by remember { mutableStateOf(user?.name ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    
    var nameError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Avatar state
    val avatarPath by AvatarManager.observeAvatarPath(context, userId).collectAsStateWithLifecycle(initialValue = null)
    var avatarVersion by remember { mutableLongStateOf(0L) }
    var showPickerSheet by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    if (cropBitmap != null) {
        AvatarCropScreen(
            bitmap = cropBitmap!!,
            onCropped = { cropped ->
                scope.launch {
                    AvatarManager.saveAvatar(context, userId, cropped)
                    avatarVersion++
                    cropBitmap = null
                }
            },
            onCancel = { cropBitmap = null }
        )
        return
    }

    val c = LocalAppColors.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 16.dp),
                            strokeWidth = 2.dp,
                            color = Primary
                        )
                    } else {
                        IconButton(onClick = {
                            nameError = name.isBlank()
                            
                            if (!nameError && user != null) {
                                scope.launch {
                                    isSaving = true
                                    try {
                                        userRepository.updateUser(
                                            id = user.id,
                                            name = name,
                                            email = email,
                                            role = user.role,
                                            phone = phone,
                                            siteId = user.siteId,
                                            actor = user.name
                                        )
                                        // Update session
                                        AppSession.login(user.copy(
                                            name = name,
                                            email = email,
                                            phone = phone
                                        ))
                                        onSaved()
                                    } catch (e: Exception) {
                                        // Handle error
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            }
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = c.bgDeep
                )
            )
        },
        containerColor = c.bgDeep
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Section
            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.clickable { showPickerSheet = true }) {
                UserAvatar(
                    name = user?.name ?: "",
                    avatarPath = avatarPath,
                    avatarVersion = avatarVersion,
                    primaryColor = Primary,
                    size = 120.dp
                )
                // Camera icon badge
                Surface(
                    color = c.surface1,
                    shape = CircleShape,
                    modifier = Modifier.size(32.dp).border(1.dp, c.borderColor, CircleShape),
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.CameraAlt, null, tint = c.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ProfileField(
                    label         = "Full Name",
                    value         = name,
                    onValueChange = { name = it; nameError = false },
                    placeholder   = "e.g. John Smith",
                    isError       = nameError,
                    errorMsg      = "Name is required",
                    imeAction     = ImeAction.Next,
                    onNext        = { focusManager.moveFocus(FocusDirection.Next) },
                    capitalization= KeyboardCapitalization.Words,
                    enabled       = !isSaving
                )
                
                ProfileField(
                    label         = "Email",
                    value         = email,
                    onValueChange = { email = it },
                    placeholder   = "e.g. john@example.com",
                    isError       = false,
                    errorMsg      = "",
                    imeAction     = ImeAction.Next,
                    onNext        = { focusManager.moveFocus(FocusDirection.Next) },
                    enabled       = !isSaving
                )

                ProfileField(
                    label         = "Phone",
                    value         = phone,
                    onValueChange = { phone = it },
                    placeholder   = "e.g. +27 123 4567",
                    isError       = false,
                    errorMsg      = "",
                    imeAction     = ImeAction.Done,
                    onNext        = { focusManager.clearFocus() },
                    enabled       = !isSaving
                )
            }
        }
    }

    if (showPickerSheet) {
        AvatarPickerSheet(
            hasAvatar = avatarPath != null,
            onBitmapReady = { bmp ->
                showPickerSheet = false
                cropBitmap = bmp
            },
            onRemove = {
                scope.launch { AvatarManager.deleteAvatar(context, userId) }
            },
            onDismiss = { showPickerSheet = false }
        )
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    errorMsg: String,
    imeAction: ImeAction,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = imeAction,
            capitalization = capitalization
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onNext = { onNext() }
        ),
        supportingText = if (isError) { { Text(errorMsg) } } else null
    )
}
