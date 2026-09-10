package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeirloomRepository
import com.example.data.NotificationItem
import com.example.data.NotificationType
import com.example.ui.components.HeirloomTopBar
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToPantry: () -> Unit
) {
    val context = LocalContext.current
    val notifications by HeirloomRepository.notifications.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredList = when (selectedFilter) {
        "Family Notes" -> notifications.filter { it.type == NotificationType.FAMILY_NOTE }
        "Pantry Alerts" -> notifications.filter { it.type == NotificationType.PANTRY_ALERT }
        "Cloud & Scans" -> notifications.filter { it.type == NotificationType.CLOUD_SYNC || it.type == NotificationType.SCAN_COMPLETE }
        else -> notifications
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "Notifications",
                onBackClick = onNavigateBack
            )
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("notifications_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Action
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Activity & Kitchen Alerts",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = WarmOnSurface
                        )
                        Text(
                            text = "${notifications.count { !it.isRead }} unread updates",
                            fontSize = 12.sp,
                            color = WarmOnSurfaceVariant
                        )
                    }

                    if (notifications.any { !it.isRead }) {
                        TextButton(
                            onClick = {
                                HeirloomRepository.markAllNotificationsRead()
                                Toast.makeText(context, "All notifications marked as read", Toast.LENGTH_SHORT).show()
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("mark_all_read_btn")
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark all read", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TerracottaPrimary)
                        }
                    }
                }
            }

            // Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Family Notes", "Pantry Alerts", "Cloud & Scans").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) TerracottaPrimary else SurfaceContainerHigh,
                            modifier = Modifier.clickable { selectedFilter = filter }
                        ) {
                            Text(
                                text = filter,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OnPrimary else WarmOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = SageTertiary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "All Caught Up!",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "No new alerts or family activity in this category.",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    NotificationCard(
                        item = item,
                        onClick = {
                            if (item.relatedRecipeId != null) {
                                onNavigateToRecipeDetail(item.relatedRecipeId)
                            } else if (item.type == NotificationType.PANTRY_ALERT) {
                                onNavigateToPantry()
                            }
                        },
                        onDismiss = {
                            HeirloomRepository.dismissNotification(item.id)
                            Toast.makeText(context, "Notification dismissed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    item: NotificationItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val icon = when (item.type) {
        NotificationType.FAMILY_NOTE -> Icons.Default.EditNote
        NotificationType.PANTRY_ALERT -> Icons.Default.ShoppingBag
        NotificationType.SCAN_COMPLETE -> Icons.Default.DocumentScanner
        NotificationType.CLOUD_SYNC -> Icons.Default.CloudDone
    }

    val iconTint = when (item.type) {
        NotificationType.FAMILY_NOTE -> TerracottaPrimary
        NotificationType.PANTRY_ALERT -> MustardSecondary
        NotificationType.SCAN_COMPLETE -> SageTertiary
        NotificationType.CLOUD_SYNC -> Color(0xFF3F69A8)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!item.isRead) SurfaceContainerHigh else SurfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (!item.isRead) 2.dp else 0.dp),
        border = if (!item.isRead) androidx.compose.foundation.BorderStroke(1.dp, TerracottaPrimary.copy(alpha = 0.3f)) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_item_${item.id}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WarmOnSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.timestamp,
                            fontSize = 11.sp,
                            color = WarmOnSurfaceVariant
                        )
                        if (!item.isRead) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TerracottaPrimary)
                            )
                        }
                    }
                }

                Text(
                    text = item.message,
                    fontSize = 12.sp,
                    color = WarmOnSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (item.relatedRecipeId != null || item.type == NotificationType.PANTRY_ALERT) {
                    Text(
                        text = if (item.relatedRecipeId != null) "View Recipe >" else "Open Pantry >",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerracottaPrimary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = WarmOutline,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
