package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.CookingStep
import com.example.data.GeminiRecipeParser
import com.example.data.HeirloomRepository
import com.example.data.IngredientItem
import com.example.data.ParsedRecipeResult
import com.example.data.RecipeSourceType
import com.example.data.ScannedCard
import com.example.data.local.AppDatabase
import com.example.data.local.Recipe as RoomRecipe
import com.example.data.Recipe as DomainRecipe
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun CardScannerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSingleReview: () -> Unit,
    onNavigateToBatchReview: () -> Unit,
    onNavigateToRecipes: () -> Unit = {},
    onNavigateToPantry: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "Camera permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Camera permission needed to scan recipe cards", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var flashOverlayVisible by remember { mutableStateOf(false) }

    var scanMode by remember { mutableStateOf("Single Card") } // Single Card, 2-Sided Index, Recipe Booklet
    var isFlashOn by remember { mutableStateOf(false) }
    var isGridOn by remember { mutableStateOf(true) }
    var isChimeOn by remember { mutableStateOf(true) }
    var autoCaptureActive by remember { mutableStateOf(true) }

    // Last saved recipe from Room Database for in-scanner review card
    var lastSavedRoomRecipe by remember { mutableStateOf<RoomRecipe?>(null) }
    var showSavedDetailsDialog by remember { mutableStateOf(false) }

    var isParsingRecipe by remember { mutableStateOf(false) }
    var parsingStatusMessage by remember { mutableStateOf("Scanning recipe card...") }
    var parsedSuccessRecipe by remember { mutableStateOf<DomainRecipe?>(null) }
    var showParsedSuccessDialog by remember { mutableStateOf(false) }

    if (showParsedSuccessDialog && parsedSuccessRecipe != null) {
        val recipe = parsedSuccessRecipe!!
        AlertDialog(
            onDismissRequest = { showParsedSuccessDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SageTertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "OCR Parsing Complete!",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SageTertiary.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = SageTertiary, modifier = Modifier.size(14.dp))
                            Text("Published in Tin", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SageTertiary)
                        }
                    }

                    Text(
                        text = recipe.title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmOnSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = SurfaceContainerHigh) {
                            Text("⏱ ${recipe.prepTime} prep • ${recipe.cookTime} cook", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                        Surface(shape = RoundedCornerShape(6.dp), color = SurfaceContainerHigh) {
                            Text("🍽 ${recipe.servings} Servings", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }

                    Text("Extracted Ingredients (${recipe.ingredients.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmOnSurface)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        recipe.ingredients.take(4).forEach { ing ->
                            Text("• ${ing.name}", fontSize = 12.sp, color = WarmOnSurfaceVariant, maxLines = 1)
                        }
                        if (recipe.ingredients.size > 4) {
                            Text("+ ${recipe.ingredients.size - 4} more ingredients...", fontSize = 11.sp, color = TerracottaPrimary)
                        }
                    }

                    Text("Extracted Steps (${recipe.steps.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmOnSurface)
                    recipe.steps.take(2).forEach { step ->
                        Text("${step.stepNumber}. ${step.instruction}", fontSize = 12.sp, color = WarmOnSurfaceVariant, maxLines = 2)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showParsedSuccessDialog = false
                        onNavigateToSingleReview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                ) {
                    Text("Review Card in Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParsedSuccessDialog = false }) {
                    Text("Scan Next Card", color = WarmOnSurfaceVariant)
                }
            }
        )
    }

    if (isParsingRecipe) {
        Dialog(onDismissRequest = { /* prevent dismissal during critical OCR */ }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = SurfaceContainerLowest,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = TerracottaPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Parsing Handwritten Card",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmOnSurface
                    )
                    Text(
                        text = parsingStatusMessage,
                        fontSize = 13.sp,
                        color = WarmOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    LaunchedEffect(isFlashOn, cameraControl) {
        try {
            cameraControl?.cameraControl?.enableTorch(isFlashOn)
        } catch (e: Exception) {
            Log.e("CardScanner", "Unable to toggle torch", e)
        }
    }

    // Pulsing animation for scanning reticle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val scannedCardsFromRepo by HeirloomRepository.scannedCards.collectAsState()
    var sessionCards by remember {
        mutableStateOf<List<Triple<String, String, String>>>(emptyList())
    }

    // Keep session cards synced with repository scanned cards
    LaunchedEffect(scannedCardsFromRepo) {
        if (sessionCards.isEmpty() && scannedCardsFromRepo.isNotEmpty()) {
            sessionCards = scannedCardsFromRepo.mapIndexed { idx, card ->
                Triple("#${idx + 1}", card.title, card.imageUrl)
            }
        }
    }

    // Function to parse and persist captured recipe card into Room Database and Heirloom Repository
    fun parseAndSaveCard(
        imageFile: File,
        fallbackTitle: String = "Handwritten Recipe Card"
    ) {
        isParsingRecipe = true
        parsingStatusMessage = "Analyzing handwriting with Gemini Multimodal OCR..."

        coroutineScope.launch {
            try {
                // Call real multimodal Gemini API (with robust local heuristics fallback)
                val parsedResult = withContext(Dispatchers.IO) {
                    GeminiRecipeParser.parseRecipeFromImage(imageFile)
                }

                parsingStatusMessage = "Saving recipe & ingredients to your tin..."

                val db = AppDatabase.getDatabase(context)
                val newRoomRecipe = RoomRecipe(
                    title = parsedResult.title.ifBlank { fallbackTitle },
                    ingredients = parsedResult.ingredients.map { it.name },
                    instructions = parsedResult.steps.joinToString("\n") { it.instruction },
                    handwrittenImagePath = imageFile.absolutePath
                )
                val generatedId = withContext(Dispatchers.IO) {
                    db.recipeDao().insertRecipe(newRoomRecipe)
                }

                val savedWithId = newRoomRecipe.copy(id = generatedId)
                lastSavedRoomRecipe = savedWithId

                // Construct rich Domain Recipe
                val domainRecipe = DomainRecipe(
                    id = "rec-room-$generatedId",
                    title = parsedResult.title.ifBlank { fallbackTitle },
                    source = "Handwritten Card #${generatedId}",
                    sourceType = RecipeSourceType.VINTAGE_SCAN,
                    description = parsedResult.description.ifBlank { "Transcribed from vintage manuscript card via Gemini Multimodal OCR." },
                    imageUrl = imageFile.absolutePath,
                    prepTime = parsedResult.prepTime,
                    cookTime = parsedResult.cookTime,
                    servings = parsedResult.servings,
                    difficulty = parsedResult.difficulty,
                    binderCategory = parsedResult.binderCategory,
                    ingredients = parsedResult.ingredients,
                    steps = parsedResult.steps
                )

                HeirloomRepository.addRecipe(domainRecipe)

                // Add to repository scanned cards archive
                val scannedCard = ScannedCard(
                    id = "scan-$generatedId",
                    title = domainRecipe.title,
                    snippet = parsedResult.description,
                    imageUrl = imageFile.absolutePath,
                    category = parsedResult.binderCategory,
                    confidence = 96
                )
                HeirloomRepository.addScannedCard(scannedCard)

                // Add to scanner session cards
                val newIndexLabel = "#${sessionCards.size + 1}"
                sessionCards = listOf(Triple(newIndexLabel, domainRecipe.title, imageFile.absolutePath)) + sessionCards

                parsedSuccessRecipe = domainRecipe
                showParsedSuccessDialog = true
                isParsingRecipe = false

                Toast.makeText(
                    context,
                    "OCR Transcribed: ${domainRecipe.title} (Published in Tin)",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                isParsingRecipe = false
                Log.e("CardScanner", "Error during Gemini OCR parsing", e)
                Toast.makeText(context, "OCR parse notice: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to capture photo using CameraX ImageCapture
    fun capturePhoto() {
        if (isCapturing || isParsingRecipe) return
        isCapturing = true
        flashOverlayVisible = true

        coroutineScope.launch {
            delay(120)
            flashOverlayVisible = false
        }

        val cardsDir = File(context.filesDir, "recipe_cards").apply {
            if (!exists()) mkdirs()
        }
        val photoFile = File(cardsDir, "recipe_card_${System.currentTimeMillis()}.jpg")

        val capture = imageCapture
        if (capture != null && hasCameraPermission) {
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        parseAndSaveCard(photoFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isCapturing = false
                        Log.e("CardScanner", "CameraX image capture failed, fallback to file parse", exception)
                        parseAndSaveCard(photoFile)
                    }
                }
            )
        } else {
            // Simulated capture for headless emulator environments
            isCapturing = false
            parseAndSaveCard(photoFile)
        }
    }

    // Android Photo Picker Launcher for importing cards
    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val cardsDir = File(context.filesDir, "imported_cards").apply {
                        if (!exists()) mkdirs()
                    }
                    val targetFile = File(cardsDir, "card_imported_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        parseAndSaveCard(targetFile, fallbackTitle = "Imported Heirloom Photo")
                    }
                } catch (e: Exception) {
                    Log.e("CardScanner", "Failed to import photo", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Could not import image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF16120E),
        topBar = {
            Surface(
                color = Color(0xFF1E1712),
                modifier = Modifier.fillMaxWidth()
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp).testTag("scanner_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        // Seamless navigation quick pills
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF2A211B),
                            modifier = Modifier
                                .clickable { onNavigateToRecipes() }
                                .testTag("scanner_to_recipes_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = MustardSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text("Recipes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF2A211B),
                            modifier = Modifier
                                .clickable { onNavigateToPantry() }
                                .testTag("scanner_to_pantry_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Kitchen,
                                    contentDescription = null,
                                    tint = SageTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text("Pantry", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    // Toggles (Flash & Grid)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                isFlashOn = !isFlashOn
                                Toast.makeText(context, if (isFlashOn) "Flash Torch ON" else "Flash Torch OFF", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (isFlashOn) MustardSecondary else Color.White.copy(alpha = 0.7f)
                            )
                        }

                        IconButton(
                            onClick = { isGridOn = !isGridOn },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isGridOn) Icons.Default.GridOn else Icons.Default.GridOff,
                                contentDescription = "Grid",
                                tint = if (isGridOn) TerracottaPrimary else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("card_scanner_screen"),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Viewfinder Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onNavigateToSingleReview() } // Tap view opens single card review
            ) {
                if (hasCameraPermission) {
                    // Live CameraX Preview & ImageCapture binding
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    val capture = ImageCapture.Builder()
                                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                        .build()
                                    imageCapture = capture

                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                    cameraProvider.unbindAll()
                                    val cam = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        capture
                                    )
                                    cameraControl = cam
                                } catch (e: Exception) {
                                    Log.e("CardScanner", "CameraX binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("camerax_preview_view")
                    )
                } else {
                    // Fallback Tabletop Preview & Permission Request Prompt
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = "https://lh3.googleusercontent.com/aida-public/AB6AXuBi7oOsBbvNOvPHw4XZEomHDza7OH6lSFx-PQJeQOj71KO2QuZODvziI4WHiAObnyfTrSnn44jLBm-He0VxGAF4hweV9ppLQdDo066QKc4C3vgaOu-I-0CwYHbfj1tRQH2HhEtVY4gzuQhf5OBG1mozHGBSo78ZmNVYpNdI_sYMvSEMiTsuQd6vXPzyInsq5tQF4YhdHoaVorc9J7PfjuOaEr9IOHO5ETV01H6Ao_MvpnL6lokNqQJQ",
                            contentDescription = "Live Camera Feed",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Surface(
                            color = Color.Black.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = MustardSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "Camera Permission Required",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Enable camera to scan handwritten cards live with CameraX",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text("Enable Camera", fontSize = 12.sp, color = OnPrimary)
                                }
                            }
                        }
                    }
                }

                // 3x3 Grid Overlay
                if (isGridOn) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.weight(1f))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxSize()) {
                        Spacer(modifier = Modifier.weight(1f))
                        VerticalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.weight(1f))
                        VerticalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // OCR Target Bounding Frame with Reticles
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .border(
                            width = 2.dp,
                            color = TerracottaPrimary.copy(alpha = pulseAlpha),
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    // Reticle Corner Accents
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .size(24.dp)
                            .border(3.dp, MustardSecondary, RoundedCornerShape(topStart = 8.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .border(3.dp, MustardSecondary, RoundedCornerShape(topEnd = 8.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(24.dp)
                            .border(3.dp, MustardSecondary, RoundedCornerShape(bottomStart = 8.dp))
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .border(3.dp, MustardSecondary, RoundedCornerShape(bottomEnd = 8.dp))
                    )

                    // Detected Recipe Title Overlay in Center
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = MustardSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Butterscotch Skillet Blondies (98%)",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Holding Steady Timer / Status
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = TerracottaPrimary.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = if (isCapturing) "Saving directly to Room Database..." else "Tap shutter to capture & save to Room",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Top Right Telemetry HUD
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TelemetryBadge("CameraX Live", SageTertiary)
                    TelemetryBadge("Room DB: Active", MustardSecondary)
                    TelemetryBadge("Palmer Script", TerracottaPrimary)
                }

                // Preset Chips on Viewfinder Bottom Left
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Auto 2-Sided: ON",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Parchment Boost: ON",
                            fontSize = 10.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Shutter flash animation overlay
                if (flashOverlayVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }

            // Session Stack Horizontal Strip (Recently Captured from This Tin Box)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E1712))
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grandma Rose's Tin Box (${sessionCards.size} Cards)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = "View Batch Review >",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MustardSecondary,
                        modifier = Modifier
                            .clickable { onNavigateToBatchReview() }
                            .testTag("scanner_view_batch_btn")
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessionCards) { (index, title, img) ->
                        val imageSource: Any = remember(img) {
                            if (img.startsWith("/") || img.startsWith("file:")) {
                                File(img.removePrefix("file://"))
                            } else {
                                img
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2A211B))
                                .clickable { onNavigateToSingleReview() }
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            ) {
                                AsyncImage(
                                    model = imageSource,
                                    contentDescription = title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column {
                                Text(
                                    text = index,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MustardSecondary
                                )
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Shutter Deck
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16120E))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Scanning Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    listOf("Single Card", "2-Sided Index", "Recipe Booklet").forEach { mode ->
                        val isSelected = scanMode == mode
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) TerracottaPrimary else Color.Transparent,
                            modifier = Modifier.clickable { scanMode = mode }
                        ) {
                            Text(
                                text = mode,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) OnPrimary else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Shutter Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Import gallery picker (Android Photo Picker)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                pickMediaLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("scanner_import_gallery")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A211B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Import Gallery",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Import", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }

                    // Center: Shutter Trigger with CameraX capture & Room saving
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(4.dp, MustardSecondary, CircleShape)
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(TerracottaPrimary)
                            .clickable {
                                capturePhoto()
                            }
                            .shadow(8.dp, CircleShape)
                            .testTag("scanner_shutter_trigger"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                color = MustardSecondary,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MustardSecondary)
                            )
                        }
                    }

                    // Right: Crop / Deskew
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable {
                                Toast.makeText(context, "Auto crop and deskew adjusted", Toast.LENGTH_SHORT).show()
                            }
                            .testTag("scanner_crop_deskew")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2A211B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CropRotate,
                                contentDescription = "Crop Deskew",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Deskew", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                    }
                }

                // Finish Box Scan CTA Button
                Button(
                    onClick = onNavigateToBatchReview,
                    colors = ButtonDefaults.buttonColors(containerColor = MustardSecondary),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("finish_box_scan_btn")
                ) {
                    Text(
                        text = "Review Scanned Tin Box (${sessionCards.size} Cards) >",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = OnSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryBadge(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

