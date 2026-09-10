package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.HeirloomRepository
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String) {
    data object Auth : Screen("auth", "Sign In")
    data object Onboarding : Screen("onboarding", "Profile Setup")
    data object Recipes : Screen("recipes", "Recipes")
    data object Import : Screen("import", "Import")
    data object Cookbooks : Screen("cookbooks", "Cookbooks")
    data object Pantry : Screen("pantry", "Pantry")
    data object Grocery : Screen("grocery", "Grocery")
    data object CardScanner : Screen("card_scanner", "Card Scanner")
    data object BatchReview : Screen("batch_review", "OCR Review")
    data object SingleCardReview : Screen("single_card_review", "Card Detail")
    data object Profile : Screen("profile", "Profile & Settings")
    data object Notifications : Screen("notifications", "Notifications")
    data object CloudSync : Screen("cloud_sync", "Cloud Sync")
    data object NewCookbook : Screen("new_cookbook", "Design Binder")
    data object OrganizeCookbook : Screen("organize_cookbook/{cookbookId}", "Organize Binder") {
        fun createRoute(cookbookId: String) = "organize_cookbook/$cookbookId"
    }
    data object RecipeDetail : Screen("recipe_detail/{recipeId}", "Recipe Detail") {
        fun createRoute(recipeId: String) = "recipe_detail/$recipeId"
    }
}

@Composable
fun HeirloomTopBar(
    subtitle: String,
    onBackClick: (() -> Unit)? = null,
    onProfileClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    var showThemeDialog by remember { mutableStateOf(false) }

    if (showThemeDialog) {
        ThemeSwitcherDialog(onDismissRequest = { showThemeDialog = false })
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("top_bar_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    // Vintage Well-Loved Cookbook Logo Emblem
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_between_stained_pages_logo),
                            contentDescription = "Between Stained Pages Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column {
                    Text(
                        text = "Between Stained Pages",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 18.sp
                    )
                    Text(
                        text = subtitle.uppercase(),
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Palette Switcher Action Button
                IconButton(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.size(40.dp).testTag("top_bar_theme_switcher")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Palette,
                        contentDescription = "Color Theme Switcher",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier.size(40.dp).testTag("top_bar_notifications")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val userProfile by HeirloomRepository.userProfile.collectAsState()

                // Circular Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable { onProfileClick() }
                        .testTag("top_bar_profile"),
                    contentAlignment = Alignment.Center
                ) {
                    if (userProfile.avatarType == "custom_photo" && userProfile.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = userProfile.avatarUrl,
                            contentDescription = "User Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val icon = when (userProfile.avatarType) {
                            "flour_sifter" -> Icons.Default.BakeryDining
                            "vintage_apron" -> Icons.Default.DryCleaning
                            "rolling_pin" -> Icons.Default.TakeoutDining
                            "dutch_oven" -> Icons.Default.OutdoorGrill
                            else -> Icons.Default.SoupKitchen
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = "User Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeirloomBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    groceryCount: Int = 3
) {
    val isScannerActive = currentRoute == Screen.CardScanner.route
    val barColor = if (isScannerActive) Color(0xFF1E1712) else MaterialTheme.colorScheme.surface
    val activeColor = if (isScannerActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val inactiveColor = if (isScannerActive) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        color = barColor,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Recipes Tab
            BottomNavItem(
                title = "Recipes",
                icon = if (currentRoute == Screen.Recipes.route) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                isSelected = currentRoute == Screen.Recipes.route,
                selectedColor = activeColor,
                unselectedColor = inactiveColor,
                onClick = { onNavigate(Screen.Recipes.route) },
                testTag = "tab_recipes"
            )

            // Cookbooks Tab
            BottomNavItem(
                title = "Cookbooks",
                icon = if (currentRoute == Screen.Cookbooks.route) Icons.Filled.CollectionsBookmark else Icons.Outlined.CollectionsBookmark,
                isSelected = currentRoute == Screen.Cookbooks.route,
                selectedColor = activeColor,
                unselectedColor = inactiveColor,
                onClick = { onNavigate(Screen.Cookbooks.route) },
                testTag = "tab_cookbooks"
            )

            // Center Elevated Action Button (Scanner Camera Interface)
            Box(
                modifier = Modifier
                    .offset(y = (-10).dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isScannerActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onNavigate(Screen.CardScanner.route)
                    }
                    .shadow(6.dp, CircleShape)
                    .testTag("tab_center_scan"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = "Scanner Camera",
                    tint = if (isScannerActive) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Pantry Tab
            BottomNavItem(
                title = "Pantry",
                icon = if (currentRoute == Screen.Pantry.route) Icons.Filled.Kitchen else Icons.Outlined.Kitchen,
                isSelected = currentRoute == Screen.Pantry.route,
                selectedColor = activeColor,
                unselectedColor = inactiveColor,
                onClick = { onNavigate(Screen.Pantry.route) },
                testTag = "tab_pantry"
            )

            // Grocery Tab with badge
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onNavigate(Screen.Grocery.route)
                    }
                    .testTag("tab_grocery"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box {
                        Icon(
                            imageVector = if (currentRoute == Screen.Grocery.route) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                            contentDescription = "Grocery",
                            tint = if (currentRoute == Screen.Grocery.route) activeColor else inactiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                        if (groceryCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-4).dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$groceryCount",
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Grocery",
                        fontSize = 11.sp,
                        fontWeight = if (currentRoute == Screen.Grocery.route) FontWeight.Bold else FontWeight.Medium,
                        color = if (currentRoute == Screen.Grocery.route) activeColor else inactiveColor
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    unselectedColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) selectedColor else unselectedColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else unselectedColor
            )
        }
    }
}
