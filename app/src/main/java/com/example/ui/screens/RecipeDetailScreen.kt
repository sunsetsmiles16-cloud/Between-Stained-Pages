package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
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
import com.example.data.CookingStep
import com.example.data.HeirloomRepository
import com.example.data.IngredientItem
import com.example.data.Recipe
import com.example.ui.components.PdfExportDialog
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun RecipeDetailScreen(
    recipeId: String,
    onNavigateBack: () -> Unit
) {
    val recipes by HeirloomRepository.recipes.collectAsState()
    val recipe = recipes.find { it.id == recipeId } ?: recipes.firstOrNull()

    if (recipe == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(WarmBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("Recipe not found", color = WarmOnSurface)
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmBackground)
    ) {
        val isWideScreen = maxWidth >= 768.dp

        RecipeDetailPaneContent(
            recipe = recipe,
            isMultiColumn = isWideScreen,
            showTopBar = true,
            onNavigateBack = onNavigateBack,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Reusable Recipe Detail Pane Content.
 * Formats the recipe into a responsive layout:
 * - When isMultiColumn is true (Tablet / Desktop):
 *   Two side-by-side sub-columns:
 *     Left Column: Ingredients checklist with Serving Size Scaler at the top.
 *     Right Column: Step-by-step Instructions with Keep Screen Awake toggle bar fixed at the top.
 * - When isMultiColumn is false (Smartphone):
 *   Single column stacked view with the scaler and awake toggle.
 */
@Composable
fun RecipeDetailPaneContent(
    recipe: Recipe,
    modifier: Modifier = Modifier,
    isMultiColumn: Boolean = true,
    showTopBar: Boolean = false,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val recipes by HeirloomRepository.recipes.collectAsState()

    var keepAwake by remember { mutableStateOf(true) }
    var servings by remember(recipe.id) { mutableStateOf(recipe.servings) }
    var checkedIngredients by remember(recipe.id) { mutableStateOf(setOf<String>()) }
    var showPdfExportDialog by remember { mutableStateOf(false) }

    // Screen Keep Awake implementation via WindowManager flags
    DisposableEffect(keepAwake) {
        val window = (context as? android.app.Activity)?.window
        if (keepAwake) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Active Kitchen Timer State
    var timerRunning by remember { mutableStateOf(false) }
    var timerSecondsRemaining by remember { mutableStateOf(12 * 60 + 30) }
    var timerLabel by remember { mutableStateOf("Simmer Reduction") }

    // Coroutine countdown effect for the timer
    LaunchedEffect(timerRunning, timerSecondsRemaining) {
        if (timerRunning && timerSecondsRemaining > 0) {
            delay(1000L)
            timerSecondsRemaining -= 1
        } else if (timerRunning && timerSecondsRemaining == 0) {
            timerRunning = false
            Toast.makeText(context, "Timer finished for $timerLabel!", Toast.LENGTH_LONG).show()
        }
    }

    if (showPdfExportDialog) {
        PdfExportDialog(
            allRecipes = recipes,
            initialSelectedRecipeIds = setOf(recipe.id),
            initialTitle = recipe.title,
            onDismiss = { showPdfExportDialog = false }
        )
    }

    val isFav = recipe.isFavorite || recipe.bookmarked

    Scaffold(
        containerColor = WarmBackground,
        topBar = {
            if (showTopBar) {
                Surface(
                    color = WarmSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(56.dp)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("recipe_detail_back_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = WarmOnSurface
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(36.dp))
                        }

                        // Keep Awake Pill (in top bar)
                        KeepAwakePill(
                            keepAwake = keepAwake,
                            onToggle = {
                                keepAwake = !keepAwake
                                Toast.makeText(
                                    context,
                                    if (keepAwake) "Screen Keep-Awake ON" else "Screen Keep-Awake OFF",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                        // Top bar Action Icons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { HeirloomRepository.toggleFavorite(recipe.id) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("recipe_detail_favorite_btn")
                            ) {
                                Icon(
                                    imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = if (isFav) "Remove from favorites" else "Add to favorites",
                                    tint = if (isFav) Color(0xFFD32F2F) else WarmOnSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Shared recipe: ${recipe.title}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = WarmOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { showPdfExportDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("recipe_detail_print_pdf_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Print,
                                    contentDescription = "Export & Print PDF",
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("recipe_detail_content"),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = if (showTopBar) 12.dp else 16.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. RECIPE HEADER CARD: Archival Tag, Title, Image & Action Controls
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = TerracottaPrimary
                            ) {
                                Text(
                                    text = "HEIRLOOM ARCHIVAL RECIPE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimary,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = SurfaceContainerHigh
                                ) {
                                    Text(
                                        text = recipe.binderCategory,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmOnSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                if (!showTopBar) {
                                    // Action buttons for Embedded Pane mode
                                    IconButton(
                                        onClick = { HeirloomRepository.toggleFavorite(recipe.id) },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("recipe_detail_favorite_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) Color(0xFFD32F2F) else WarmOnSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showPdfExportDialog = true },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("recipe_detail_print_pdf_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Print,
                                            contentDescription = "Print PDF",
                                            tint = TerracottaPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = recipe.title,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = WarmOnSurface,
                            lineHeight = 32.sp,
                            modifier = Modifier.testTag("recipe_detail_title")
                        )

                        if (recipe.description.isNotBlank()) {
                            Text(
                                text = recipe.description,
                                fontSize = 13.sp,
                                color = WarmOnSurfaceVariant,
                                lineHeight = 19.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Source: ${recipe.source}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SageTertiary
                            )

                            if (recipe.pantryStatusText.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = TertiaryFixed
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF193616),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = recipe.pantryStatusText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF193616)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. RECIPE TIMES & METRICS ROW
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetaPill(
                        icon = Icons.Default.Schedule,
                        label = "Prep Time",
                        value = recipe.prepTime,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("meta_prep_time")
                    )
                    MetaPill(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Cook Time",
                        value = recipe.cookTime,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("meta_cook_time")
                    )
                    MetaPill(
                        icon = Icons.Default.Group,
                        label = "Base Yield",
                        value = "${recipe.servings} Servings",
                        modifier = Modifier.weight(1f)
                    )
                    MetaPill(
                        icon = Icons.Default.Speed,
                        label = "Difficulty",
                        value = recipe.difficulty,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. MULTI-COLUMN INGREDIENT & INSTRUCTION VIEW (or Single Column for Smartphone)
            if (isMultiColumn) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // ==================== LEFT SUB-COLUMN: INGREDIENTS & SCALER ====================
                        Column(
                            modifier = Modifier
                                .weight(0.44f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Serving Size Scaler Bar at the top of left column
                            ServingSizeScalerCard(
                                currentServings = servings,
                                baseServings = recipe.servings,
                                onServingsChange = { servings = it }
                            )

                            // Ingredients Checklist
                            IngredientsChecklistCard(
                                ingredients = recipe.ingredients,
                                checkedIngredients = checkedIngredients,
                                currentServings = servings,
                                baseServings = recipe.servings,
                                onToggleIngredient = { ingredientId ->
                                    checkedIngredients = if (checkedIngredients.contains(ingredientId)) {
                                        checkedIngredients - ingredientId
                                    } else {
                                        checkedIngredients + ingredientId
                                    }
                                }
                            )
                        }

                        // ==================== RIGHT SUB-COLUMN: INSTRUCTIONS & AWAKE TOGGLE ====================
                        Column(
                            modifier = Modifier
                                .weight(0.56f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // "Keep Screen Awake" Toggle Bar fixed at the top of right column
                            KeepAwakeToggleBar(
                                keepAwake = keepAwake,
                                onToggle = {
                                    keepAwake = !keepAwake
                                    Toast.makeText(
                                        context,
                                        if (keepAwake) "Screen Keep-Awake ON: Screen will stay on" else "Screen Keep-Awake OFF",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )

                            // Active Kitchen Timer Card (if running)
                            if (timerRunning) {
                                ActiveKitchenTimerCard(
                                    timerLabel = timerLabel,
                                    secondsRemaining = timerSecondsRemaining,
                                    onStopTimer = { timerRunning = false },
                                    onAddMinute = { timerSecondsRemaining += 60 }
                                )
                            }

                            // Step-by-Step Instructions Section
                            InstructionsSection(
                                steps = recipe.steps,
                                onStartStepTimer = { durationMin, stepTitle ->
                                    timerLabel = stepTitle
                                    timerSecondsRemaining = durationMin * 60
                                    timerRunning = true
                                    Toast.makeText(
                                        context,
                                        "Timer started for $durationMin min: $stepTitle",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onStartGuidedCook = {
                                    timerRunning = true
                                    Toast.makeText(
                                        context,
                                        "Guided cooking active! Timer started.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // ==================== SINGLE COLUMN LAYOUT (SMARTPHONE) ====================
                item {
                    ServingSizeScalerCard(
                        currentServings = servings,
                        baseServings = recipe.servings,
                        onServingsChange = { servings = it }
                    )
                }

                item {
                    IngredientsChecklistCard(
                        ingredients = recipe.ingredients,
                        checkedIngredients = checkedIngredients,
                        currentServings = servings,
                        baseServings = recipe.servings,
                        onToggleIngredient = { ingredientId ->
                            checkedIngredients = if (checkedIngredients.contains(ingredientId)) {
                                checkedIngredients - ingredientId
                            } else {
                                checkedIngredients + ingredientId
                            }
                        }
                    )
                }

                item {
                    KeepAwakeToggleBar(
                        keepAwake = keepAwake,
                        onToggle = {
                            keepAwake = !keepAwake
                            Toast.makeText(
                                context,
                                if (keepAwake) "Screen Keep-Awake ON: Screen will stay on" else "Screen Keep-Awake OFF",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }

                if (timerRunning) {
                    item {
                        ActiveKitchenTimerCard(
                            timerLabel = timerLabel,
                            secondsRemaining = timerSecondsRemaining,
                            onStopTimer = { timerRunning = false },
                            onAddMinute = { timerSecondsRemaining += 60 }
                        )
                    }
                }

                item {
                    InstructionsSection(
                        steps = recipe.steps,
                        onStartStepTimer = { durationMin, stepTitle ->
                            timerLabel = stepTitle
                            timerSecondsRemaining = durationMin * 60
                            timerRunning = true
                            Toast.makeText(
                                context,
                                "Timer started for $durationMin min: $stepTitle",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onStartGuidedCook = {
                            timerRunning = true
                            Toast.makeText(
                                context,
                                "Guided cooking active! Timer started.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Serving Size Scaler Component
 */
@Composable
fun ServingSizeScalerCard(
    currentServings: Int,
    baseServings: Int,
    onServingsChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scaleMultiplier = currentServings.toFloat() / (if (baseServings > 0) baseServings else 1)
    val isScaled = currentServings != baseServings

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("serving_scaler_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        imageVector = Icons.Default.Scale,
                        contentDescription = null,
                        tint = SageTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Serving Size Scaler",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = WarmOnSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isScaled) TerracottaPrimaryContainer else SurfaceContainerHigh
                ) {
                    Text(
                        text = if (isScaled) "${String.format(java.util.Locale.US, "%.1fx", scaleMultiplier)} Scaled" else "Base ($baseServings srv)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isScaled) OnPrimaryContainer else WarmOnSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adjust Portions:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = WarmOnSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Decrement Button
                    FilledIconButton(
                        onClick = { if (currentServings > 1) onServingsChange(currentServings - 1) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceContainerLowest),
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(1.dp, CircleShape)
                            .testTag("serving_decrement_button")
                    ) {
                        Text(
                            text = "-",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (currentServings > 1) WarmOnSurface else WarmOutline
                        )
                    }

                    Text(
                        text = "$currentServings ${if (currentServings == 1) "Serving" else "Servings"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmOnSurface,
                        modifier = Modifier.widthIn(min = 80.dp)
                    )

                    // Increment Button
                    FilledIconButton(
                        onClick = { onServingsChange(currentServings + 1) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = SurfaceContainerLowest),
                        modifier = Modifier
                            .size(34.dp)
                            .shadow(1.dp, CircleShape)
                            .testTag("serving_increment_button")
                    ) {
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = WarmOnSurface
                        )
                    }

                    if (isScaled) {
                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaPrimary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onServingsChange(baseServings) }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Ingredients Checklist Component
 */
@Composable
fun IngredientsChecklistCard(
    ingredients: List<IngredientItem>,
    checkedIngredients: Set<String>,
    currentServings: Int,
    baseServings: Int,
    onToggleIngredient: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = ingredients.count { checkedIngredients.contains(it.id) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recipe_ingredients_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Ingredients (${ingredients.size})",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = WarmOnSurface
                    )
                }

                Text(
                    text = "$completedCount of ${ingredients.size} prepped",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (completedCount == ingredients.size && ingredients.isNotEmpty()) SageTertiary else WarmOnSurfaceVariant
                )
            }

            HorizontalDivider(color = SurfaceContainer)

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ingredients.forEach { item ->
                    val isChecked = checkedIngredients.contains(item.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleIngredient(item.id) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleIngredient(item.id) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SageTertiary,
                                uncheckedColor = WarmOutline
                            ),
                            modifier = Modifier.size(20.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                                fontSize = 14.sp,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (isChecked) WarmOutline else WarmOnSurface
                            )
                            if (item.note.isNotBlank()) {
                                Text(
                                    text = item.note,
                                    fontSize = 11.sp,
                                    color = WarmOnSurfaceVariant
                                )
                            }
                        }

                        if (item.inPantry) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = TertiaryFixed
                            ) {
                                Text(
                                    text = "In Pantry",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF193616),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (item.isMarkedOut) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SecondaryFixed
                            ) {
                                Text(
                                    text = "On List",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF432C00),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    HorizontalDivider(color = SurfaceContainerHigh.copy(alpha = 0.5f))
                }
            }
        }
    }
}

/**
 * Keep Screen Awake Banner Component
 */
@Composable
fun KeepAwakeToggleBar(
    keepAwake: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (keepAwake) TertiaryFixed else SurfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("keep_awake_toggle_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (keepAwake) Color(0xFF193616) else SurfaceContainerLowest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (keepAwake) Icons.Default.LightMode else Icons.Default.ModeNight,
                        contentDescription = null,
                        tint = if (keepAwake) OnPrimary else WarmOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = "Keep Screen Awake",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (keepAwake) Color(0xFF193616) else WarmOnSurface
                    )
                    Text(
                        text = if (keepAwake) "Screen stays illuminated while cooking" else "Normal system sleep timeout active",
                        fontSize = 11.sp,
                        color = if (keepAwake) Color(0xFF2A5226) else WarmOnSurfaceVariant
                    )
                }
            }

            Switch(
                checked = keepAwake,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = OnPrimary,
                    checkedTrackColor = Color(0xFF193616),
                    uncheckedThumbColor = WarmOutline,
                    uncheckedTrackColor = SurfaceContainerLowest
                ),
                modifier = Modifier.testTag("keep_awake_switch")
            )
        }
    }
}

/**
 * Top Bar Pill for Keep Awake
 */
@Composable
fun KeepAwakePill(
    keepAwake: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (keepAwake) TertiaryFixed else SurfaceContainerHigh,
        modifier = Modifier.clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (keepAwake) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = null,
                tint = if (keepAwake) Color(0xFF193616) else WarmOnSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = if (keepAwake) "Awake: ON" else "Awake: OFF",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (keepAwake) Color(0xFF193616) else WarmOnSurfaceVariant
            )
        }
    }
}

/**
 * Active Kitchen Timer Countdown Card
 */
@Composable
fun ActiveKitchenTimerCard(
    timerLabel: String,
    secondsRemaining: Int,
    onStopTimer: () -> Unit,
    onAddMinute: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TerracottaPrimaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_timer_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TerracottaPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = timerLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = OnPrimaryContainer
                    )
                    Text(
                        text = timeFormatted,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TerracottaPrimary
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = onAddMinute,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = OnPrimaryContainer),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+1 min", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onStopTimer,
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Stop", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = OnPrimary)
                }
            }
        }
    }
}

/**
 * Step-by-Step Instructions Section
 */
@Composable
fun InstructionsSection(
    steps: List<CookingStep>,
    onStartStepTimer: (Int, String) -> Unit,
    onStartGuidedCook: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                    imageVector = Icons.Default.FormatListNumbered,
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Instructions (${steps.size} Steps)",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = WarmOnSurface
                )
            }
        }

        steps.forEach { step ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recipe_step_${step.stepNumber}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(TerracottaPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${step.stepNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OnPrimary
                                )
                            }
                            Text(
                                text = step.title,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = WarmOnSurface
                            )
                        }

                        if (step.durationMinutes != null) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SurfaceContainerHigh
                            ) {
                                Text(
                                    text = "${step.durationMinutes} min",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmOnSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = step.instruction,
                        fontSize = 14.sp,
                        color = WarmOnSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    if (step.durationMinutes != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    onStartStepTimer(
                                        step.durationMinutes,
                                        step.timerLabel ?: step.title
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Start ${step.durationMinutes}:00 Timer",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = onStartGuidedCook,
            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("start_cook_mode_btn")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Start Guided Cook Mode",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MetaPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = TerracottaPrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = WarmOnSurface
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = WarmOnSurfaceVariant
            )
        }
    }
}
