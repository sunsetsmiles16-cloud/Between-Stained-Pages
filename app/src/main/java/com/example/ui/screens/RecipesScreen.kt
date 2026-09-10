package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.Cookbook
import com.example.data.HeirloomRepository
import com.example.data.Recipe
import com.example.ui.components.HeirloomTopBar
import com.example.ui.components.PdfExportDialog
import com.example.ui.theme.*
import java.io.File

@Composable
fun RecipesScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToCookbooks: () -> Unit,
    onNavigateToPantry: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCloudSync: () -> Unit = {}
) {
    val recipes by HeirloomRepository.recipes.collectAsState()
    val cookbooks by HeirloomRepository.cookbooks.collectAsState()
    val userProfile by HeirloomRepository.userProfile.collectAsState()
    val hasSeenWelcomeModal by HeirloomRepository.hasSeenWelcomeModal.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All Recipes") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showPdfExportDialog by remember { mutableStateOf(false) }
    var showAddRecipeModal by remember { mutableStateOf(false) }

    // Tablet split-screen active selected recipe (defaults to first recipe in tin)
    var selectedRecipeId by remember { mutableStateOf<String?>(recipes.firstOrNull()?.id) }

    // Screen 1: Welcome Sequence Modal on first launch / after reset
    val showWelcomeModal = !hasSeenWelcomeModal && recipes.isEmpty()
    if (showWelcomeModal) {
        Dialog(onDismissRequest = { /* Modal persists until user taps action */ }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = WarmSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .testTag("welcome_modal_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Soft sage icon header
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD8E4D5)), // Soft sage container
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color(0xFF4A6B46), // Soft sage green
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = "Welcome to Stained Pages",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = WarmOnSurface,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Your digital home for loved, tested, and well-worn recipes.",
                        fontSize = 15.sp,
                        color = WarmOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            HeirloomRepository.setSeenWelcomeModal(true, context)
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF62875E), // Soft sage green button
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("welcome_modal_action_btn")
                    ) {
                        Text(
                            text = "Open My Recipe Tin",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Guided "Add First Recipe" Modal
    if (showAddRecipeModal) {
        AlertDialog(
            onDismissRequest = { showAddRecipeModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SoupKitchen,
                        contentDescription = null,
                        tint = TerracottaPrimary
                    )
                    Text(
                        text = "Add Recipe to Tin",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = WarmOnSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Brief, friendly tool-tip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEAF1E8),
                        border = BorderStroke(1.dp, Color(0xFF9FB99B)),
                        modifier = Modifier.fillMaxWidth().testTag("add_recipe_tooltip")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color(0xFF4A6B46),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Paste a web link, social video URL, or take a photo of an old recipe card to begin.",
                                fontSize = 13.sp,
                                color = WarmOnSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    // Import from Link/Video
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddRecipeModal = false
                                onNavigateToImport()
                            }
                            .testTag("add_recipe_modal_import_link")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(TerracottaPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Link,
                                    contentDescription = null,
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text("Web Link or Video URL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmOnSurface)
                                Text("Import from blogs, TikTok, YouTube, or Reels", fontSize = 12.sp, color = WarmOnSurfaceVariant)
                            }
                        }
                    }

                    // Scan Handwritten Card
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SurfaceContainerHigh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddRecipeModal = false
                                onNavigateToScanner()
                            }
                            .testTag("add_recipe_modal_scan_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SageTertiary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF4A6B46),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text("Scan Recipe Card", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmOnSurface)
                                Text("Digitize vintage handwritten cards or book clippings", fontSize = 12.sp, color = WarmOnSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddRecipeModal = false }) {
                    Text("Cancel", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    val filteredRecipes = remember(recipes, searchQuery, selectedFilter) {
        val trimmedQuery = searchQuery.trim()
        recipes.filter { recipe ->
            val matchesQuery = trimmedQuery.isBlank() ||
                    recipe.title.contains(trimmedQuery, ignoreCase = true) ||
                    recipe.ingredients.any { ingredient ->
                        ingredient.name.contains(trimmedQuery, ignoreCase = true) ||
                        ingredient.note.contains(trimmedQuery, ignoreCase = true)
                    } ||
                    recipe.description.contains(trimmedQuery, ignoreCase = true) ||
                    recipe.binderCategory.contains(trimmedQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Favorites" -> recipe.isFavorite || recipe.bookmarked
                "Ready in 30m" -> recipe.prepTime.contains("20") || recipe.cookTime.contains("15") || recipe.prepTime.contains("15")
                "Family Bakes" -> recipe.binderCategory == "Family Bakes" || recipe.title.contains("Cake") || recipe.title.contains("Cobbler") || recipe.title.contains("Focaccia")
                "What Can I Cook? (9)" -> recipe.allIngredientsInPantry
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    if (showPdfExportDialog) {
        PdfExportDialog(
            allRecipes = if (filteredRecipes.isNotEmpty()) filteredRecipes else recipes,
            initialTitle = if (selectedFilter != "All Recipes") "$selectedFilter Recipes" else "My Heirloom Keepsake Cookbook",
            onDismiss = { showPdfExportDialog = false }
        )
    }

    if (showFilterSheet) {
        AlertDialog(
            onDismissRequest = { showFilterSheet = false },
            title = {
                Text(
                    text = "Filter Heirloom Recipes",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Select Meal or Prep Criteria:", fontSize = 12.sp, color = WarmOnSurfaceVariant)
                    listOf("All Recipes", "Favorites", "Ready in 30m", "Family Bakes", "What Can I Cook? (9)").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) TerracottaPrimaryContainer else SurfaceContainerHigh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedFilter = filter
                                    showFilterSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = filter,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) OnPrimaryContainer else WarmOnSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = OnPrimaryContainer, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFilterSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "Recipes",
                onProfileClick = onNavigateToProfile,
                onNotificationClick = onNavigateToNotifications
            )
        },
        floatingActionButton = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                if (recipes.isEmpty()) {
                    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
                    val pulseScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.48f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulse_scale"
                    )
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.65f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1400, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "pulse_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                                alpha = pulseAlpha
                            }
                            .clip(CircleShape)
                            .background(TerracottaPrimary)
                    )
                }

                FloatingActionButton(
                    onClick = { showAddRecipeModal = true },
                    containerColor = TerracottaPrimary,
                    contentColor = OnPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("floating_add_recipe_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add First Recipe",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTablet = maxWidth >= 768.dp

            if (isTablet) {
                // ==================== TABLET SPLIT-SCREEN MASTER-DETAIL LAYOUT ====================
                val selectedRecipe = recipes.find { it.id == selectedRecipeId }

                Row(modifier = Modifier.fillMaxSize()) {
                    // LEFT PANE (35% width): Recipe Feed with Search, Filter Chips, and List
                    Column(
                        modifier = Modifier
                            .weight(0.35f)
                            .fillMaxHeight()
                            .background(WarmSurface)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("recipes_feed_list"),
                            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Left Pane Header & Search
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Recipe Tin",
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp,
                                                color = WarmOnSurface
                                            )
                                            Text(
                                                text = "${filteredRecipes.size} recipes available",
                                                fontSize = 12.sp,
                                                color = WarmOnSurfaceVariant
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            IconButton(
                                                onClick = onNavigateToImport,
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("extract_new_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.AddCircle,
                                                    contentDescription = "Add Recipe",
                                                    tint = TerracottaPrimary,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { showPdfExportDialog = true },
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .testTag("export_recipes_pdf_button")
                                            ) {
                                                Icon(
                                                    Icons.Default.PictureAsPdf,
                                                    contentDescription = "Export PDF",
                                                    tint = TerracottaPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Search Bar in Left Pane
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceContainerHigh)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = "Search",
                                            tint = TerracottaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        TextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = {
                                                Text(
                                                    "Search tin...",
                                                    fontSize = 12.sp,
                                                    color = WarmOutline
                                                )
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                disabledContainerColor = Color.Transparent,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("recipe_search_input")
                                        )
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = { searchQuery = "" },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("recipe_search_clear_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear Search",
                                                    tint = WarmOnSurfaceVariant,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else {
                                            IconButton(
                                                onClick = { showFilterSheet = true },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("recipe_filter_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Tune,
                                                    contentDescription = "Filter",
                                                    tint = TerracottaPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Filter Chips Horizontal Scroll
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val filters = listOf(
                                            "All Recipes",
                                            "Favorites",
                                            "Ready in 30m",
                                            "Family Bakes",
                                            "What Can I Cook? (9)"
                                        )
                                        filters.forEach { filter ->
                                            val isSelected = selectedFilter == filter
                                            Surface(
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (isSelected) TerracottaPrimary else SurfaceContainer,
                                                modifier = Modifier
                                                    .clickable { selectedFilter = filter }
                                                    .testTag("filter_chip_$filter")
                                            ) {
                                                Text(
                                                    text = filter,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) OnPrimary else WarmOnSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Left Pane Recipe List Items
                            if (recipes.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        WoodenRecipeBoxIllustration()
                                        Text(
                                            text = "Your recipe tin is empty!",
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = WarmOnSurface,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Tap the + button below to add your first recipe from a link, video, or photo.",
                                            fontSize = 13.sp,
                                            color = WarmOnSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                        Button(
                                            onClick = { showAddRecipeModal = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .testTag("tablet_empty_state_add_recipe_btn")
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Add First Recipe", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else if (filteredRecipes.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No matching recipes in tin",
                                            fontSize = 13.sp,
                                            color = WarmOnSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                items(filteredRecipes) { recipe ->
                                    val isSelected = (recipe.id == selectedRecipeId)
                                    CompactRecipeFeedCard(
                                        recipe = recipe,
                                        isSelected = isSelected,
                                        onCardClick = { selectedRecipeId = recipe.id },
                                        onFavoriteToggle = { HeirloomRepository.toggleFavorite(recipe.id) }
                                    )
                                }
                            }
                        }
                    }

                    // Vertical Divider between Panes
                    VerticalDivider(color = SurfaceContainerHigh, thickness = 1.dp)

                    // RIGHT PANE (65% width): Active Recipe Detail View or Cozy Empty State
                    Box(
                        modifier = Modifier
                            .weight(0.65f)
                            .fillMaxHeight()
                            .background(WarmBackground)
                    ) {
                        if (selectedRecipe == null) {
                            // Cozy empty state graphic in the right pane
                            TabletEmptyDetailPane(
                                onSelectFirstRecipe = {
                                    selectedRecipeId = filteredRecipes.firstOrNull()?.id ?: recipes.firstOrNull()?.id
                                },
                                totalRecipeCount = recipes.size,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Active Recipe Detail View formatted with multi-column layout
                            RecipeDetailPaneContent(
                                recipe = selectedRecipe,
                                isMultiColumn = true,
                                showTopBar = false,
                                onNavigateBack = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                // ==================== SMARTPHONE SINGLE-COLUMN LAYOUT ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("recipes_feed_list"),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Header Greeting & Search Section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { onNavigateToPantry() }
                                            .testTag("recipe_to_pantry_badge")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = MustardSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "KITCHEN PANTRY ACTIVE >",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp,
                                            color = MustardSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val greetingPrefix = remember {
                                        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                                        when {
                                            hour < 12 -> "Good morning"
                                            hour < 17 -> "Good afternoon"
                                            else -> "Good evening"
                                        }
                                    }
                                    val displayName = userProfile.name.ifBlank { "Home Cook" }
                                    Text(
                                        text = "$greetingPrefix, $displayName",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 28.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = "What are we simmering on the stove today?",
                                        fontSize = 13.sp,
                                        color = WarmOnSurfaceVariant
                                    )
                                }

                                // Retro Skillet emblem
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceContainerHigh)
                                        .shadow(2.dp, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DinnerDining,
                                        contentDescription = "Active Stove",
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 4.dp, end = 4.dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(MustardSecondary)
                                    )
                                }
                            }

                            // Search bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SurfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = {
                                        Text(
                                            "Search by title or ingredient (e.g. vanilla, pie)...",
                                            fontSize = 13.sp,
                                            color = WarmOutline
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("recipe_search_input")
                                )
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("recipe_search_clear_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear Search",
                                            tint = WarmOnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = { showFilterSheet = true },
                                        modifier = Modifier.size(32.dp).testTag("recipe_filter_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = "Filter",
                                            tint = TerracottaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Search Active Indicator
                            if (searchQuery.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Found ${filteredRecipes.size} ${if (filteredRecipes.size == 1) "recipe" else "recipes"} matching \"${searchQuery.trim()}\"",
                                        fontSize = 12.sp,
                                        color = TerracottaPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Clear",
                                        fontSize = 12.sp,
                                        color = WarmOnSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { searchQuery = "" }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .testTag("recipe_search_clear_text")
                                    )
                                }
                            }

                            // Horizontal Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val filters = listOf(
                                    "All Recipes",
                                    "Favorites",
                                    "Ready in 30m",
                                    "Family Bakes",
                                    "What Can I Cook? (9)"
                                )
                                filters.forEach { filter ->
                                    val isSelected = selectedFilter == filter
                                    val isWhatCanICook = filter.contains("What Can I Cook")

                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = when {
                                            isSelected -> TerracottaPrimary
                                            isWhatCanICook -> TertiaryFixed
                                            else -> SurfaceContainer
                                        },
                                        modifier = Modifier
                                            .clickable { selectedFilter = filter }
                                            .testTag("filter_chip_$filter")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (filter == "Favorites") {
                                                Icon(
                                                    Icons.Filled.Favorite,
                                                    contentDescription = null,
                                                    tint = if (isSelected) OnPrimary else Color(0xFFD32F2F),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            } else if (filter == "Ready in 30m") {
                                                Icon(
                                                    Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = if (isSelected) OnPrimary else MustardSecondary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            } else if (filter == "Family Bakes") {
                                                Icon(
                                                    Icons.Default.Cake,
                                                    contentDescription = null,
                                                    tint = if (isSelected) OnPrimary else TerracottaPrimary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            } else if (isWhatCanICook) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = SageTertiary,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            Text(
                                                text = filter,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected || isWhatCanICook) FontWeight.Bold else FontWeight.Medium,
                                                color = when {
                                                    isSelected -> OnPrimary
                                                    isWhatCanICook -> Color(0xFF193616)
                                                    else -> WarmOnSurfaceVariant
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Smart OCR & Video Parser Promotion Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("extract_promo_card")
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(TerracottaPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoFixHigh,
                                                contentDescription = null,
                                                tint = OnPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "SMART OCR & VIDEO PARSER",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 1.sp,
                                                color = TerracottaPrimary
                                            )
                                            Text(
                                                text = "Transcribe Heirloom Recipes",
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = WarmOnSurface
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Drop a TikTok or Reel link, scan grandma's handwritten index card, or snap a photo of any cookbook page.",
                                        fontSize = 13.sp,
                                        color = WarmOnSurfaceVariant,
                                        lineHeight = 18.sp
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            onClick = onNavigateToImport,
                                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                            shape = RoundedCornerShape(999.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .testTag("extract_new_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircle,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Extract New",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }

                                        FilledIconButton(
                                            onClick = { showPdfExportDialog = true },
                                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceContainerLowest),
                                            modifier = Modifier
                                                .size(46.dp)
                                                .shadow(1.dp, CircleShape)
                                                .testTag("export_recipes_pdf_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = "Export PDF Cookbook",
                                                tint = TerracottaPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }

                                        FilledIconButton(
                                            onClick = onNavigateToScanner,
                                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceContainerLowest),
                                            modifier = Modifier
                                                .size(46.dp)
                                                .shadow(1.dp, CircleShape)
                                                .testTag("camera_scan_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = "Scan Card",
                                                tint = TerracottaPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Pinned Cookbooks Section Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Pinned Cookbooks",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        color = WarmOnSurface
                                    )
                                }

                                Text(
                                    text = "See all (4)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaPrimary,
                                    modifier = Modifier
                                        .clickable { onNavigateToCookbooks() }
                                        .testTag("see_all_cookbooks")
                                )
                            }

                            // Pinned Cookbooks Horizontal Scroll
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                cookbooks.take(2).forEach { book ->
                                    PinnedCookbookCard(book = book, onClick = onNavigateToCookbooks)
                                }
                            }

                            // Recent Extractions Section Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HistoryEdu,
                                        contentDescription = null,
                                        tint = TerracottaPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Search Results (${filteredRecipes.size})" else "Recent Extractions",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 20.sp,
                                        color = WarmOnSurface
                                    )
                                }

                                Text(
                                    text = if (searchQuery.isNotBlank()) "Filtered" else "Archived ${recipes.size}",
                                    fontSize = 12.sp,
                                    color = WarmOnSurfaceVariant
                                )
                            }
                        }
                    }

                    // Recipe Items Feed or Empty Search State
                    if (recipes.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                                    .testTag("empty_tin_state")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    WoodenRecipeBoxIllustration()

                                    Text(
                                        text = "Your recipe tin is empty!",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                        color = WarmOnSurface,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Tap the + button below to add your first recipe from a link, video, or photo.",
                                        fontSize = 14.sp,
                                        color = WarmOnSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 20.sp
                                    )
                                    Button(
                                        onClick = { showAddRecipeModal = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .testTag("empty_state_add_recipe_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add First Recipe", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else if (filteredRecipes.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SurfaceContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                                    .testTag("empty_search_state")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SearchOff,
                                        contentDescription = null,
                                        tint = WarmOutline,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Text(
                                        text = "No recipes found",
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = WarmOnSurface
                                    )
                                    Text(
                                        text = "No saved recipes match \"$searchQuery\" in title or ingredients.",
                                        fontSize = 13.sp,
                                        color = WarmOnSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = { searchQuery = "" },
                                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                        shape = RoundedCornerShape(999.dp),
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Text("Clear Search", fontSize = 13.sp, color = OnPrimary)
                                    }
                                }
                            }
                        }
                    } else {
                        items(filteredRecipes) { recipe ->
                            if (userProfile.compactFeedView) {
                                CompactRecipeFeedCard(
                                    recipe = recipe,
                                    onCardClick = { onNavigateToDetail(recipe.id) },
                                    onFavoriteToggle = { HeirloomRepository.toggleFavorite(recipe.id) }
                                )
                            } else {
                                RecipeFeedCard(
                                    recipe = recipe,
                                    onCardClick = { onNavigateToDetail(recipe.id) },
                                    onFavoriteToggle = { HeirloomRepository.toggleFavorite(recipe.id) }
                                )
                            }
                        }
                    }

                    // Bottom Cloud Sync Info Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                                .clickable { onNavigateToCloudSync() }
                                .testTag("recipe_cloud_sync_card")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MustardSecondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = OnSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Family Recipe Cloud Sync",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = WarmOnSurface
                                        )
                                        Text(
                                            text = "Archived across 4 shared kitchen devices • Tap to manage",
                                            fontSize = 12.sp,
                                            color = WarmOnSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open Cloud Sync Hub",
                                    tint = TerracottaPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cozy Empty State graphic in the Right Pane when no recipe is selected on tablet
 */
@Composable
fun TabletEmptyDetailPane(
    onSelectFirstRecipe: () -> Unit,
    totalRecipeCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmBackground)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .testTag("tablet_empty_detail_state")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cozy Cottagecore Emblem
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(TertiaryFixed.copy(alpha = 0.8f))
                        .shadow(2.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFF193616),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = TerracottaPrimaryContainer
                ) {
                    Text(
                        text = "HEIRLOOM KITCHEN TIN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnPrimaryContainer,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Select a recipe from your tin to start cooking.",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = WarmOnSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Text(
                    text = "Choose any heirloom card from the left panel to view scaled ingredients, step-by-step instructions, and active kitchen timers.",
                    fontSize = 14.sp,
                    color = WarmOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                    modifier = Modifier.widthIn(max = 420.dp)
                )

                if (totalRecipeCount > 0) {
                    Button(
                        onClick = onSelectFirstRecipe,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Open First Recipe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PinnedCookbookCard(
    book: Cookbook,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cookbookCardSpringScale"
    )

    val isTerracotta = book.fabricColor == "terracotta"
    val bgColor = if (isTerracotta) TerracottaPrimary else MustardSecondaryContainer
    val textColor = if (isTerracotta) OnPrimary else OnSecondaryContainer

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 1.dp else 3.dp),
        modifier = Modifier
            .width(250.dp)
            .height(200.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .testTag("pinned_cookbook_${book.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isTerracotta) Icons.Default.MenuBook else Icons.Default.SoupKitchen,
                    contentDescription = null,
                    tint = if (isTerracotta) PrimaryFixed else MustardSecondary,
                    modifier = Modifier.size(28.dp)
                )

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isTerracotta) Color.White.copy(alpha = 0.2f) else SurfaceContainerLowest.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "${book.recipeCount} Recipes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "VOLUME I • AUTUMN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = textColor.copy(alpha = 0.8f)
                )
                Text(
                    text = book.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = textColor,
                    lineHeight = 26.sp
                )
                Text(
                    text = book.description,
                    fontSize = 12.sp,
                    color = textColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(PrimaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("JD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TerracottaPrimary)
                    }
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(SecondaryFixed),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MustardSecondary)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Open Volume",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeFeedCard(
    recipe: Recipe,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onBookmarkToggle: () -> Unit = onFavoriteToggle
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Subtle spring-based scale feedback on click/press
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "recipeCardSpringScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 1.dp else if (isSelected) 3.dp else 2.5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "recipeCardSpringElevation"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceContainerHighest else SurfaceContainerLowest
        ),
        border = if (isSelected) BorderStroke(2.dp, TerracottaPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onCardClick
            )
            .testTag("recipe_feed_card_${recipe.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Hero Image Container with Badges
            val imageSource: Any? = remember(recipe.imageUrl) {
                if (recipe.imageUrl.isNotBlank() && (recipe.imageUrl.startsWith("/") || recipe.imageUrl.startsWith("file:"))) {
                    File(recipe.imageUrl.removePrefix("file://"))
                } else if (recipe.imageUrl.isNotBlank()) {
                    recipe.imageUrl
                } else {
                    null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer)
            ) {
                if (imageSource != null) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recipe_image_preview_${recipe.id}")
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = WarmOutline,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Recipe Card",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }
                    }
                }

                // Top Left Time & Servings badges
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = WarmSurface.copy(alpha = 0.92f),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MustardSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = recipe.prepTime,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmOnSurface
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = WarmSurface.copy(alpha = 0.92f),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = SageTertiary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${recipe.servings} Servings",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmOnSurface
                            )
                        }
                    }
                }

                // Top Right Heart Favorite Button
                val isFav = recipe.isFavorite || recipe.bookmarked
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WarmSurface.copy(alpha = 0.94f))
                        .shadow(2.dp, CircleShape)
                        .testTag("favorite_button_${recipe.id}")
                ) {
                    Icon(
                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                        tint = if (isFav) Color(0xFFD32F2F) else WarmOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Source Label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = when (recipe.sourceType) {
                        com.example.data.RecipeSourceType.TIKTOK -> Icons.Default.Videocam
                        com.example.data.RecipeSourceType.INSTAGRAM -> Icons.Default.Share
                        com.example.data.RecipeSourceType.VINTAGE_SCAN -> Icons.Default.PhotoCamera
                        else -> Icons.Default.Link
                    },
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = recipe.source,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerracottaPrimary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Title
            Text(
                text = recipe.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = WarmOnSurface,
                lineHeight = 22.sp
            )

            // Description
            Text(
                text = recipe.description,
                fontSize = 13.sp,
                color = WarmOnSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = SurfaceContainer)
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom row: Pantry ingredients match & Cook CTA
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
                            .background(if (recipe.allIngredientsInPantry) SageTertiary else MustardSecondary)
                    )
                    Text(
                        text = recipe.pantryStatusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (recipe.allIngredientsInPantry) SageTertiary else MustardSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable { onCardClick() }
                ) {
                    Text(
                        text = "Cook",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerracottaPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Custom composable card component for the recipe list screen that displays the recipe title
 * and an image preview loaded from the saved file path (or remote URL).
 */
@Composable
fun RecipeCardWithImagePreview(
    title: String,
    imagePath: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.965f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "previewCardSpringScale"
    )

    val elevation by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 1.dp else 2.5.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "previewCardSpringElevation"
    )

    val imageSource: Any? = remember(imagePath) {
        if (!imagePath.isNullOrBlank() && (imagePath.startsWith("/") || imagePath.startsWith("file:"))) {
            File(imagePath.removePrefix("file://"))
        } else if (!imagePath.isNullOrBlank()) {
            imagePath
        } else {
            null
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            )
            .testTag("recipe_card_with_image_preview")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Image Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceContainer)
            ) {
                if (imageSource != null) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("recipe_preview_image")
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = WarmOutline,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Handwritten Card",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }
                    }
                }

                // Top Right Heart Favorite Button
                if (onFavoriteToggle != null) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WarmSurface.copy(alpha = 0.94f))
                            .shadow(2.dp, CircleShape)
                            .testTag("recipe_card_favorite_btn")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFD32F2F) else WarmOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Recipe Title
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = WarmOnSurface,
                lineHeight = 22.sp,
                modifier = Modifier.testTag("recipe_preview_title")
            )

            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = WarmOnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RecipePreviewCard(
    title: String,
    imagePath: String?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    RecipeCardWithImagePreview(title, imagePath, modifier, subtitle, isFavorite, onFavoriteToggle, onClick)
}

@Composable
fun CompactRecipeFeedCard(
    recipe: Recipe,
    onCardClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceContainerHighest else SurfaceContainerLowest
        ),
        border = if (isSelected) BorderStroke(2.dp, TerracottaPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("compact_recipe_card_${recipe.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mini Thumbnail or Category Icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainerHigh)
            ) {
                if (recipe.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestaurantMenu,
                            contentDescription = null,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recipe.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = WarmOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = recipe.binderCategory,
                        fontSize = 10.sp,
                        color = SageTertiary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = WarmOnSurfaceVariant
                    )
                    Text(
                        text = "${recipe.cookTime} • ${recipe.servings} srv",
                        fontSize = 10.sp,
                        color = WarmOnSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("compact_fav_button_${recipe.id}")
            ) {
                Icon(
                    imageVector = if (recipe.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (recipe.isFavorite) TerracottaPrimary else WarmOnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Cozy custom illustration of a vintage wooden recipe tin box with cream divider tabs and antique brass latch.
 */
@Composable
fun WoodenRecipeBoxIllustration(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(110.dp, 84.dp)
            .testTag("wooden_recipe_box_illustration")
    ) {
        val w = size.width
        val h = size.height

        // Vintage cream index divider tabs peeking out of open tin
        drawRoundRect(
            color = Color(0xFFE5DDD0),
            topLeft = Offset(w * 0.18f, h * 0.08f),
            size = Size(w * 0.28f, h * 0.24f),
            cornerRadius = CornerRadius(8f, 8f)
        )
        drawRoundRect(
            color = Color(0xFFF3EFE6),
            topLeft = Offset(w * 0.52f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.22f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Wooden recipe tin body (honey oak)
        drawRoundRect(
            color = Color(0xFFB5703C),
            topLeft = Offset(w * 0.08f, h * 0.26f),
            size = Size(w * 0.84f, h * 0.68f),
            cornerRadius = CornerRadius(14f, 14f)
        )

        // Top lid rim bevel / shadow
        drawRoundRect(
            color = Color(0xFF9E5C2C),
            topLeft = Offset(w * 0.06f, h * 0.24f),
            size = Size(w * 0.88f, h * 0.12f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        drawRoundRect(
            color = Color(0xFFC7834C),
            topLeft = Offset(w * 0.07f, h * 0.23f),
            size = Size(w * 0.86f, h * 0.07f),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Front panel inset border (wood craftsmanship)
        drawRoundRect(
            color = Color(0xFFA66330),
            topLeft = Offset(w * 0.14f, h * 0.40f),
            size = Size(w * 0.72f, h * 0.48f),
            cornerRadius = CornerRadius(10f, 10f)
        )

        // Antique brass nameplate / latch
        drawRoundRect(
            color = Color(0xFFD4AF37),
            topLeft = Offset(w * 0.43f, h * 0.44f),
            size = Size(w * 0.14f, h * 0.22f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Brass screw / keyhole accent
        drawCircle(
            color = Color(0xFF4A341E),
            radius = 3.5f,
            center = Offset(w * 0.5f, h * 0.55f)
        )
    }
}
