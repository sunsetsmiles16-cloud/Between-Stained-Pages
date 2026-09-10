package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.components.HeirloomTopBar
import com.example.ui.theme.*

@Composable
fun NewCookbookScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("Sunday Supper Keepsakes") }
    var subtitle by remember { mutableStateOf("Family traditions & slow roasts") }
    var description by remember { mutableStateOf("Preserving autumn roasts, hearty sourdoughs, and heirloom family recipes.") }
    var fabricColor by remember { mutableStateOf("terracotta") }
    var foilStamp by remember { mutableStateOf("pot") }
    var typography by remember { mutableStateOf("groovy") }

    val binderBgColor = when (fabricColor) {
        "terracotta" -> TerracottaPrimary
        "avocado" -> SageTertiaryContainer
        "mustard" -> MustardSecondaryContainer
        "navy" -> Color(0xFF2C3E50)
        "forest" -> Color(0xFF2E4032)
        else -> SurfaceContainerHighest
    }
    val binderTextColor = when (fabricColor) {
        "terracotta", "avocado", "navy", "forest" -> OnPrimary
        else -> WarmOnSurface
    }
    val foilTint = when (fabricColor) {
        "terracotta", "navy", "forest" -> MustardSecondary
        else -> TerracottaPrimary
    }

    val font = when (typography) {
        "groovy" -> FontFamily.Serif
        "classic" -> FontFamily.SansSerif
        else -> FontFamily.Cursive
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "Design New Binder",
                onBackClick = onNavigateBack
            )
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("new_cookbook_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Interactive Preview
            item {
                Text(
                    text = "LIVE BINDER PREVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerracottaPrimary,
                    letterSpacing = 1.sp
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = binderBgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (foilStamp) {
                                    "wheat" -> Icons.Default.Grass
                                    "pin" -> Icons.Default.BakeryDining
                                    "heart" -> Icons.Default.Favorite
                                    "apple" -> Icons.Default.Eco
                                    else -> Icons.Default.SoupKitchen
                                },
                                contentDescription = null,
                                tint = foilTint,
                                modifier = Modifier.size(32.dp)
                            )

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.Black.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "0 Recipes • New",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = binderTextColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = title.ifBlank { "Untitled Binder" },
                                fontFamily = font,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = binderTextColor,
                                lineHeight = 26.sp,
                                maxLines = 2
                            )
                            Text(
                                text = subtitle.ifBlank { "Curated Collection" },
                                fontSize = 12.sp,
                                color = binderTextColor.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Binder Details Form
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
                        Text(
                            text = "Binder Identity",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = WarmOnSurface
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Cookbook Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_cookbook_title_input")
                        )

                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text("Subtitle / Era (e.g. 1970s Sunday Bakes)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_cookbook_subtitle_input")
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Curator Note / Story") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Fabric & Color Picker
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "1. Cover Fabric & Colorway",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = WarmOnSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FabricOption("Terracotta", TerracottaPrimary, fabricColor == "terracotta") {
                                fabricColor = "terracotta"
                            }
                            FabricOption("Avocado", SageTertiary, fabricColor == "avocado") {
                                fabricColor = "avocado"
                            }
                            FabricOption("Mustard", MustardSecondary, fabricColor == "mustard") {
                                fabricColor = "mustard"
                            }
                            FabricOption("Navy", Color(0xFF2C3E50), fabricColor == "navy") {
                                fabricColor = "navy"
                            }
                            FabricOption("Forest", Color(0xFF2E4032), fabricColor == "forest") {
                                fabricColor = "forest"
                            }
                            FabricOption("Oatmeal", SurfaceContainerHighest, fabricColor == "oatmeal") {
                                fabricColor = "oatmeal"
                            }
                        }

                        HorizontalDivider(color = WarmOutlineVariant.copy(alpha = 0.4f))

                        Text(
                            text = "2. Retro Spine Foil Stamp",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = WarmOnSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StampOption(Icons.Default.SoupKitchen, "Pot", foilStamp == "pot") { foilStamp = "pot" }
                            StampOption(Icons.Default.Grass, "Wheat", foilStamp == "wheat") { foilStamp = "wheat" }
                            StampOption(Icons.Default.BakeryDining, "Pin", foilStamp == "pin") { foilStamp = "pin" }
                            StampOption(Icons.Default.Favorite, "Heart", foilStamp == "heart") { foilStamp = "heart" }
                            StampOption(Icons.Default.Eco, "Apple", foilStamp == "apple") { foilStamp = "apple" }
                        }

                        HorizontalDivider(color = WarmOutlineVariant.copy(alpha = 0.4f))

                        Text(
                            text = "3. Typography Style",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = WarmOnSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TypoCard("Groovy 70s", FontFamily.Serif, typography == "groovy") { typography = "groovy" }
                            TypoCard("Editorial", FontFamily.SansSerif, typography == "classic") { typography = "classic" }
                            TypoCard("Scripted", FontFamily.Cursive, typography == "script") { typography = "script" }
                        }
                    }
                }
            }

            // Create Action Button
            item {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            HeirloomRepository.createCookbook(
                                title = title,
                                subtitle = subtitle,
                                description = description,
                                fabricColor = fabricColor,
                                foilStamp = foilStamp,
                                typography = typography
                            )
                            Toast.makeText(context, "Cookbook '$title' created successfully!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(context, "Please enter a binder title", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("create_new_binder_submit_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create & Bind Cookbook", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun FabricOption(label: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = if (color == SurfaceContainerHighest) WarmOnSurface else Color.White, modifier = Modifier.size(20.dp))
            }
        }
        Text(text = label, fontSize = 11.sp, color = if (isSelected) TerracottaPrimary else WarmOnSurfaceVariant, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun StampOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TerracottaPrimaryContainer else SurfaceContainerHigh,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) OnPrimaryContainer else WarmOnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) OnPrimaryContainer else WarmOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun TypoCard(label: String, font: FontFamily, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) TerracottaPrimary else SurfaceContainerHigh,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Text(
            text = label,
            fontFamily = font,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isSelected) OnPrimary else WarmOnSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
