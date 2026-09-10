package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.PdfCookbookExporter
import com.example.data.PdfExportConfig
import com.example.data.PdfLayoutStyle
import com.example.data.Recipe
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun PdfExportDialog(
    allRecipes: List<Recipe>,
    initialSelectedRecipeIds: Set<String> = emptySet(),
    initialTitle: String = "My Heirloom Keepsake Cookbook",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedIds by remember {
        mutableStateOf(
            if (initialSelectedRecipeIds.isNotEmpty()) initialSelectedRecipeIds
            else allRecipes.map { it.id }.toSet()
        )
    }

    var cookbookTitle by remember { mutableStateOf(initialTitle) }
    var subtitle by remember { mutableStateOf("Family recipes & cursive notes preserved with love") }
    var selectedStyle by remember { mutableStateOf(PdfLayoutStyle.HEIRLOOM_KEEPSAKE) }
    var includeCoverPage by remember { mutableStateOf(true) }
    var includeToc by remember { mutableStateOf(true) }
    var includeNotes by remember { mutableStateOf(true) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var activeTab by remember { mutableStateOf(0) } // 0: Select Recipes, 1: Formatting & Cover

    val selectedRecipes = remember(selectedIds, allRecipes) {
        allRecipes.filter { it.id in selectedIds }
    }

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmBackground),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("pdf_export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = TerracottaPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Export Printable Cookbook",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = WarmOnSurface
                            )
                            Text(
                                text = "Formatted PDF for physical printing & keepsakes",
                                fontSize = 11.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.size(32.dp).testTag("pdf_export_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmOnSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Switcher (Select Recipes vs Layout Style)
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = SurfaceContainerHigh,
                    contentColor = TerracottaPrimary,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = {
                            Text(
                                "1. Recipes (${selectedIds.size}/${allRecipes.size})",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("pdf_tab_recipes")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = {
                            Text(
                                "2. Formatting & Cover",
                                fontSize = 12.sp,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("pdf_tab_layout")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Content Area
                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab == 0) {
                        // TAB 0: RECIPE SELECTION
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Select / Deselect All Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Choose recipes to include in the PDF:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = WarmOnSurface
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    TextButton(
                                        onClick = { selectedIds = allRecipes.map { it.id }.toSet() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Select All", fontSize = 11.sp, color = TerracottaPrimary)
                                    }
                                    TextButton(
                                        onClick = { selectedIds = emptySet() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Clear", fontSize = 11.sp, color = WarmOnSurfaceVariant)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                modifier = Modifier.fillMaxSize().testTag("pdf_recipes_list"),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(allRecipes, key = { it.id }) { recipe ->
                                    val isChecked = recipe.id in selectedIds

                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isChecked) SurfaceContainerLowest else SurfaceContainer
                                        ),
                                        border = if (isChecked) androidx.compose.foundation.BorderStroke(1.5.dp, TerracottaPrimary.copy(alpha = 0.5f)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedIds = if (isChecked) {
                                                    selectedIds - recipe.id
                                                } else {
                                                    selectedIds + recipe.id
                                                }
                                            }
                                            .testTag("pdf_recipe_checkbox_${recipe.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    selectedIds = if (checked) selectedIds + recipe.id else selectedIds - recipe.id
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = TerracottaPrimary,
                                                    checkmarkColor = Color.White
                                                )
                                            )

                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
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
                                                    fontSize = 14.sp,
                                                    color = WarmOnSurface,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${recipe.ingredients.size} ingredients • ${recipe.servings} Servings • ${recipe.source}",
                                                    fontSize = 11.sp,
                                                    color = WarmOnSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 1: FORMATTING & COVER OPTIONS
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().testTag("pdf_formatting_options"),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Cookbook Title", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmOnSurface)
                                    OutlinedTextField(
                                        value = cookbookTitle,
                                        onValueChange = { cookbookTitle = it },
                                        placeholder = { Text("e.g. Grandma Rose's Heritage Bakes") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("pdf_title_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Subtitle & Dedication", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmOnSurface)
                                    OutlinedTextField(
                                        value = subtitle,
                                        onValueChange = { subtitle = it },
                                        placeholder = { Text("e.g. Preserved for our family table") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("pdf_subtitle_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Typography & Frame Style", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmOnSurface)

                                    PdfLayoutStyle.entries.forEach { style ->
                                        val isSelected = selectedStyle == style
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) TerracottaPrimary.copy(alpha = 0.08f) else SurfaceContainerLowest
                                            ),
                                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, TerracottaPrimary) else null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedStyle = style }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { selectedStyle = style },
                                                    colors = RadioButtonDefaults.colors(selectedColor = TerracottaPrimary)
                                                )
                                                Column {
                                                    Text(
                                                        text = style.displayName,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = WarmOnSurface
                                                    )
                                                    Text(
                                                        text = style.description,
                                                        fontSize = 11.sp,
                                                        color = WarmOnSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Page Options", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarmOnSurface)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Include Keepsake Cover Page", fontSize = 12.sp, color = WarmOnSurface)
                                            Switch(
                                                checked = includeCoverPage,
                                                onCheckedChange = { includeCoverPage = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TerracottaPrimary)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Include Table of Contents", fontSize = 12.sp, color = WarmOnSurface)
                                            Switch(
                                                checked = includeToc,
                                                onCheckedChange = { includeToc = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TerracottaPrimary)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Include Baker's Notes / Tips", fontSize = 12.sp, color = WarmOnSurface)
                                            Switch(
                                                checked = includeNotes,
                                                onCheckedChange = { includeNotes = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TerracottaPrimary)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Output Banner if generated
                if (generatedPdfFile != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SageTertiary.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SageTertiary),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PDF Generated Successfully!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF23441F)
                                )
                                Text(
                                    text = "${selectedRecipes.size} recipes • Standard A4 Printable",
                                    fontSize = 11.sp,
                                    color = WarmOnSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        generatedPdfFile?.let { PdfCookbookExporter.viewPdf(context, it) }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("pdf_view_btn")
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Preview", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = {
                                        generatedPdfFile?.let {
                                            PdfCookbookExporter.shareOrPrintPdf(context, it, cookbookTitle)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SageTertiary),
                                    shape = RoundedCornerShape(999.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("pdf_print_btn")
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Print / Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Summary & Bottom Action Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedRecipes.size} recipes selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = WarmOnSurface
                        )
                        Text(
                            text = "Est. ${(if (includeCoverPage) 1 else 0) + selectedRecipes.size} pages ready for print",
                            fontSize = 11.sp,
                            color = WarmOnSurfaceVariant
                        )
                    }

                    Button(
                        onClick = {
                            if (selectedRecipes.isEmpty()) {
                                Toast.makeText(context, "Please select at least 1 recipe to export", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isGenerating = true
                            coroutineScope.launch {
                                val config = PdfExportConfig(
                                    title = cookbookTitle.ifBlank { "My Heirloom Keepsake Cookbook" },
                                    subtitle = subtitle.ifBlank { "Family Recipes Preserved with Love" },
                                    layoutStyle = selectedStyle,
                                    includeCoverPage = includeCoverPage,
                                    includeTableOfContents = includeToc,
                                    includeSecretNotes = includeNotes
                                )

                                val result = PdfCookbookExporter.generateCookbookPdf(
                                    context = context,
                                    recipes = selectedRecipes,
                                    config = config
                                )

                                isGenerating = false
                                result.onSuccess { file ->
                                    generatedPdfFile = file
                                    Toast.makeText(context, "PDF generated successfully!", Toast.LENGTH_SHORT).show()
                                    // Trigger print/share chooser
                                    PdfCookbookExporter.shareOrPrintPdf(context, file, config.title)
                                }.onFailure { err ->
                                    Toast.makeText(context, "Failed to generate PDF: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = selectedRecipes.isNotEmpty() && !isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .height(46.dp)
                            .testTag("generate_pdf_submit_btn")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compiling PDF...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate & Export PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
