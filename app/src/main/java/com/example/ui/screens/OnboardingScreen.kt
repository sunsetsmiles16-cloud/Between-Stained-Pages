package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HeirloomRepository
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    onOnboardingCompleted: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var servings by remember { mutableIntStateOf(4) }
    var selectedDietary by remember { mutableStateOf(setOf<String>()) }

    val dietaryOptions = listOf(
        "Vegetarian",
        "Gluten-Free",
        "Dairy-Free",
        "Nut-Free",
        "Vegan",
        "Low Carb",
        "Pescatarian"
    )

    // Soft sage green accent colors specified by design prompt
    val softSageGreen = Color(0xFF62875E)
    val sageBadgeContainer = Color(0xFFD8E4D5)
    val sageBadgeTint = Color(0xFF4A6B46)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .testTag("onboarding_profile_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Cozy Sage Icon Badge
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(sageBadgeContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = sageBadgeTint,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Header & Subheader
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Welcome to Between Stained Pages",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = WarmOnSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Let's set up your personal recipe tin.",
                        fontSize = 15.sp,
                        color = WarmOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )
                }

                HorizontalDivider(
                    color = SurfaceContainerHighest,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // 1. Your Name Input (Required)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Your Name *",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WarmOnSurface
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError && it.isNotBlank()) {
                                nameError = false
                            }
                        },
                        placeholder = {
                            Text(
                                text = "What should we call you in the kitchen?",
                                fontSize = 14.sp,
                                color = WarmOnSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (nameError) MaterialTheme.colorScheme.error else SageTertiary
                            )
                        },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Please enter your name to personalize your recipe tin.") }
                        } else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SageTertiary,
                            unfocusedBorderColor = WarmOutlineVariant,
                            focusedContainerColor = SurfaceContainerLowest,
                            unfocusedContainerColor = SurfaceContainerLowest,
                            errorContainerColor = SurfaceContainerLowest
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )
                }

                // 2. Default Servings Counter
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Default Servings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = WarmOnSurface
                    )
                    Text(
                        text = "Household baseline for automatic ingredient scaling",
                        fontSize = 12.sp,
                        color = WarmOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest)
                            .border(1.dp, WarmOutlineVariant, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                tint = SageTertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "$servings servings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = WarmOnSurface,
                                modifier = Modifier.testTag("onboarding_servings_text")
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { if (servings > 1) servings-- },
                                enabled = servings > 1,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainerHigh)
                                    .testTag("onboarding_servings_decrement")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Servings",
                                    tint = if (servings > 1) TerracottaPrimary else WarmOnSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = { if (servings < 24) servings++ },
                                enabled = servings < 24,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceContainerHigh)
                                    .testTag("onboarding_servings_increment")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Servings",
                                    tint = if (servings < 24) TerracottaPrimary else WarmOnSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Dietary Preferences (Optional Multi-Select Chips)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dietary Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = WarmOnSurface
                        )
                        Text(
                            text = "Optional",
                            fontSize = 12.sp,
                            color = WarmOnSurfaceVariant
                        )
                    }

                    Text(
                        text = "Highlight recipe tags and kitchen pantry staples",
                        fontSize = 12.sp,
                        color = WarmOnSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dietaryOptions.forEach { option ->
                            val isSelected = selectedDietary.contains(option)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedDietary = if (isSelected) {
                                        selectedDietary - option
                                    } else {
                                        selectedDietary + option
                                    }
                                },
                                label = {
                                    Text(
                                        text = option,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = softSageGreen,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = SurfaceContainerLowest,
                                    labelColor = WarmOnSurface
                                ),
                                modifier = Modifier.testTag("onboarding_dietary_chip_$option")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Call-To-Action Button: "Open My Recipe Tin" (Soft Sage Green)
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                        } else {
                            nameError = false
                            HeirloomRepository.completeOnboarding(
                                context = context,
                                name = name,
                                defaultServings = servings,
                                dietaryPreferences = selectedDietary.toList()
                            )
                            // Also mark the welcome modal as seen so user goes straight into tin
                            HeirloomRepository.setSeenWelcomeModal(true, context)
                            onOnboardingCompleted()
                        }
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = softSageGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("open_recipe_tin_button")
                ) {
                    Text(
                        text = "Open My Recipe Tin",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
