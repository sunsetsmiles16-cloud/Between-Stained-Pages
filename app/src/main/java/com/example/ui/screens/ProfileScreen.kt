package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AppThemePalette
import com.example.data.HeirloomRepository
import com.example.ui.components.HeirloomTopBar
import com.example.ui.components.ThemePaletteSelectorSection
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCloudSync: () -> Unit,
    onResetToFirstRun: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by HeirloomRepository.userProfile.collectAsState()
    val recipes by HeirloomRepository.recipes.collectAsState()
    val cookbooks by HeirloomRepository.cookbooks.collectAsState()
    val scannedCards by HeirloomRepository.scannedCards.collectAsState()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAvatarPickerDialog by remember { mutableStateOf(false) }
    var showExportTinDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showResetNewUserDialog by remember { mutableStateOf(false) }

    // Dietary options list
    val allDietaryOptions = listOf(
        "Vegetarian",
        "Gluten-Free",
        "Dairy-Free",
        "Nut-Free",
        "Vegan",
        "Low Carb",
        "Pescatarian",
        "Halal",
        "Kosher"
    )

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var tempName by remember { mutableStateOf(userProfile.name) }
        var tempTitle by remember { mutableStateOf(userProfile.title) }
        var tempBio by remember { mutableStateOf(userProfile.bio) }
        var tempEmail by remember { mutableStateOf(userProfile.email) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile & Bio",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TerracottaPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_name_input")
                    )
                    OutlinedTextField(
                        value = tempTitle,
                        onValueChange = { tempTitle = it },
                        label = { Text("Kitchen Title") },
                        placeholder = { Text("e.g. Heirloom Chef & Curator") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_title_input")
                    )
                    OutlinedTextField(
                        value = tempBio,
                        onValueChange = { tempBio = it },
                        label = { Text("Custom Bio or Motto") },
                        placeholder = { Text("e.g. Baking memories from scratch") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_bio_input")
                    )
                    OutlinedTextField(
                        value = tempEmail,
                        onValueChange = { tempEmail = it },
                        label = { Text("Account Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_profile_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        HeirloomRepository.updateProfile(
                            name = tempName.ifBlank { "Home Cook" },
                            title = tempTitle.ifBlank { "Curator" },
                            bio = tempBio.ifBlank { "Baking memories from scratch." },
                            email = tempEmail.ifBlank { "" },
                            measurementUnits = userProfile.measurementUnits,
                            defaultServings = userProfile.defaultServings,
                            keepAwake = userProfile.keepAwakeOnCookMode,
                            autoSync = userProfile.autoSyncPantryToGrocery,
                            haptic = userProfile.hapticFeedback,
                            avatarType = userProfile.avatarType,
                            dietaryPreferences = userProfile.dietaryPreferences,
                            compactFeedView = userProfile.compactFeedView,
                            keepScreenAwakeByDefault = userProfile.keepScreenAwakeByDefault,
                            context = context
                        )
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SageTertiary),
                    modifier = Modifier.testTag("save_profile_dialog_btn")
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    // Avatar Selection Dialog
    if (showAvatarPickerDialog) {
        val avatarOptions = listOf(
            Triple("wooden_spoon", "Wooden Spoon", Icons.Default.SoupKitchen),
            Triple("flour_sifter", "Flour Sifter", Icons.Default.BakeryDining),
            Triple("vintage_apron", "Vintage Apron", Icons.Default.DryCleaning),
            Triple("rolling_pin", "Rolling Pin", Icons.Default.TakeoutDining),
            Triple("dutch_oven", "Dutch Oven", Icons.Default.OutdoorGrill),
            Triple("custom_photo", "Family Photo", Icons.Default.AccountCircle)
        )

        AlertDialog(
            onDismissRequest = { showAvatarPickerDialog = false },
            title = {
                Text(
                    text = "Choose Vintage Avatar",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TerracottaPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Pick an illustrated culinary heirloom icon for your recipe profile frame:",
                        fontSize = 13.sp,
                        color = WarmOnSurfaceVariant
                    )
                    avatarOptions.forEach { (typeKey, label, icon) ->
                        val isSelected = userProfile.avatarType == typeKey
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SurfaceContainerHighest else SurfaceContainerLow,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, SageTertiary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    HeirloomRepository.setAvatarType(typeKey)
                                    showAvatarPickerDialog = false
                                    Toast.makeText(context, "Avatar updated to $label", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 2.dp)
                                .testTag("avatar_option_$typeKey")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SageTertiary else SurfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) Color.White else TerracottaPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = if (typeKey == "wooden_spoon") "Classic heirloom stir" else if (typeKey == "flour_sifter") "Baking & pastry craft" else if (typeKey == "vintage_apron") "Grandma's stitched linen" else "Kitchen treasure",
                                        fontSize = 11.sp,
                                        color = WarmOnSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = SageTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAvatarPickerDialog = false }) {
                    Text("Done", color = TerracottaPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Export Recipe Tin Dialog
    if (showExportTinDialog) {
        var selectedFormat by remember { mutableStateOf("json") }
        val exportJson = remember(recipes) { HeirloomRepository.exportRecipeTinJson() }
        val exportCsv = remember(recipes) { HeirloomRepository.exportRecipeTinCsv() }
        val activeText = if (selectedFormat == "json") exportJson else exportCsv

        AlertDialog(
            onDismissRequest = { showExportTinDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = SageTertiary)
                    Text(
                        text = "Export My Recipe Tin",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = TerracottaPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Download or copy all ${recipes.size} recipes from your recipe tin. You own 100% of your culinary data.",
                        fontSize = 13.sp,
                        color = WarmOnSurfaceVariant
                    )

                    // Format Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (selectedFormat == "json") SageTertiary else SurfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFormat = "json" }
                                .testTag("export_format_json")
                        ) {
                            Text(
                                text = "JSON Format (.json)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFormat == "json") Color.White else WarmOnSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (selectedFormat == "csv") SageTertiary else SurfaceContainerLow,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFormat = "csv" }
                                .testTag("export_format_csv")
                        ) {
                            Text(
                                text = "Spreadsheet (.csv)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedFormat == "csv") Color.White else WarmOnSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    // Preview Box
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        Text(
                            text = activeText.take(600) + if (activeText.length > 600) "\n... (+ ${activeText.length - 600} characters)" else "",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = WarmOnSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Recipe Tin Export", activeText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Recipe tin data copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceContainerHighest)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, activeText)
                                putExtra(Intent.EXTRA_SUBJECT, "Between Stained Pages - Recipe Tin Export")
                                type = if (selectedFormat == "json") "application/json" else "text/csv"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Recipe Tin"))
                            showExportTinDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SageTertiary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share / Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportTinDialog = false }) {
                    Text("Close", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    // Sign Out Dialog
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign Out of Stained Pages?", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold) },
            text = { Text("Your local recipe tin and saved cookbooks will remain safely cached on this device.", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        Toast.makeText(context, "Signed out safely.", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmOutline)
                ) {
                    Text("Sign Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = {
                Text(
                    text = "Clear Data & Reset Tin?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "This will erase all local recipes, scanned cards, and custom cookbooks, resetting Stained Pages to a pristine clean slate. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = WarmOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            HeirloomRepository.resetToNewUser(context)
                            showDeleteAccountDialog = false
                            Toast.makeText(context, "Account data reset. Your recipe tin is now clean.", Toast.LENGTH_SHORT).show()
                            onResetToFirstRun()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Keep My Data", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    // Developer / Preview First-Run Reset Dialog
    if (showResetNewUserDialog) {
        AlertDialog(
            onDismissRequest = { showResetNewUserDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Reset to New User View?",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = WarmOnSurface
                )
            },
            text = {
                Text(
                    text = "This simulates a fresh first-time install by clearing all local database records, resetting app storage, re-arming the Welcome modal sequence, and returning you to the empty recipe tin.",
                    fontSize = 14.sp,
                    color = WarmOnSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            HeirloomRepository.resetToNewUser(context)
                            showResetNewUserDialog = false
                            Toast.makeText(context, "App reset to new user state! Welcome modal re-armed.", Toast.LENGTH_SHORT).show()
                            onResetToFirstRun()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.testTag("confirm_reset_new_user_button")
                ) {
                    Text("Reset & Launch First-Run", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetNewUserDialog = false }) {
                    Text("Cancel", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "User Profile & Account",
                onBackClick = onNavigateBack
            )
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .testTag("profile_screen_content"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Cozy Cottagecore Profile Header Card
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth().testTag("profile_header_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Rounded Avatar Frame with Cottagecore Illustrated Icon or Photo
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .border(2.5.dp, SageTertiary.copy(alpha = 0.6f), CircleShape)
                                        .background(SurfaceContainerHighest)
                                        .clickable { showAvatarPickerDialog = true }
                                        .testTag("profile_avatar_frame"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (userProfile.avatarType == "custom_photo" && userProfile.avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = userProfile.avatarUrl,
                                            contentDescription = "User Avatar",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        val avatarIcon = when (userProfile.avatarType) {
                                            "flour_sifter" -> Icons.Default.BakeryDining
                                            "vintage_apron" -> Icons.Default.DryCleaning
                                            "rolling_pin" -> Icons.Default.TakeoutDining
                                            "dutch_oven" -> Icons.Default.OutdoorGrill
                                            else -> Icons.Default.SoupKitchen
                                        }
                                        Icon(
                                            imageVector = avatarIcon,
                                            contentDescription = "Heirloom Avatar Icon",
                                            tint = TerracottaPrimary,
                                            modifier = Modifier.size(42.dp)
                                        )
                                    }

                                    // Small edit badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(SageTertiary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Change Avatar",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userProfile.name.ifBlank { "Home Cook" },
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = userProfile.title.ifBlank { "Curator" },
                                        fontSize = 13.sp,
                                        color = TerracottaPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (userProfile.email.isNotBlank()) {
                                        Text(
                                            text = userProfile.email,
                                            fontSize = 12.sp,
                                            color = WarmOnSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { showEditProfileDialog = true },
                                    modifier = Modifier.testTag("edit_profile_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = "Edit Profile",
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }

                            // Bio / Motto Quote Box
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = SurfaceContainerHigh.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatQuote,
                                        contentDescription = null,
                                        tint = SageTertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "\"${userProfile.bio}\"",
                                        fontFamily = FontFamily.Serif,
                                        fontSize = 13.sp,
                                        color = WarmOnSurface,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }

                            // Quick Edit Profile & Change Avatar Buttons (Soft Sage Styling)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showEditProfileDialog = true },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SageTertiary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("edit_profile_action_button")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Edit Bio & Name", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showAvatarPickerDialog = true },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = SurfaceContainerLowest,
                                        contentColor = WarmOnSurface
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("change_avatar_button")
                                ) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = SageTertiary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Choose Avatar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Stats Banner
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard("Recipes", "${recipes.size}", Icons.Default.MenuBook, Modifier.weight(1f))
                        ProfileStatCard("Cookbooks", "${cookbooks.size}", Icons.Default.LibraryBooks, Modifier.weight(1f))
                        ProfileStatCard("Scanned Cards", "${scannedCards.size}", Icons.Default.DocumentScanner, Modifier.weight(1f))
                    }
                }

                // 2. Account Settings & Cooking Preferences
                item {
                    SectionHeader("COOKING & DIETARY PREFERENCES")
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Dietary Preferences Multi-Select Chips
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Dietary Preferences",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = "${userProfile.dietaryPreferences.size} active",
                                        fontSize = 12.sp,
                                        color = SageTertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Auto-tags imported recipes and highlights pantry matches.",
                                    fontSize = 12.sp,
                                    color = WarmOnSurfaceVariant
                                )

                                // Multi-select Flow/Horizontal Chips
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        allDietaryOptions.forEach { option ->
                                            val isSelected = userProfile.dietaryPreferences.contains(option)
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { HeirloomRepository.toggleDietaryPreference(option) },
                                                label = {
                                                    Text(
                                                        text = option,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                leadingIcon = if (isSelected) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                                } else null,
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = SageTertiary,
                                                    selectedLabelColor = Color.White,
                                                    selectedLeadingIconColor = Color.White,
                                                    containerColor = SurfaceContainerLow,
                                                    labelColor = WarmOnSurface
                                                ),
                                                modifier = Modifier.testTag("dietary_chip_$option")
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = SurfaceContainerHighest)

                            // Default Serving Size Preference (Numerical Selector)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Default Serving Size",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = "Household baseline for auto-scaling recipes",
                                        fontSize = 12.sp,
                                        color = WarmOnSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { HeirloomRepository.setDefaultServings(userProfile.defaultServings - 1) },
                                        enabled = userProfile.defaultServings > 1,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceContainerHigh)
                                            .testTag("serving_decrement_button")
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = TerracottaPrimary)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SurfaceContainerLow,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SageTertiary.copy(alpha = 0.5f)),
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = "${userProfile.defaultServings} servings",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = WarmOnSurface,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { HeirloomRepository.setDefaultServings(userProfile.defaultServings + 1) },
                                        enabled = userProfile.defaultServings < 20,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(SurfaceContainerHigh)
                                            .testTag("serving_increment_button")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Increase", tint = TerracottaPrimary)
                                    }
                                }
                            }

                            HorizontalDivider(color = SurfaceContainerHighest)

                            // Measurement Units Selector
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Measurement System",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = WarmOnSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("US Customary", "Metric System", "Imperial").forEach { unit ->
                                        val isSelected = userProfile.measurementUnits.startsWith(unit.split(" ")[0])
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = if (isSelected) TerracottaPrimary else SurfaceContainerLow,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    HeirloomRepository.updateProfile(
                                                        name = userProfile.name,
                                                        title = userProfile.title,
                                                        bio = userProfile.bio,
                                                        email = userProfile.email,
                                                        measurementUnits = unit,
                                                        defaultServings = userProfile.defaultServings,
                                                        keepAwake = userProfile.keepAwakeOnCookMode,
                                                        autoSync = userProfile.autoSyncPantryToGrocery,
                                                        haptic = userProfile.hapticFeedback,
                                                        avatarType = userProfile.avatarType,
                                                        dietaryPreferences = userProfile.dietaryPreferences,
                                                        compactFeedView = userProfile.compactFeedView,
                                                        keepScreenAwakeByDefault = userProfile.keepScreenAwakeByDefault
                                                    )
                                                    Toast.makeText(context, "Units updated to $unit", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Text(
                                                text = unit,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else WarmOnSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Backup & Data Management ("Export My Recipe Tin")
                item {
                    SectionHeader("DATA MANAGEMENT & ARCHIVE BACKUP")
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(SageTertiary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AllInbox,
                                        contentDescription = null,
                                        tint = SageTertiary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Your Recipe Tin Data",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = "${recipes.size} recipes stored locally • Complete data sovereignty",
                                        fontSize = 12.sp,
                                        color = WarmOnSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { showExportTinDialog = true },
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SageTertiary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("export_recipe_tin_button")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export My Recipe Tin (JSON / CSV)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // 4. App Settings & Display Toggles
                item {
                    SectionHeader("APP SETTINGS & DISPLAY")
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Keep Screen Awake by Default
                            ProfileToggleRow(
                                icon = Icons.Default.Lightbulb,
                                title = "Keep Screen Awake by Default",
                                subtitle = "Persists active screen awake during cooking across sessions",
                                checked = userProfile.keepScreenAwakeByDefault,
                                onCheckedChange = { HeirloomRepository.setKeepScreenAwakeByDefault(it) },
                                tag = "toggle_keep_awake"
                            )

                            HorizontalDivider(color = SurfaceContainerHighest)

                            // Compact Feed View
                            ProfileToggleRow(
                                icon = Icons.Default.ViewAgenda,
                                title = "Compact Feed View",
                                subtitle = "Switch between rich recipe cards and a dense list layout",
                                checked = userProfile.compactFeedView,
                                onCheckedChange = { HeirloomRepository.setCompactFeedView(it) },
                                tag = "toggle_compact_feed"
                            )

                            HorizontalDivider(color = SurfaceContainerHighest)

                            // Auto-Sync Pantry to Grocery
                            ProfileToggleRow(
                                icon = Icons.Default.ShoppingCartCheckout,
                                title = "Auto-Add Depleted Pantry to Grocery",
                                subtitle = "Ingredients marked 'Out' jump to the shopping list",
                                checked = userProfile.autoSyncPantryToGrocery,
                                onCheckedChange = {
                                    HeirloomRepository.updateProfile(
                                        name = userProfile.name,
                                        title = userProfile.title,
                                        bio = userProfile.bio,
                                        email = userProfile.email,
                                        measurementUnits = userProfile.measurementUnits,
                                        defaultServings = userProfile.defaultServings,
                                        keepAwake = userProfile.keepAwakeOnCookMode,
                                        autoSync = it,
                                        haptic = userProfile.hapticFeedback,
                                        avatarType = userProfile.avatarType,
                                        dietaryPreferences = userProfile.dietaryPreferences,
                                        compactFeedView = userProfile.compactFeedView,
                                        keepScreenAwakeByDefault = userProfile.keepScreenAwakeByDefault
                                    )
                                },
                                tag = "toggle_pantry_sync"
                            )

                            HorizontalDivider(color = SurfaceContainerHighest)

                            // Haptic Kitchen Timers
                            ProfileToggleRow(
                                icon = Icons.Default.Vibration,
                                title = "Haptic Kitchen Timers & Cues",
                                subtitle = "Gentle tactile feedback when steps complete",
                                checked = userProfile.hapticFeedback,
                                onCheckedChange = {
                                    HeirloomRepository.updateProfile(
                                        name = userProfile.name,
                                        title = userProfile.title,
                                        bio = userProfile.bio,
                                        email = userProfile.email,
                                        measurementUnits = userProfile.measurementUnits,
                                        defaultServings = userProfile.defaultServings,
                                        keepAwake = userProfile.keepAwakeOnCookMode,
                                        autoSync = userProfile.autoSyncPantryToGrocery,
                                        haptic = it,
                                        avatarType = userProfile.avatarType,
                                        dietaryPreferences = userProfile.dietaryPreferences,
                                        compactFeedView = userProfile.compactFeedView,
                                        keepScreenAwakeByDefault = userProfile.keepScreenAwakeByDefault
                                    )
                                },
                                tag = "toggle_haptic"
                            )
                        }
                    }
                }

                // 5. Aesthetic Palette Switcher Section
                item {
                    ThemePaletteSelectorSection()
                }

                // 6. Account Actions (Edit Profile, Sign Out, Delete Account)
                item {
                    SectionHeader("ACCOUNT & PREVIEW ACTIONS")
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            // Discreet Developer / Preview Reset Button
                            AccountActionItem(
                                icon = Icons.Default.RestartAlt,
                                title = "Reset to New User View",
                                subtitle = "Wipe all storage & test fresh onboarding sequence",
                                onClick = { showResetNewUserDialog = true },
                                tag = "reset_to_new_user_view_button"
                            )
                            HorizontalDivider(color = SurfaceContainerHighest)
                            AccountActionItem(
                                icon = Icons.Default.CloudSync,
                                title = "Family Cloud Sync Hub",
                                subtitle = "Manage connected kitchen tablets & devices",
                                onClick = onNavigateToCloudSync
                            )
                            HorizontalDivider(color = SurfaceContainerHighest)
                            AccountActionItem(
                                icon = Icons.Default.Edit,
                                title = "Edit Profile & Bio",
                                subtitle = "Change your display name, title, and motto",
                                onClick = { showEditProfileDialog = true }
                            )
                            HorizontalDivider(color = SurfaceContainerHighest)
                            AccountActionItem(
                                icon = Icons.Default.Logout,
                                title = "Sign Out",
                                subtitle = "Preserve offline recipe tin on this device",
                                onClick = { showSignOutDialog = true }
                            )
                            HorizontalDivider(color = SurfaceContainerHighest)
                            AccountActionItem(
                                icon = Icons.Default.DeleteForever,
                                title = "Clear Data & Reset Tin",
                                subtitle = "Permanently remove all recipes and start fresh",
                                onClick = { showDeleteAccountDialog = true },
                                isDestructive = true
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = TerracottaPrimary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SageTertiary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = value,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = WarmOnSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = WarmOnSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp)
            .testTag(tag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (checked) SageTertiary.copy(alpha = 0.15f) else SurfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) SageTertiary else WarmOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = WarmOnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = WarmOnSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SageTertiary
            )
        )
    }
}

@Composable
private fun AccountActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    tag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isDestructive) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else SurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else SageTertiary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDestructive) MaterialTheme.colorScheme.error else WarmOnSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = WarmOnSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = WarmOutline
        )
    }
}
