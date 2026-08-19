package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ExpiryStatus
import com.example.model.PantryCategory
import com.example.model.PantryItem
import com.example.ui.components.AddEditItemDialog
import com.example.ui.components.CategoryIcon
import com.example.ui.components.ExpiryStatusBadge
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.ExpiryCritical
import com.example.ui.theme.ExpiryCriticalBg
import com.example.ui.theme.ExpiryWarning
import com.example.ui.theme.ExpiryWarningBg
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.FreshSafe
import com.example.ui.theme.FreshSafeBg
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSoft
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PantryViewModel

@Composable
fun DashboardScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val expiringSoonItems = remember(allItems) {
        allItems.filter { it.daysUntilExpiry() <= 3 }
            .sortedBy { it.expiryDate }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
    ) {
        // App Hero Header with Sustainability Gradient
        item {
            DashboardHeroHeader(
                totalItems = stats.totalItems,
                expiringSoonCount = stats.expiringSoonCount,
                onScanClick = { viewModel.navigateTo(AppScreen.SCAN) },
                onAddClick = { showAddDialog = true }
            )
        }

        // Key Metric Summary Cards Row
        item {
            Spacer(modifier = Modifier.height(16.dp))
            StatSummaryRow(
                totalItems = stats.totalItems,
                expiringSoonCount = stats.expiringSoonCount,
                freshCount = stats.freshCount,
                onViewPantry = { viewModel.navigateTo(AppScreen.INVENTORY) }
            )
        }

        // Food Waste Impact & Sustainability Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            FoodWasteImpactCard(
                savedKg = stats.foodWasteReducedKg,
                moneySaved = stats.moneySaved,
                co2Saved = stats.co2SavedKg
            )
        }

        // Section: Items Nearing Expiry (Actionable Alerts)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (expiringSoonItems.isNotEmpty()) ExpiryWarning else FreshSafe)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Expiring Soon",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (expiringSoonItems.isNotEmpty()) {
                    Text(
                        text = "See All (${expiringSoonItems.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = EmeraldGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { viewModel.navigateTo(AppScreen.INVENTORY) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (expiringSoonItems.isEmpty()) {
                AllGoodCard(onAddItems = { showAddDialog = true })
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(expiringSoonItems, key = { it.id }) { item ->
                        ExpiringItemHeroCard(
                            item = item,
                            onCookRecipe = { viewModel.navigateTo(AppScreen.RECIPES) },
                            onMarkConsumed = { viewModel.markConsumed(item) }
                        )
                    }
                }
            }
        }

        // Section: Zero Waste Recipe Generator CTA
        item {
            Spacer(modifier = Modifier.height(20.dp))
            RecipeSuggestionBanner(
                expiringCount = expiringSoonItems.size,
                onExploreRecipes = { viewModel.navigateTo(AppScreen.RECIPES) }
            )
        }

        // Section: Category Breakdown quick access
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Pantry Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            CategoryDistributionGrid(
                items = allItems,
                onCategoryClick = { category ->
                    viewModel.selectCategory(category)
                    viewModel.navigateTo(AppScreen.INVENTORY)
                }
            )
        }
    }

    if (showAddDialog) {
        AddEditItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, qty, unit, category, shelfLife, storage, price, notes ->
                viewModel.addItem(name, qty, unit, category, shelfLife, storage, price, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun DashboardHeroHeader(
    totalItems: Int,
    expiringSoonCount: Int,
    onScanClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(ForestGreen, EmeraldGreenDark, EmeraldGreen)
                )
            )
            .padding(top = 28.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = EmeraldGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SMART PANTRY",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFA5D6A7)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Food Waste Reducer",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alerts",
                            tint = if (expiringSoonCount > 0) Color(0xFFFFD54F) else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Quick-Action 'Scan Receipt' Hero Button
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onScanClick() }
                    .testTag("hero_scan_receipt_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MintContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Scan Receipt",
                                tint = EmeraldGreenDark,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Scan Grocery Receipt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryLight
                            )
                            Text(
                                text = "Auto-detect items & expiry dates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = EmeraldGreen,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private val TextPrimaryLight = Color(0xFF191C1A)

@Composable
fun StatSummaryRow(
    totalItems: Int,
    expiringSoonCount: Int,
    freshCount: Int,
    onViewPantry: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        StatPill(
            label = "Total Items",
            value = totalItems.toString(),
            color = EmeraldGreen,
            bgColor = MintContainer,
            modifier = Modifier.weight(1f).clickable { onViewPantry() }
        )
        StatPill(
            label = "Expiring Soon",
            value = expiringSoonCount.toString(),
            color = ExpiryWarning,
            bgColor = ExpiryWarningBg,
            modifier = Modifier.weight(1f).clickable { onViewPantry() }
        )
        StatPill(
            label = "Fresh & Safe",
            value = freshCount.toString(),
            color = FreshSafe,
            bgColor = FreshSafeBg,
            modifier = Modifier.weight(1f).clickable { onViewPantry() }
        )
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
        ) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun FoodWasteImpactCard(
    savedKg: Double,
    moneySaved: Double,
    co2Saved: Double
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sustainability Impact",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    color = MintContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Eco Tier: Green Star",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldGreenDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                ImpactMetric(label = "Food Rescued", value = "${String.format("%.1f", savedKg)} kg")
                ImpactMetric(label = "Money Saved", value = "$${String.format("%.2f", moneySaved)}")
                ImpactMetric(label = "CO2 Avoided", value = "${String.format("%.1f", co2Saved)} kg")
            }

            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { 0.78f },
                color = EmeraldGreen,
                trackColor = MintContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "78% of your pantry items consumed before expiration this month",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImpactMetric(label: String, value: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = ForestGreen
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ExpiringItemHeroCard(
    item: PantryItem,
    onCookRecipe: () -> Unit,
    onMarkConsumed: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .width(220.dp)
            .testTag("expiring_item_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MintContainer)
                ) {
                    CategoryIcon(category = item.getCategoryEnum(), modifier = Modifier.size(20.dp))
                }

                ExpiryStatusBadge(item = item)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit} • ${item.storageLocation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onCookRecipe,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Recipe", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onMarkConsumed,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Done",
                        tint = EmeraldGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AllGoodCard(onAddItems: () -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FreshSafeBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = FreshSafe,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No Items Urgently Expiring!",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreenDark
                )
                Text(
                    text = "Your food waste risk is currently very low. Keep it up!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RecipeSuggestionBanner(
    expiringCount: Int,
    onExploreRecipes: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF8E1)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable { onExploreRecipes() }
            .testTag("recipe_suggestion_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFECB3))
                ) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = Color(0xFFE65100),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Smart Zero-Waste Chef",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E342E)
                    )
                    Text(
                        text = if (expiringCount > 0)
                            "$expiringCount ingredients expiring soon • Tap to cook"
                        else
                            "Generate recipes from your active pantry",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6D4C41)
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Explore",
                tint = Color(0xFFE65100),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CategoryDistributionGrid(
    items: List<PantryItem>,
    onCategoryClick: (PantryCategory) -> Unit
) {
    val categories = PantryCategory.entries
    val countsByCategory = remember(items) {
        items.groupBy { it.getCategoryEnum() }.mapValues { it.value.size }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        val rows = categories.chunked(2)
        rows.forEach { rowCategories ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                rowCategories.forEach { cat ->
                    val count = countsByCategory[cat] ?: 0
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp),
                        tonalElevation = 1.dp,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onCategoryClick(cat) }
                            .testTag("dashboard_cat_${cat.name}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MintContainer)
                            ) {
                                CategoryIcon(category = cat, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = cat.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$count items",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
