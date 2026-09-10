package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeviceSyncItem
import com.example.data.HeirloomRepository
import com.example.ui.components.HeirloomTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CloudSyncScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncDevices by HeirloomRepository.syncDevices.collectAsState()
    val lastSyncTime by HeirloomRepository.lastSyncTimestamp.collectAsState()

    var isSyncing by remember { mutableStateOf(false) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteEmail by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf("Family Chef & Editor") }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = {
                Text(
                    text = "Invite Family Member",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "They will gain access to view, edit marginalia, and add scanned cards to the family binders.",
                        fontSize = 12.sp,
                        color = WarmOnSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inviteEmail,
                        onValueChange = { inviteEmail = it },
                        label = { Text("Family Member's Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("invite_email_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteEmail.isNotBlank()) {
                            Toast.makeText(context, "Invitation sent to $inviteEmail!", Toast.LENGTH_SHORT).show()
                            inviteEmail = ""
                            showInviteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.testTag("send_invite_confirm_btn")
                ) {
                    Text("Send Invite")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("Cancel", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "Cloud Sync Hub",
                onBackClick = onNavigateBack
            )
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("cloud_sync_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(SageTertiary)
                                )
                                Text(
                                    text = if (isSyncing) "SYNCHRONIZING..." else "CLOUD ENCRYPTED & ACTIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = if (isSyncing) MustardSecondary else SageTertiary
                                )
                            }

                            Text(
                                text = "Vault ID: #HL-8821",
                                fontSize = 11.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }

                        Text(
                            text = "Family Recipe Cloud Sync",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = WarmOnSurface
                        )

                        Text(
                            text = "Every vintage card scan, handwritten marginalia note, and audio recording is continuously synchronized across all household devices.",
                            fontSize = 13.sp,
                            color = WarmOnSurfaceVariant,
                            lineHeight = 17.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Last synced: $lastSyncTime",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    if (!isSyncing) {
                                        isSyncing = true
                                        coroutineScope.launch {
                                            delay(1200)
                                            HeirloomRepository.triggerCloudSync()
                                            isSyncing = false
                                            Toast.makeText(context, "Cloud sync completed successfully!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                shape = RoundedCornerShape(999.dp),
                                enabled = !isSyncing,
                                modifier = Modifier.testTag("sync_now_btn")
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = OnPrimary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Syncing...")
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sync Now")
                                }
                            }
                        }
                    }
                }
            }

            // Storage Metrics Breakdown
            item {
                Text(
                    text = "VAULT STORAGE ALLOCATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerracottaPrimary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Archive Usage", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmOnSurface)
                            Text("4.2 MB / 15 GB Used", fontSize = 12.sp, color = TerracottaPrimary, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { 0.12f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = TerracottaPrimary,
                            trackColor = SurfaceContainerHighest
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StorageTypePill("Vintage Scans", "3.1 MB", TerracottaPrimary)
                            StorageTypePill("Audio Notes", "0.8 MB", MustardSecondary)
                            StorageTypePill("Recipes Data", "0.3 MB", SageTertiary)
                        }
                    }
                }
            }

            // Connected Family Devices
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONNECTED FAMILY DEVICES (${syncDevices.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerracottaPrimary,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { showInviteDialog = true },
                        modifier = Modifier.size(32.dp).testTag("invite_device_btn")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Member", tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            items(syncDevices) { device ->
                DeviceCard(device = device)
            }

            // Invite Family Member CTA
            item {
                Button(
                    onClick = { showInviteDialog = true },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("invite_family_member_btn")
                ) {
                    Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Invite Family Member to Binder", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun StorageTypePill(label: String, size: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = "$label ($size)",
            fontSize = 11.sp,
            color = WarmOnSurfaceVariant
        )
    }
}

@Composable
private fun DeviceCard(device: DeviceSyncItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (device.isCurrentDevice) TerracottaPrimaryContainer else SurfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        device.deviceType.contains("Tablet", ignoreCase = true) -> Icons.Default.TabletAndroid
                        device.deviceType.contains("Display", ignoreCase = true) -> Icons.Default.Tv
                        else -> Icons.Default.Smartphone
                    },
                    contentDescription = null,
                    tint = if (device.isCurrentDevice) OnPrimaryContainer else WarmOnSurface,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.deviceName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WarmOnSurface
                    )
                    if (device.isCurrentDevice) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = TerracottaPrimary
                        ) {
                            Text(
                                text = "This Device",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = OnPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "${device.deviceType} • ${device.lastSyncTime}",
                    fontSize = 11.sp,
                    color = WarmOnSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Synced",
                tint = SageTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
