package com.example.ui.recipes

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.model.PantryItem
import com.example.model.Recipe
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.EmeraldGreenLight
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintContainer
import com.example.ui.theme.MintSoft
import com.example.viewmodel.PantryViewModel

@Composable
fun RecipeSuggestionsScreen(
    viewModel: PantryViewModel,
    modifier: Modifier = Modifier
) {
    val recipes by viewModel.suggestedRecipes.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGeneratingRecipes.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val recentlyCooked by viewModel.recentlyCookedRecipe.collectAsStateWithLifecycle()

    val expiringItems = remember(allItems) {
        allItems.filter { it.daysUntilExpiry() <= 4 }
    }

    var selectedIngredientFilter by remember { mutableStateOf<String?>(null) }

    val displayedRecipes = remember(recipes, selectedIngredientFilter) {
        if (selectedIngredientFilter == null) {
            recipes
        } else {
            recipes.filter { recipe ->
                recipe.matchedExpiringIngredients.any { it.contains(selectedIngredientFilter!!, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 100.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("recipe_suggestions_screen")
    ) {
        // Header
        item {
            RecipeHeaderSection(
                expiringCount = expiringItems.size,
                isGenerating = isGenerating,
                onGenerateFresh = { viewModel.generateRecipes() }
            )
        }

        // Expiring Ingredients Highlight Chips
        if (expiringItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Expiring Ingredients in Focus",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedIngredientFilter == null,
                            onClick = { selectedIngredientFilter = null },
                            label = { Text("All Recipes") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldGreen,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.testTag("filter_all_recipes_chip")
                        )
                    }

                    items(expiringItems, key = { it.id }) { item ->
                        FilterChip(
                            selected = selectedIngredientFilter == item.name,
                            onClick = {
                                selectedIngredientFilter = if (selectedIngredientFilter == item.name) null else item.name
                            },
                            label = {
                                Text("${item.name} (${item.daysUntilExpiry()}d left)")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintContainer,
                                selectedLabelColor = EmeraldGreenDark
                            ),
                            modifier = Modifier.testTag("filter_ingredient_${item.id}")
                        )
                    }
                }
            }
        }

        // Recently Cooked Feedback Alert
        recentlyCooked?.let { title ->
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MintContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Great job! \"$title\" cooked and ingredients updated!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ForestGreen
                        )
                    }
                }
            }
        }

        // Recipes List
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Suggested Zero-Waste Recipes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${displayedRecipes.size} recipes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (displayedRecipes.isEmpty()) {
            item {
                RecipesEmptyView(onGenerate = { viewModel.generateRecipes() })
            }
        } else {
            items(displayedRecipes, key = { it.id }) { recipe ->
                RecipeItemCard(
                    recipe = recipe,
                    onCookThis = { viewModel.cookRecipe(recipe) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun RecipeHeaderSection(
    expiringCount: Int,
    isGenerating: Boolean,
    onGenerateFresh: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Zero-Waste Chef",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = ForestGreen
                    )
                    Text(
                        text = if (expiringCount > 0)
                            "$expiringCount ingredients expiring soon • Rescued in recipes below"
                        else
                            "Turn your pantry ingredients into delicious waste-free meals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onGenerateFresh,
                    enabled = !isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("generate_recipes_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isGenerating) "Cooking..." else "New Ideas",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecipeItemCard(
    recipe: Recipe,
    onCookThis: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var cookedState by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("recipe_card_${recipe.id}")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Waste Saved Badge & Difficulty
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MintContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = EmeraldGreenDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Saves ${String.format("%.2f", recipe.wasteSavedScoreKg)}kg food waste",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldGreenDark
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${recipe.prepTimeMinutes + recipe.cookTimeMinutes}m total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Description
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = recipe.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Rescued Ingredients Pills
            Text(
                text = "Rescued Ingredients:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ForestGreen
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                recipe.matchedExpiringIngredients.take(3).forEach { ing ->
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = EmeraldGreenDark,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ing,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldGreenDark
                            )
                        }
                    }
                }
            }

            // Expandable Instructions
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (recipe.otherIngredients.isNotEmpty()) {
                        Text(
                            text = "Other Pantry Staples Needed:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        recipe.otherIngredients.forEach { item ->
                            Text(
                                text = "• $item",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "Cooking Steps:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    recipe.instructions.forEachIndexed { idx, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                color = MintContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldGreenDark
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("expand_recipe_${recipe.id}")
                ) {
                    Text(if (expanded) "Hide Details" else "View Steps", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = EmeraldGreen
                    )
                }

                Button(
                    onClick = {
                        cookedState = true
                        onCookThis()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cookedState) ForestGreen else EmeraldGreen
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("cook_recipe_button_${recipe.id}")
                ) {
                    Icon(
                        imageVector = if (cookedState) Icons.Default.Check else Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (cookedState) "Cooked & Rescued!" else "Cook (Use Items)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun RecipesEmptyView(onGenerate: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MintContainer)
        ) {
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = null,
                tint = EmeraldGreen,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Recipes Generated Yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap below to let AI analyze your expiring items and suggest quick zero-waste recipes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
        ) {
            Text("Generate Zero-Waste Recipes")
        }
    }
}
