package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.cloud.HeirloomCloudAuthService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Soft Sage Green for Action Buttons as requested
val SoftSageGreen = Color(0xFF62875E)
val SoftSageGreenDark = Color(0xFF4C6D48)
val SoftSageContainer = Color(0xFFE4EDE3)

enum class AuthMode {
    SIGN_UP, LOG_IN
}

/**
 * Sign Up and Log In screen for new and returning visitors.
 * Provides cloud authentication, multi-device synchronization, and
 * user-specific recipe vault access.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var authMode by remember { mutableStateOf(AuthMode.SIGN_UP) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun handleSubmit() {
        errorMessage = null
        focusManager.clearFocus()

        val email = emailInput.trim()
        val password = passwordInput.trim()
        val name = nameInput.trim()

        if (authMode == AuthMode.SIGN_UP && name.isBlank()) {
            errorMessage = "Please enter your cook name."
            return
        }
        if (email.isBlank() || !email.contains("@")) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }

        isSubmitting = true
        coroutineScope.launch {
            if (authMode == AuthMode.SIGN_UP) {
                val result = HeirloomCloudAuthService.signUp(
                    name = name,
                    email = email,
                    password = password,
                    context = context
                )
                isSubmitting = false
                result.onSuccess { user ->
                    Toast.makeText(context, "Welcome, ${user.name}! Your recipe tin is ready.", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Unable to create account. Please try again."
                }
            } else {
                val result = HeirloomCloudAuthService.logIn(
                    email = email,
                    password = password,
                    context = context
                )
                isSubmitting = false
                result.onSuccess { user ->
                    Toast.makeText(context, "Welcome back, ${user.name}!", Toast.LENGTH_SHORT).show()
                    onAuthSuccess()
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Unable to log in. Please check your credentials."
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Vintage Stained Pages Branding Badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SoftSageContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Heirloom Recipe Tin",
                    tint = SoftSageGreenDark,
                    modifier = Modifier.size(32.dp)
                )
            }

            // App Title & Tagline
            Text(
                text = "Between Stained Pages",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = TerracottaPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (authMode == AuthMode.SIGN_UP) {
                    "Create your cloud recipe tin to access loved family recipes across all your kitchen devices."
                } else {
                    "Welcome back! Sign in to synchronize your recipe tin and notes across devices."
                },
                fontSize = 13.sp,
                color = WarmOnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Auth Card with Warm Cream Background
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auth_screen_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Mode Toggle: "Create Account" vs "Log In to Existing Account"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SurfaceContainerHigh)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (authMode == AuthMode.SIGN_UP) SoftSageGreen else Color.Transparent)
                                .clickable {
                                    authMode = AuthMode.SIGN_UP
                                    errorMessage = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("auth_toggle_signup"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Create Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (authMode == AuthMode.SIGN_UP) Color.White else WarmOnSurfaceVariant
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (authMode == AuthMode.LOG_IN) SoftSageGreen else Color.Transparent)
                                .clickable {
                                    authMode = AuthMode.LOG_IN
                                    errorMessage = null
                                }
                                .padding(vertical = 10.dp)
                                .testTag("auth_toggle_login"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Log In",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (authMode == AuthMode.LOG_IN) Color.White else WarmOnSurfaceVariant
                            )
                        }
                    }

                    // Error Banner
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_error_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = errorMessage ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Form Fields with Rounded Inputs
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Name field (only in Sign Up mode)
                        if (authMode == AuthMode.SIGN_UP) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = {
                                    nameInput = it
                                    if (errorMessage != null) errorMessage = null
                                },
                                label = { Text("Your Cook Name") },
                                placeholder = { Text("e.g., Sarah or Grandpa Joe") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = SageTertiary)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text,
                                    imeAction = ImeAction.Next
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SoftSageGreen,
                                    unfocusedBorderColor = WarmOutline.copy(alpha = 0.6f),
                                    focusedLabelColor = SoftSageGreen
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_name_input")
                            )
                        }

                        // Email field
                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Email Address") },
                            placeholder = { Text("cook@stainedpages.app") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Mail, contentDescription = null, tint = SageTertiary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftSageGreen,
                                unfocusedBorderColor = WarmOutline.copy(alpha = 0.6f),
                                focusedLabelColor = SoftSageGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_email_input")
                        )

                        // Password field
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                if (errorMessage != null) errorMessage = null
                            },
                            label = { Text("Password") },
                            placeholder = { Text("Min 6 characters") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Lock, contentDescription = null, tint = SageTertiary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = WarmOnSurfaceVariant
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { handleSubmit() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SoftSageGreen,
                                unfocusedBorderColor = WarmOutline.copy(alpha = 0.6f),
                                focusedLabelColor = SoftSageGreen
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_password_input")
                        )
                    }

                    // Soft Sage Green Action Button
                    Button(
                        onClick = { handleSubmit() },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SoftSageGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_btn")
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (authMode == AuthMode.SIGN_UP) Icons.Default.PersonAdd else Icons.Default.Login,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (authMode == AuthMode.SIGN_UP) {
                                        "Create Account & Open Tin"
                                    } else {
                                        "Log In to My Recipe Tin"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // Toggle mode prompt at bottom of card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                authMode = if (authMode == AuthMode.SIGN_UP) AuthMode.LOG_IN else AuthMode.SIGN_UP
                                errorMessage = null
                            }
                            .padding(vertical = 4.dp)
                            .testTag("auth_switch_mode_btn"),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (authMode == AuthMode.SIGN_UP) {
                                "Already have an account? "
                            } else {
                                "First time here? "
                            },
                            fontSize = 13.sp,
                            color = WarmOnSurfaceVariant
                        )
                        Text(
                            text = if (authMode == AuthMode.SIGN_UP) "Log in" else "Create an account",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftSageGreenDark
                        )
                    }
                }
            }

            // Quick Demo Returning User Card (for easy testing of returning account flow)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SoftSageContainer.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        authMode = AuthMode.LOG_IN
                        emailInput = "cook@stainedpages.app"
                        passwordInput = "heirloom123"
                        errorMessage = null
                    }
                    .testTag("auth_quick_demo_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = SoftSageGreenDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Test Returning Cook Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = WarmOnSurface
                        )
                        Text(
                            text = "Fill cook@stainedpages.app to test multi-device synced recipes",
                            fontSize = 11.sp,
                            color = WarmOnSurfaceVariant
                        )
                    }
                    Text(
                        text = "Fill",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SoftSageGreenDark
                    )
                }
            }

            // Trust & Privacy Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = SageTertiary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Row-level encrypted cloud storage. Your family recipes stay strictly yours.",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = WarmOnSurfaceVariant
                )
            }
        }
    }
}
