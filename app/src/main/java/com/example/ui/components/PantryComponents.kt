package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExpiryStatus
import com.example.model.PantryCategory
import com.example.model.PantryItem
import com.example.model.StorageLocation
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.ExpiryCritical
import com.example.ui.theme.ExpiryCriticalBg
import com.example.ui.theme.ExpiryWarning
import com.example.ui.theme.ExpiryWarningBg
import com.example.ui.theme.FreshSafe
import com.example.ui.theme.FreshSafeBg
import com.example.ui.theme.MintContainer

@Composable
fun CategoryIcon(
    category: PantryCategory,
    modifier: Modifier = Modifier,
    tint: Color = EmeraldGreen
) {
    val icon: ImageVector = when (category) {
        PantryCategory.PRODUCE -> Icons.Default.Eco
        PantryCategory.DAIRY -> Icons.Default.Egg
        PantryCategory.PANTRY -> Icons.Default.Kitchen
        PantryCategory.MEAT_SEAFOOD -> Icons.Default.SetMeal
        PantryCategory.BAKERY -> Icons.Default.BakeryDining
        PantryCategory.BEVERAGES -> Icons.Default.LocalCafe
        PantryCategory.FROZEN -> Icons.Default.AcUnit
        PantryCategory.SNACKS_CONDIMENTS -> Icons.Default.Fastfood
    }

    Icon(
        imageVector = icon,
        contentDescription = category.displayName,
        tint = tint,
        modifier = modifier
    )
}

@Composable
fun ExpiryStatusBadge(
    item: PantryItem,
    modifier: Modifier = Modifier
) {
    val days = item.daysUntilExpiry()
    val status = item.getExpiryStatus()

    val (bgColor, textColor, text) = when (status) {
        ExpiryStatus.EXPIRED -> {
            Triple(ExpiryCriticalBg, ExpiryCritical, "Expired ${-days}d ago")
        }
        ExpiryStatus.EXPIRING_TODAY -> {
            Triple(ExpiryCriticalBg, ExpiryCritical, "Expires Today!")
        }
        ExpiryStatus.EXPIRING_SOON -> {
            Triple(
                ExpiryWarningBg,
                ExpiryWarning,
                if (days == 1) "Expires Tomorrow" else "Expires in $days days"
            )
        }
        ExpiryStatus.FRESH -> {
            Triple(FreshSafeBg, FreshSafe, "$days days left")
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = when (status) {
                    ExpiryStatus.EXPIRED, ExpiryStatus.EXPIRING_TODAY -> Icons.Default.Warning
                    ExpiryStatus.EXPIRING_SOON -> Icons.Default.CalendarToday
                    ExpiryStatus.FRESH -> Icons.Default.CheckCircle
                },
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PantryItemCard(
    item: PantryItem,
    onMarkConsumed: () -> Unit,
    onQuantityChange: (Double) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pantry_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MintContainer)
                    ) {
                        CategoryIcon(category = item.getCategoryEnum(), modifier = Modifier.size(24.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.getCategoryEnum().displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " • ${item.storageLocation}",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldGreenDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.testTag("item_options_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark as Consumed") },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen) },
                            onClick = {
                                showMenu = false
                                onMarkConsumed()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Item") },
                            leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata row: Expiry + Quantity Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ExpiryStatusBadge(item = item)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Exp: ${item.formattedExpiryDate()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { onQuantityChange(-1.0) },
                        modifier = Modifier.size(32.dp).testTag("qty_minus_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = "Decrease",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()} ${item.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = { onQuantityChange(1.0) },
                        modifier = Modifier.size(32.dp).testTag("qty_plus_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Increase",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${item.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemDialog(
    initialItem: PantryItem? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        quantity: Double,
        unit: String,
        category: PantryCategory,
        shelfLifeDays: Int,
        storageLocation: String,
        price: Double,
        notes: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var quantityStr by remember { mutableStateOf(initialItem?.quantity?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "1") }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "pcs") }
    var selectedCategory by remember { mutableStateOf(initialItem?.getCategoryEnum() ?: PantryCategory.PRODUCE) }
    var shelfLifeDays by remember { mutableIntStateOf(initialItem?.daysUntilExpiry()?.coerceAtLeast(1) ?: selectedCategory.defaultShelfLifeDays) }
    var selectedStorage by remember { mutableStateOf(initialItem?.storageLocation ?: StorageLocation.FRIDGE.name) }
    var priceStr by remember { mutableStateOf(initialItem?.estimatedPrice?.toString() ?: "2.99") }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialItem == null) "Add Pantry Item" else "Edit Pantry Item",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isError = false
                    },
                    label = { Text("Item Name *") },
                    placeholder = { Text("e.g., Greek Yogurt, Avocados") },
                    isError = isError && name.isBlank(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_item_name")
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = quantityStr,
                        onValueChange = { quantityStr = it },
                        label = { Text("Qty") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_item_quantity")
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("pcs, carton, g") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_item_unit")
                    )
                }

                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Category selection chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf(
                        PantryCategory.PRODUCE,
                        PantryCategory.DAIRY,
                        PantryCategory.PANTRY,
                        PantryCategory.MEAT_SEAFOOD
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                shelfLifeDays = cat.defaultShelfLifeDays
                            },
                            label = { Text(cat.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintContainer,
                                selectedLabelColor = EmeraldGreenDark
                            ),
                            modifier = Modifier.testTag("category_chip_${cat.name}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val secondaryCategories = listOf(
                        PantryCategory.BAKERY,
                        PantryCategory.BEVERAGES,
                        PantryCategory.FROZEN,
                        PantryCategory.SNACKS_CONDIMENTS
                    )
                    secondaryCategories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                shelfLifeDays = cat.defaultShelfLifeDays
                            },
                            label = { Text(cat.displayName, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintContainer,
                                selectedLabelColor = EmeraldGreenDark
                            )
                        )
                    }
                }

                // Storage location
                Text(
                    text = "Storage Location",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StorageLocation.entries.forEach { loc ->
                        FilterChip(
                            selected = selectedStorage == loc.name,
                            onClick = { selectedStorage = loc.name },
                            label = { Text(loc.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintContainer,
                                selectedLabelColor = EmeraldGreenDark
                            )
                        )
                    }
                }

                // Shelf life days / estimated expiry
                OutlinedTextField(
                    value = shelfLifeDays.toString(),
                    onValueChange = { shelfLifeDays = it.toIntOrNull() ?: 7 },
                    label = { Text("Estimated Shelf Life (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_shelf_life")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("e.g., opened, sealed, store on top shelf") },
                    singleLine = false,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val qty = quantityStr.toDoubleOrNull() ?: 1.0
                    val price = priceStr.toDoubleOrNull() ?: 2.99
                    onSave(
                        name,
                        qty,
                        unit,
                        selectedCategory,
                        shelfLifeDays,
                        selectedStorage,
                        price,
                        notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
