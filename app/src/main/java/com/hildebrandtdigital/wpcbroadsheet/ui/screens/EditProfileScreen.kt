package com.hildebrandtdigital.wpcbroadsheet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.hildebrandtdigital.wpcbroadsheet.data.model.User
import com.hildebrandtdigital.wpcbroadsheet.data.model.UserRole
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AppSession
import com.hildebrandtdigital.wpcbroadsheet.data.repository.AvatarManager
import com.hildebrandtdigital.wpcbroadsheet.data.repository.SampleData
import com.hildebrandtdigital.wpcbroadsheet.ui.components.AvatarPickerSheet
import com.hildebrandtdigital.wpcbroadsheet.ui.components.UserAvatar
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcBrandingSize
import com.hildebrandtdigital.wpcbroadsheet.ui.components.WpcLogoBadge
import com.hildebrandtdigital.wpcbroadsheet.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onNavigateBack: () -> Unit
) {
    val user = AppSession.currentUser
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    
    var firstNameError by remember { mutableStateOf(false) }
    var lastNameError by remember { mutableStateOf(false) }

    var showAvatarPicker by remember { mutableStateOf(false) }

    val c = LocalAppColors.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Back icon would go here
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
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
            Box(contentAlignment = Alignment.BottomEnd) {
                UserAvatar(
                    name = (user?.firstName ?: "") + " " + (user?.lastName ?: ""),
                    avatarPath = null, // Temporary
                    avatarVersion = 0L, // Temporary
                    primaryColor = Primary,
                    size = 120.dp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Form Fields
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileField(
                        label         = "First Name",
                        value         = firstName,
                        onValueChange = { firstName = it; firstNameError = false },
                        placeholder   = "e.g. John",
                        isError       = firstNameError,
                        errorMsg      = "First name is required",
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Next) },
                        modifier      = Modifier.weight(1f),
                        capitalization= KeyboardCapitalization.Words,
                    )
                    ProfileField(
                        label         = "Last Name",
                        value         = lastName,
                        onValueChange = { lastName = it; lastNameError = false },
                        placeholder   = "e.g. Smith",
                        isError       = lastNameError,
                        errorMsg      = "Last name is required",
                        imeAction     = ImeAction.Next,
                        onNext        = { focusManager.moveFocus(FocusDirection.Next) },
                        modifier      = Modifier.weight(1f),
                        capitalization= KeyboardCapitalization.Words,
                    )
                }
            }
        }
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
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = imeAction,
            capitalization = capitalization
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onNext = { onNext() }
        )
    )
}
