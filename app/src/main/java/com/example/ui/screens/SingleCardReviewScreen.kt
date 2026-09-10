package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.HeirloomTopBar
import com.example.ui.theme.*

@Composable
fun SingleCardReviewScreen(
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    onScanBackOfCard: () -> Unit
) {
    val context = LocalContext.current
    var showBoundingBoxes by remember { mutableStateOf(true) }
    var recipeTitle by remember { mutableStateOf("Grandma's Apple Cinnamon Cobbler") }
    var showEditTitleDialog by remember { mutableStateOf(false) }
    var selectedCollection by remember { mutableStateOf("Sunday Family Suppers") }
    var syncPantry by remember { mutableStateOf(true) }
    var verifiedNutmeg by remember { mutableStateOf(false) }

    if (showEditTitleDialog) {
        var tempTitle by remember { mutableStateOf(recipeTitle) }
        AlertDialog(
            onDismissRequest = { showEditTitleDialog = false },
            title = { Text("Edit Recipe Title", fontFamily = FontFamily.Serif) },
            text = {
                OutlinedTextField(
                    value = tempTitle,
                    onValueChange = { tempTitle = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempTitle.isNotBlank()) recipeTitle = tempTitle
                        showEditTitleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitleDialog = false }) {
                    Text("Cancel", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "OCR Review",
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = WarmSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Saved as Formatted Recipe Card!", Toast.LENGTH_SHORT).show()
                            onSaveSuccess()
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_formatted_card_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save as Formatted Recipe Card", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onScanBackOfCard,
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmOnSurface),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.Flip, contentDescription = null, tint = MustardSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan Back of Card", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                Toast.makeText(context, "Retake card photo...", Toast.LENGTH_SHORT).show()
                                onNavigateBack()
                            },
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Retake Photo", fontSize = 13.sp, color = TerracottaPrimary)
                        }
                    }
                }
            }
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("single_card_review_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SageTertiary)
                        )
                        Text(
                            text = "TRANSCRIPTION COMPLETE (94%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SageTertiary,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = SurfaceContainerHigh
                    ) {
                        Text(
                            text = "Inspect Split",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmOnSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Manuscript Card with Bounding Box Overlays
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBXhmiVMPjDih0UFIcx3QCU3SFwojo0yVaIfdC3c-CyV3uqWVuCKlfj9CcC09MTyxtAXecn2aT2hokKWu_Vvf5Q3V0J7Iy3PIcG2XQeGtjh5Op8fEVtW9nCDoXBpHqPt7gsyhRM9yTAkU-7WEo6FYlbzb5WwJnmK0F72rsmOodWCD_ov54Zr6p7tZIKqtuxFYkdrM9tjRD8wX4b9_jy5dbfJmB9O-no-SrcO4Q5eOYFndPXVd653yfs",
                                contentDescription = "Original Handwritten Card",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bounding Box Overlays
                            if (showBoundingBoxes) {
                                // Title Box
                                Box(
                                    modifier = Modifier
                                        .offset(x = 16.dp, y = 14.dp)
                                        .border(1.5.dp, TerracottaPrimary, RoundedCornerShape(4.dp))
                                        .background(TerracottaPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Title 99%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Ingredients Box
                                Box(
                                    modifier = Modifier
                                        .offset(x = 16.dp, y = 60.dp)
                                        .border(1.5.dp, SageTertiary, RoundedCornerShape(4.dp))
                                        .background(SageTertiary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Ingredients 97%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Cursive Deciphered Highlight
                                Box(
                                    modifier = Modifier
                                        .offset(x = 180.dp, y = 120.dp)
                                        .border(1.5.dp, MustardSecondary, RoundedCornerShape(4.dp))
                                        .background(MustardSecondary.copy(alpha = 0.35f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Nutmeg 92%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Controls below image
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = showBoundingBoxes,
                                    onCheckedChange = { showBoundingBoxes = it },
                                    colors = CheckboxDefaults.colors(checkedColor = TerracottaPrimary),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Show Bounding Boxes & Confidence",
                                    fontSize = 11.sp,
                                    color = WarmOnSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Rotated 90°", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.RotateRight, contentDescription = null, tint = WarmOnSurfaceVariant, modifier = Modifier.size(16.dp))
                                Text("Rotate", fontSize = 11.sp, color = WarmOnSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Detected Recipe Header Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DETECTED RECIPE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaPrimary,
                                letterSpacing = 1.sp
                            )
                            IconButton(
                                onClick = { showEditTitleDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Title", tint = WarmOnSurfaceVariant, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = recipeTitle,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = WarmOnSurface
                        )

                        Text(
                            text = "Scanned Oct 14 • 1974 Manuscript Card • 2 Sides",
                            fontSize = 12.sp,
                            color = WarmOnSurfaceVariant
                        )
                    }
                }
            }

            // Cursive Clarity Notice
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MustardSecondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = null,
                            tint = MustardSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Cursive Clarity Assistance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "We deciphered 2 faint cursive phrases: 'pinch of nutmeg' in line 6, and '45–50 minutes' in Step 4. Highlighted below for confirmation.",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Transcribed Ingredients List
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Transcribed Ingredients (7)",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "Confidence",
                                fontSize = 11.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }

                        val sampleIngredients = listOf(
                            Pair("4 cups peeled, sliced apples", "99%"),
                            Pair("1/2 cup granulated sugar", "98%"),
                            Pair("1 tsp ground cinnamon", "99%"),
                            Pair("1 cup all-purpose flour", "95%"),
                            Pair("1/2 cup whole milk", "97%"),
                            Pair("1/4 cup melted butter", "96%")
                        )

                        sampleIngredients.forEach { (name, conf) ->
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
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(TerracottaPrimary)
                                    )
                                    Text(text = name, fontSize = 13.sp, color = WarmOnSurface)
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = TertiaryFixed
                                ) {
                                    Text(
                                        text = conf,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF193616),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = SurfaceContainer)
                        }

                        // Unclear Nutmeg item with verification card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (verifiedNutmeg) TertiaryFixed.copy(alpha = 0.3f) else SecondaryFixed.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (verifiedNutmeg) SageTertiary else MustardSecondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    verifiedNutmeg = !verifiedNutmeg
                                    Toast.makeText(context, if (verifiedNutmeg) "Verified: Pinch of nutmeg confirmed!" else "Reset verification", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Pinch of salt & nutmeg",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = if (verifiedNutmeg) "Verified by user" else "Deciphered with Cursive AI • Tap to verify",
                                        fontSize = 11.sp,
                                        color = if (verifiedNutmeg) SageTertiary else MustardSecondary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (verifiedNutmeg) SageTertiary else MustardSecondary
                                ) {
                                    Text(
                                        text = if (verifiedNutmeg) "VERIFIED" else "92% REVIEW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Parsed Cooking Steps
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Parsed Cooking Steps (4)",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = WarmOnSurface
                        )

                        val steps = listOf(
                            Triple("1", "Prep Apples & Dish", "Mix peeled sliced apples, 1/2 cup sugar, and cinnamon. Spread evenly into buttered 9x9 inch baking dish."),
                            Triple("2", "Make Soft Batter", "In another bowl, stir together flour, sugar, baking powder, milk, and melted butter until smooth. Pour over apples."),
                            Triple("3", "Cinnamon Crust Topping", "Mix remaining 1/4 cup sugar & 1 tsp cinnamon. Generously sprinkle over batter before baking."),
                            Triple("4", "Bake & Serve", "Bake at 350°F (175°C) for 45-50 minutes until golden bubbling crust forms. Serve warm with vanilla ice cream!")
                        )

                        steps.forEach { (num, title, text) ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(TerracottaPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(num, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmOnSurface)
                                    Text(text, fontSize = 13.sp, color = WarmOnSurfaceVariant, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Marginalia & Family Lore Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MustardSecondaryContainer.copy(alpha = 0.35f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MustardSecondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = MustardSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "MARGINALIA & FAMILY LORE CAPTURED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MustardSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "\"Grandma Rose always used extra butter on the dish corners!\"",
                                fontFamily = FontFamily.Serif,
                                fontSize = 14.sp,
                                color = WarmOnSurface,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Captured from handwriting in upper-right margin",
                                fontSize = 11.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recipe Organization & Save Location
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Save to Cookbook Collection",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = WarmOnSurface
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceContainerLowest)
                                .clickable {
                                    selectedCollection = if (selectedCollection == "Sunday Family Suppers") "Vintage Bakes & Pastries" else "Sunday Family Suppers"
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(18.dp))
                                Text(selectedCollection, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = WarmOnSurface)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = WarmOutline)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Checkbox(
                                checked = syncPantry,
                                onCheckedChange = { syncPantry = it },
                                colors = CheckboxDefaults.colors(checkedColor = TerracottaPrimary)
                            )
                            Text("Sync required ingredients against kitchen pantry", fontSize = 12.sp, color = WarmOnSurface)
                        }
                    }
                }
            }
        }
    }
}
