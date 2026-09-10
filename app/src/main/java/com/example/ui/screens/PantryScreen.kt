package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.GroceryItem
import com.example.data.HeirloomRepository
import com.example.data.PantryItem
import com.example.data.PantryItemStatus
import com.example.ui.components.HeirloomTopBar
import com.example.ui.theme.*

@Composable
fun PantryScreen(
    initialTab: String = "pantry",
    onNavigateToRecipes: () -> Unit,
    onNavigateToScanner: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(initialTab) } // "pantry" or "grocery"
    val pantryItems by HeirloomRepository.pantryItems.collectAsState()
    val groceryItems by HeirloomRepository.groceryItems.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemNote by remember { mutableStateOf("") }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (selectedTab == "pantry") "Add Custom Ingredient" else "Add Grocery Item",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Item Name (e.g. Vanilla Bean)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_item_name_input")
                    )
                    OutlinedTextField(
                        value = newItemNote,
                        onValueChange = { newItemNote = it },
                        label = { Text("Notes (e.g. 2 pods, pantry door)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_item_note_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            if (selectedTab == "pantry") {
                                HeirloomRepository.addCustomPantryItem(newItemName, newItemNote)
                                Toast.makeText(context, "Added $newItemName to pantry!", Toast.LENGTH_SHORT).show()
                            } else {
                                HeirloomRepository.addCustomGroceryItem(newItemName, newItemNote)
                                Toast.makeText(context, "Added $newItemName to grocery list!", Toast.LENGTH_SHORT).show()
                            }
                            newItemName = ""
                            newItemNote = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.testTag("confirm_add_item_btn")
                ) {
                    Text("Add Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = if (selectedTab == "pantry") "Pantry" else "Grocery",
                onProfileClick = onNavigateToProfile,
                onNotificationClick = onNavigateToNotifications
            )
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("pantry_screen_list"),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header & Tab Switcher
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "My Kitchen Pantry",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "Larder, staples & fresh supply",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onNavigateToScanner,
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceContainerHigh),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("pantry_scan_card_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Scan Card",
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Scan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaPrimary
                                )
                            }

                            FilledTonalButton(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = SurfaceContainerHigh),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("add_custom_ingredient_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ Add",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaPrimary
                                )
                            }
                        }
                    }

                    // Retro Segmented Pill Switcher (Pantry vs Grocery)
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selectedTab == "pantry") TerracottaPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = "pantry" }
                                    .testTag("pantry_tab_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Kitchen,
                                        contentDescription = null,
                                        tint = if (selectedTab == "pantry") OnPrimary else WarmOnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pantry (${pantryItems.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedTab == "pantry") OnPrimary else WarmOnSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = if (selectedTab == "grocery") TerracottaPrimary else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = "grocery" }
                                    .testTag("grocery_tab_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = if (selectedTab == "grocery") OnPrimary else WarmOnSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Grocery (${groceryItems.count { !it.isChecked }})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (selectedTab == "grocery") OnPrimary else WarmOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (selectedTab == "pantry") {
                        // Smart Pantry Sync Banner
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MustardSecondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SyncAlt,
                                    contentDescription = null,
                                    tint = MustardSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Smart Pantry Sync: Toggle any ingredient to 'Out' and it automatically jumps onto your grocery shopping list.",
                                    fontSize = 12.sp,
                                    color = WarmOnSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Cook Tonight Banner
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SageTertiaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToRecipes() }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = OnTertiaryContainer,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "You can make 9 saved recipes right now!",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnTertiaryContainer
                                    )
                                }
                                Text(
                                    text = "View >",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // PANTRY TAB VIEW
            if (selectedTab == "pantry") {
                val categories = listOf("Dairy & Cold Shelf", "Baking & Pantry Grains", "Produce & Fresh Herbs")
                categories.forEach { category ->
                    val itemsInCategory = pantryItems.filter { it.category == category }
                    if (itemsInCategory.isNotEmpty()) {
                        item {
                            Text(
                                text = category.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaPrimary,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                            )
                        }

                        items(itemsInCategory) { item ->
                            PantryItemRow(
                                item = item,
                                onStatusChange = { newStatus ->
                                    HeirloomRepository.updatePantryStatus(item.id, newStatus)
                                }
                            )
                        }
                    }
                }

                // Countertop Rituals Illustration Card
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBowSa9SmoadFlFhrPg3EtXReI7IbpLB6TFVYvOvL629oGiPA1KXS7qQZjOwtg0798NDQndcy9ECrh5VFGNohgvKTbisvuYj33aSbuX2w0pZtQa9gAY8QVFWtsmSBdF633F8T0h3uMFhrdfVexCuNZhnt6n5V2Vdm4jS0-ulDJSIvo7MdRLnI4qew_vGSsz2a_FUm8DhzIpChPktXAJMPFaUXgsM2x4LxAeqn_Fi86p67hIwdaCy-wo",
                                    contentDescription = "Kitchen Rituals",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Larder & Kitchen Rituals",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "Keep stoneware crocks and sourdough starters well-tended for spontaneous Sunday baking.",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            } else {
                // GROCERY TAB VIEW
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "AUTO-SYNCED GROCERY LIST",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerracottaPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Clear Checked",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MustardSecondary,
                                modifier = Modifier
                                    .clickable {
                                        HeirloomRepository.clearCheckedGroceryItems()
                                        Toast.makeText(context, "Cleared completed grocery items", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("clear_checked_grocery_btn")
                            )
                        }
                    }
                }

                items(groceryItems) { grocery ->
                    GroceryItemRow(
                        grocery = grocery,
                        onToggle = { HeirloomRepository.toggleGroceryItem(grocery.id) }
                    )
                }

                // Grocery Bottom Export Action
                item {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Exported shopping list to Notes & Messages!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .height(50.dp)
                            .testTag("export_grocery_list_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Grocery List", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PantryItemRow(
    item: PantryItem,
    onStatusChange: (PantryItemStatus) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .testTag("pantry_item_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = WarmOnSurface
                )
                Text(
                    text = item.note,
                    fontSize = 11.sp,
                    color = if (item.status == PantryItemStatus.OUT) TerracottaPrimary else WarmOnSurfaceVariant
                )
            }

            // In / Low / Out 3-State Toggle Pill
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = SurfaceContainerHigh
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    StatusPillOption("In", item.status == PantryItemStatus.IN, SageTertiary) {
                        onStatusChange(PantryItemStatus.IN)
                    }
                    StatusPillOption("Low", item.status == PantryItemStatus.LOW, MustardSecondary) {
                        onStatusChange(PantryItemStatus.LOW)
                    }
                    StatusPillOption("Out", item.status == PantryItemStatus.OUT, TerracottaPrimary) {
                        onStatusChange(PantryItemStatus.OUT)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPillOption(label: String, isSelected: Boolean, activeColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) activeColor else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else WarmOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun GroceryItemRow(
    grocery: GroceryItem,
    onToggle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onToggle)
            .testTag("grocery_item_${grocery.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = grocery.isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = TerracottaPrimary),
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = grocery.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (grocery.isChecked) WarmOutline else WarmOnSurface,
                    textDecoration = if (grocery.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = grocery.note,
                    fontSize = 11.sp,
                    color = WarmOnSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (grocery.tag == "Auto-Pantry") SecondaryFixed else SurfaceContainerHigh
            ) {
                Text(
                    text = grocery.tag,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (grocery.tag == "Auto-Pantry") Color(0xFF432C00) else WarmOnSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
