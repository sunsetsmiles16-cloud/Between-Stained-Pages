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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.HeirloomRepository
import com.example.data.ScannedCard
import com.example.ui.components.HeirloomTopBar
import com.example.ui.theme.*

@Composable
fun BatchReviewScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSingleReview: (String) -> Unit,
    onNavigateToScanner: () -> Unit,
    onBatchSaved: () -> Unit
) {
    val context = LocalContext.current
    val scannedCards by HeirloomRepository.scannedCards.collectAsState()
    var selectedCategory by remember { mutableStateOf("All Cards") }
    val allSelected = scannedCards.all { it.isSelected }

    val filteredCards = remember(scannedCards, selectedCategory) {
        if (selectedCategory == "All Cards") scannedCards
        else scannedCards.filter { it.category == selectedCategory }
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
                            Toast.makeText(context, "All recipes saved to your digital shelf!", Toast.LENGTH_SHORT).show()
                            onBatchSaved()
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_all_batch_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save All 28 Recipes to Digital Shelf", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToScanner,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmOnSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MustardSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan More Cards from This Box", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                .testTag("batch_review_list"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Box Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Tin Box Photo
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(14.dp))
                        ) {
                            AsyncImage(
                                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuA6KoNF5udej9Uq-wLxAW-Do7HmHgZXs6HE-wGACywBzdud2tGeytgUoGKpxvrjx-gfap4hdTanT3fnUAF95zLp0kHtZla9COzyIsaDLQMbgZn7ElOaj3loRTej_zWYLiyq3ZFBpX6CRVLfQCjOrKj1CuLjZ6ODUzxfcQyRijsChyZ-nMa62-wkE8uel46qlTRe_IXnbyTz_sx8ClA_tMGVEOqHBdiXmE7iXxPo0cjHSq7HNeh6P1XX",
                                contentDescription = "Grandma Rose's Tin Box",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = SageTertiaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(OnTertiaryContainer)
                                    )
                                    Text(
                                        text = "BATCH SCAN COMPLETE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = OnTertiaryContainer
                                    )
                                }
                            }

                            Text(
                                text = "Grandma Rose's Tin Box",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = WarmOnSurface
                            )

                            Text(
                                text = "Scanned from heirloom 1974 tin • Curated & transcribed",
                                fontSize = 12.sp,
                                color = WarmOnSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Metrics Quad Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(title = "28 Cards", subtitle = "Cards In", color = TerracottaPrimary, modifier = Modifier.weight(1f))
                    MetricBox(title = "4 Tabs", subtitle = "Dividers", color = MustardSecondary, modifier = Modifier.weight(1f))
                    MetricBox(title = "97%", subtitle = "Confidence", color = SageTertiary, modifier = Modifier.weight(1f))
                    MetricBox(title = "2 Unclear", subtitle = "Check Ink", color = Color(0xFFC04000), modifier = Modifier.weight(1f))
                }
            }

            // Auto-linked Cookbooks Banner
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CollectionsBookmark,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Auto-mapped to 3 Family Cookbooks",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmOnSurface
                            )
                        }
                        Text(
                            text = "Review Map",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaPrimary
                        )
                    }
                }
            }

            // Dividers / Tabs Filter Pills
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("All Cards", "Desserts", "Breads & Rolls", "Casseroles", "Vegetables")
                    tabs.forEach { tab ->
                        val isSelected = selectedCategory == tab
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelected) TerracottaPrimary else SurfaceContainerHigh,
                            modifier = Modifier.clickable { selectedCategory = tab }
                        ) {
                            Text(
                                text = tab,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) OnPrimary else WarmOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Batch selection control row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { HeirloomRepository.selectAllCards(it) },
                            colors = CheckboxDefaults.colors(checkedColor = TerracottaPrimary)
                        )
                        Text(
                            text = "Selected for Import (${scannedCards.count { it.isSelected }})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmOnSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = { Toast.makeText(context, "Move to Shelf...", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move", tint = WarmOnSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { Toast.makeText(context, "Adjusting image contrast...", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Tune, contentDescription = "Tune", tint = WarmOnSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Cards Feed
            items(filteredCards) { card ->
                BatchCardItem(
                    card = card,
                    onToggleSelect = { HeirloomRepository.toggleCardSelection(card.id) },
                    onCardClick = { onNavigateToSingleReview(card.id) }
                )
            }
        }
    }
}

@Composable
private fun MetricBox(title: String, subtitle: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = WarmOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun BatchCardItem(
    card: ScannedCard,
    onToggleSelect: () -> Unit,
    onCardClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("batch_card_${card.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Select Checkbox
            Checkbox(
                checked = card.isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(checkedColor = TerracottaPrimary),
                modifier = Modifier.size(20.dp)
            )

            // Card Thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceContainer)
            ) {
                AsyncImage(
                    model = card.imageUrl,
                    contentDescription = card.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Card Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerracottaPrimary,
                        letterSpacing = 0.5.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (card.confidence >= 95) TertiaryFixed else SecondaryFixed
                    ) {
                        Text(
                            text = "${card.confidence}% Acc",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (card.confidence >= 95) Color(0xFF193616) else Color(0xFF432C00),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = card.title,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = WarmOnSurface,
                    lineHeight = 18.sp
                )

                Text(
                    text = card.snippet,
                    fontSize = 12.sp,
                    color = WarmOnSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Unclear Words alert or 2-Sided indicator
                if (card.unclearWords.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MustardSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "2 Unclear: ${card.unclearWords.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = WarmOnSurfaceVariant
                            )
                        }
                        Text(
                            text = "Verify Ink",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaPrimary
                        )
                    }
                }

                if (card.isTwoSided) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = SageTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (card.isStitched) "Front & Back Stitched" else "2-Sided Linked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = SageTertiary
                        )
                    }
                }
            }
        }
    }
}
