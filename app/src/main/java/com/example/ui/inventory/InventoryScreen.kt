package com.example.ui.inventory

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PantryCategory
import com.example.model.PantryItem
import com.example.model.StorageLocation
import com.example.ui.components.AddEditItemDialog
import com.example.ui.components.CategoryIcon
import com.example.ui.components.PantryItemCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSoft
import com.example.viewmodel.PantryViewModel

@Composable
fun InventoryScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedStorage by viewModel.selectedStorage.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PantryItem?>(null) }
    var itemToDelete by remember { mutableStateOf<PantryItem?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("inventory_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Pantry Inventory",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${allItems.size} total items tracked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MintContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "${items.size} showing",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldGreenDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search pantry items...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = EmeraldGreen
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_search_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Storage Location Filter Chips (All, Fridge, Pantry, Freezer)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = selectedStorage == null,
                            onClick = { viewModel.selectStorage(null) },
                            label = { Text("All Places") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintContainer,
                                selectedLabelColor = EmeraldGreenDark
                            ),
                            modifier = Modifier.testTag("storage_all_chip")
                        )

                        StorageLocation.entries.forEach { loc ->
                            FilterChip(
                                selected = selectedStorage == loc.name,
                                onClick = { viewModel.selectStorage(loc.name) },
                                label = { Text(loc.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MintContainer,
                                    selectedLabelColor = EmeraldGreenDark
                                ),
                                modifier = Modifier.testTag("storage_${loc.name}_chip")
                            )
                        }
                    }
                }
            }

            // Category Tabs Row
            ScrollableTabRow(
                selectedTabIndex = if (selectedCategory == null) 0 else PantryCategory.entries.indexOf(selectedCategory) + 1,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldGreen,
                indicator = { tabPositions ->
                    val index = if (selectedCategory == null) 0 else PantryCategory.entries.indexOf(selectedCategory) + 1
                    if (index < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                            color = EmeraldGreen,
                            height = 3.dp
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedCategory == null,
                    onClick = { viewModel.selectCategory(null) },
                    text = {
                        Text(
                            text = "All Items (${allItems.size})",
                            fontWeight = if (selectedCategory == null) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("tab_category_all")
                )

                PantryCategory.entries.forEach { cat ->
                    val catCount = allItems.count { it.getCategoryEnum() == cat }
                    Tab(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.selectCategory(cat) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryIcon(
                                    category = cat,
                                    tint = if (selectedCategory == cat) EmeraldGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${cat.displayName} ($catCount)",
                                    fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        modifier = Modifier.testTag("tab_category_${cat.name}")
                    )
                }
            }

            // Items List
            if (items.isEmpty()) {
                InventoryEmptyView(
                    searchQuery = searchQuery,
                    hasFilter = selectedCategory != null || selectedStorage != null,
                    onClearFilters = {
                        viewModel.setSearchQuery("")
                        viewModel.selectCategory(null)
                        viewModel.selectStorage(null)
                    },
                    onAddItem = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items, key = { it.id }) { item ->
                        PantryItemCard(
                            item = item,
                            onMarkConsumed = { viewModel.markConsumed(item) },
                            onQuantityChange = { delta -> viewModel.updateQuantity(item, delta) },
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add New Pantry Item
        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = EmeraldGreen,
            contentColor = Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Add Item", fontWeight = FontWeight.Bold) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 88.dp, end = 20.dp)
                .testTag("add_item_fab")
        )
    }

    // Add dialog
    if (showAddDialog) {
        AddEditItemDialog(
            initialItem = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, qty, unit, category, shelfLife, storage, price, notes ->
                viewModel.addItem(name, qty, unit, category, shelfLife, storage, price, notes)
                showAddDialog = false
            }
        )
    }

    // Edit dialog
    itemToEdit?.let { item ->
        AddEditItemDialog(
            initialItem = item,
            onDismiss = { itemToEdit = null },
            onSave = { name, qty, unit, category, shelfLife, storage, price, notes ->
                val now = System.currentTimeMillis()
                val updatedExpiry = now + (shelfLife.toLong() * 24 * 60 * 60 * 1000)
                viewModel.updateItem(
                    item.copy(
                        name = name,
                        quantity = qty,
                        unit = unit,
                        category = category.name,
                        expiryDate = updatedExpiry,
                        storageLocation = storage,
                        estimatedPrice = price,
                        notes = notes
                    )
                )
                itemToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item?") },
            text = { Text("Are you sure you want to remove \"${item.name}\" from your pantry?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteItem(item)
                        itemToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun InventoryEmptyView(
    searchQuery: String,
    hasFilter: Boolean,
    onClearFilters: () -> Unit,
    onAddItem: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MintContainer)
        ) {
            Icon(
                imageVector = Icons.Default.Kitchen,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (searchQuery.isNotEmpty() || hasFilter) "No matching pantry items" else "Your Pantry is Empty",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (searchQuery.isNotEmpty() || hasFilter)
                "Try clearing your search query or filters to see all items."
            else
                "Scan a grocery receipt or tap 'Add Item' to start tracking your food freshness and reduce waste.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (hasFilter || searchQuery.isNotEmpty()) {
            TextButton(onClick = onClearFilters) {
                Text("Clear Filters & Search", color = EmeraldGreen, fontWeight = FontWeight.Bold)
            }
        } else {
            androidx.compose.material3.Button(
                onClick = onAddItem,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
            ) {
                Text("Add First Item")
            }
        }
    }
}
