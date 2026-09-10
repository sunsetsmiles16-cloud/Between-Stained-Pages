package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Cookbook
import com.example.data.HeirloomRepository
import com.example.data.Recipe
import com.example.ui.components.HeirloomTopBar
import com.example.ui.components.PdfExportDialog
import com.example.ui.theme.*

@Composable
fun CookbooksScreen(
    onNavigateToRecipeDetail: (String) -> Unit,
    onNavigateToNewCookbook: () -> Unit = {},
    onNavigateToOrganizeCookbook: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val cookbooks by HeirloomRepository.cookbooks.collectAsState()
    val recipes by HeirloomRepository.recipes.collectAsState()

    var selectedCookbookId by remember { mutableStateOf("1") }
    val currentBook = cookbooks.find { it.id == selectedCookbookId } ?: cookbooks.firstOrNull()

    // Local customization state for the selected binder
    var fabricColor by remember(currentBook) { mutableStateOf(currentBook?.fabricColor ?: "terracotta") }
    var foilStamp by remember(currentBook) { mutableStateOf(currentBook?.foilStamp ?: "pot") }
    var typographyChoice by remember(currentBook) { mutableStateOf(currentBook?.typography ?: "groovy") }

    var selectedFilter by remember(cookbooks.size) { mutableStateOf("All Binders (${cookbooks.size})") }
    var showPdfExportDialog by remember { mutableStateOf(false) }

    if (showPdfExportDialog && currentBook != null) {
        PdfExportDialog(
            allRecipes = recipes,
            initialTitle = currentBook.title,
            onDismiss = { showPdfExportDialog = false }
        )
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "Cookbooks",
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
                .testTag("cookbooks_screen_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header & Design New / Export PDF Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "My Digital Cookbooks",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "Craft personalized retro binders & print keepsakes",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { showPdfExportDialog = true },
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("export_cookbook_pdf_header_btn")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(15.dp), tint = TerracottaPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TerracottaPrimary)
                            }

                            Button(
                                onClick = onNavigateToNewCookbook,
                                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                shape = RoundedCornerShape(999.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("design_new_binder_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Design New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All Binders (4)", "Family Keepsakes", "Recent Bakes").forEach { filter ->
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

            // Shelf Grid (dynamic representation of binders)
            if (cookbooks.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .testTag("empty_cookbooks_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = TerracottaPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Your cookbook shelf is empty",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "Design personalized retro binders to organize your family keepsakes, seasonal bakes, and treasured notes.",
                                fontSize = 14.sp,
                                color = WarmOnSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Button(
                                onClick = onNavigateToNewCookbook,
                                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                shape = RoundedCornerShape(999.dp),
                                modifier = Modifier.testTag("empty_state_design_binder_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Design Your First Binder", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        cookbooks.chunked(2).forEach { rowBooks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowBooks.forEach { book ->
                                    CookbookBinderTile(
                                        book = book,
                                        isSelected = selectedCookbookId == book.id,
                                        onSelect = { selectedCookbookId = book.id },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowBooks.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Design Freedom Studio Interactive Customizer Card (when a binder exists)
            if (currentBook != null) {
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
                                Column {
                                    Text(
                                        text = "DESIGN FREEDOM STUDIO",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerracottaPrimary,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        text = "Personalize: ${currentBook.title}",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = WarmOnSurface
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MustardSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // 1. Cover Fabric & Texture Swatches
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("1. Cover Fabric & Texture", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarmOnSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FabricSwatch("Terracotta", TerracottaPrimary, fabricColor == "terracotta") {
                                        fabricColor = "terracotta"
                                    }
                                    FabricSwatch("Avocado", SageTertiary, fabricColor == "avocado") {
                                        fabricColor = "avocado"
                                    }
                                    FabricSwatch("Mustard", MustardSecondaryContainer, fabricColor == "mustard") {
                                        fabricColor = "mustard"
                                    }
                                    FabricSwatch("Oatmeal", SurfaceContainerHighest, fabricColor == "oatmeal") {
                                        fabricColor = "oatmeal"
                                    }
                                }
                            }

                            // 2. Retro Spine Foil Stamp
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("2. Retro Spine Foil Stamp", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarmOnSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FoilStampOption(Icons.Default.SoupKitchen, "Pot", foilStamp == "pot") {
                                        foilStamp = "pot"
                                    }
                                    FoilStampOption(Icons.Default.Grass, "Wheat", foilStamp == "wheat") {
                                        foilStamp = "wheat"
                                    }
                                    FoilStampOption(Icons.Default.BakeryDining, "Pin", foilStamp == "pin") {
                                        foilStamp = "pin"
                                    }
                                    FoilStampOption(Icons.Default.Favorite, "Heart", foilStamp == "heart") {
                                        foilStamp = "heart"
                                    }
                                }
                            }

                            // 3. Cover Title Typography
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("3. Cover Title Typography", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarmOnSurface)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TypoOption("Groovy 70s", FontFamily.Serif, typographyChoice == "groovy") {
                                        typographyChoice = "groovy"
                                    }
                                    TypoOption("Editorial", FontFamily.SansSerif, typographyChoice == "classic") {
                                        typographyChoice = "classic"
                                    }
                                    TypoOption("Scripted", FontFamily.Cursive, typographyChoice == "script") {
                                        typographyChoice = "script"
                                    }
                                }
                            }

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        HeirloomRepository.updateCookbookCustomization(
                                            cookbookId = currentBook.id,
                                            fabricColor = fabricColor,
                                            foilStamp = foilStamp,
                                            typography = typographyChoice
                                        )
                                        Toast.makeText(context, "Binder customization saved!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("save_binder_customization_btn")
                                ) {
                                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        onNavigateToOrganizeCookbook(currentBook.id)
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    modifier = Modifier.height(44.dp).testTag("rearrange_binder_recipes_btn")
                                ) {
                                    Text("Rearrange", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmOnSurface)
                                }

                                Button(
                                    onClick = { showPdfExportDialog = true },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MustardSecondary),
                                    modifier = Modifier.height(44.dp).testTag("customizer_export_pdf_btn")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = OnSecondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = OnSecondary)
                                }
                            }
                        }
                    }
                }

                // Recipes inside Selected Binder
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Inside ${currentBook.title}",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = WarmOnSurface
                        )

                        if (recipes.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = TerracottaPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { showPdfExportDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, tint = TerracottaPrimary, modifier = Modifier.size(13.dp))
                                    Text(
                                        text = "Print Binder (${recipes.take(3).size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TerracottaPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                if (recipes.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No recipes in this binder yet",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = WarmOnSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(recipes.take(3)) { recipe ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToRecipeDetail(recipe.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = recipe.imageUrl,
                                        contentDescription = recipe.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = recipe.title,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = WarmOnSurface,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${recipe.prepTime} prep • ${recipe.cookTime} cook • ${recipe.servings} Servings",
                                        fontSize = 12.sp,
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
                    }
                }
            }
        }
    }
}

@Composable
fun CookbookBinderTile(
    book: Cookbook,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (book.fabricColor) {
        "terracotta" -> TerracottaPrimary
        "avocado" -> SageTertiaryContainer
        "mustard" -> MustardSecondaryContainer
        else -> SurfaceContainerHighest
    }
    val textColor = when (book.fabricColor) {
        "terracotta", "avocado" -> OnPrimary
        else -> WarmOnSurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(3.dp, MustardSecondary) else null,
        modifier = modifier
            .height(170.dp)
            .clickable(onClick = onSelect)
            .testTag("cookbook_tile_${book.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (book.foilStamp) {
                        "wheat" -> Icons.Default.Grass
                        "pin" -> Icons.Default.BakeryDining
                        "heart" -> Icons.Default.Favorite
                        else -> Icons.Default.SoupKitchen
                    },
                    contentDescription = null,
                    tint = if (book.fabricColor == "terracotta" || book.fabricColor == "avocado") SecondaryFixed else MustardSecondary,
                    modifier = Modifier.size(24.dp)
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.Black.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${book.recipeCount}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(
                    text = book.title,
                    fontFamily = when (book.typography) {
                        "script" -> FontFamily.Cursive
                        "classic" -> FontFamily.SansSerif
                        else -> FontFamily.Serif
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = textColor,
                    lineHeight = 20.sp
                )
                Text(
                    text = book.subtitle,
                    fontSize = 11.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
            }

            if (isSelected) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MustardSecondary
                ) {
                    Text(
                        text = "ACTIVE BINDER",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun FabricSwatch(name: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color)
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MustardSecondary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(name, fontSize = 10.sp, color = WarmOnSurface)
    }
}

@Composable
private fun FoilStampOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) MustardSecondaryContainer else SurfaceContainerLowest)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MustardSecondary else SurfaceContainer,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = MustardSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = WarmOnSurface)
    }
}

@Composable
private fun TypoOption(label: String, font: FontFamily, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (isSelected) TerracottaPrimary else SurfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) TerracottaPrimary else SurfaceContainer),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontFamily = font,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) OnPrimary else WarmOnSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
