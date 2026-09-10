package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.HeirloomRepository
import com.example.ui.components.HeirloomTopBar
import com.example.ui.theme.*

@Composable
fun OrganizeCookbookScreen(
    cookbookId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val cookbooks by HeirloomRepository.cookbooks.collectAsState()
    val allRecipes by HeirloomRepository.recipes.collectAsState()

    val currentBook = cookbooks.find { it.id == cookbookId } ?: cookbooks.firstOrNull()

    if (currentBook == null) {
        Scaffold(
            topBar = {
                HeirloomTopBar(
                    subtitle = "Organize Binder",
                    onBackClick = onNavigateBack
                )
            },
            containerColor = WarmBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No binder selected",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = WarmOnSurface
                    )
                    Text(
                        text = "Create a custom cookbook binder first to organize recipes.",
                        fontSize = 14.sp,
                        color = WarmOnSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                    ) {
                        Text("Return to Shelf")
                    }
                }
            }
        }
        return
    }

    // Local mutable list of recipes for reordering
    val orderedRecipes = remember(currentBook, allRecipes) {
        mutableStateListOf<com.example.data.Recipe>().apply {
            addAll(allRecipes)
        }
    }

    Scaffold(
        topBar = {
            HeirloomTopBar(
                subtitle = "Organize Binder",
                onBackClick = onNavigateBack
            )
        },
        containerColor = WarmBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("organize_cookbook_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "REORDERING BINDER RECIPES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaPrimary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = currentBook.title,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = WarmOnSurface
                        )
                        Text(
                            text = "Use the arrows below to re-sequence the recipe order in this heirloom keepsake binder.",
                            fontSize = 12.sp,
                            color = WarmOnSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            itemsIndexed(orderedRecipes, key = { _, recipe -> recipe.id }) { index, recipe ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = SurfaceContainerHigh,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerracottaPrimary
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
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
                                text = "${recipe.prepTime} prep • ${recipe.cookTime} cook",
                                fontSize = 11.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val item = orderedRecipes.removeAt(index)
                                        orderedRecipes.add(index - 1, item)
                                    }
                                },
                                enabled = index > 0,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Up",
                                    tint = if (index > 0) TerracottaPrimary else WarmOutlineVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (index < orderedRecipes.size - 1) {
                                        val item = orderedRecipes.removeAt(index)
                                        orderedRecipes.add(index + 1, item)
                                    }
                                },
                                enabled = index < orderedRecipes.size - 1,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Down",
                                    tint = if (index < orderedRecipes.size - 1) TerracottaPrimary else WarmOutlineVariant
                                )
                            }
                        }
                    }
                }
            }

            // Save Order Button
            item {
                Button(
                    onClick = {
                        HeirloomRepository.reorderCookbookRecipes(
                            cookbookId = currentBook.id,
                            reorderedRecipeIds = orderedRecipes.map { it.id }
                        )
                        Toast.makeText(context, "Recipe order saved to ${currentBook.title}!", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_cookbook_order_btn")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Binder Sequence", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
