package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.inventory.InventoryScreen
import com.example.ui.recipes.RecipeSuggestionsScreen
import com.example.ui.scan.ScanReceiptScreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldGreenDark
import com.example.ui.theme.ExpiryWarning
import com.example.ui.theme.MintContainer
import com.example.ui.theme.SmartPantryTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.PantryViewModel

sealed class NavigationTab(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    data object Dashboard : NavigationTab(
        AppScreen.DASHBOARD,
        "Dashboard",
        Icons.Filled.Dashboard,
        Icons.Outlined.Dashboard,
        "nav_dashboard"
    )

    data object Inventory : NavigationTab(
        AppScreen.INVENTORY,
        "Pantry",
        Icons.Filled.Kitchen,
        Icons.Outlined.Kitchen,
        "nav_inventory"
    )

    data object Scan : NavigationTab(
        AppScreen.SCAN,
        "Scan",
        Icons.Filled.CameraAlt,
        Icons.Outlined.CameraAlt,
        "nav_scan"
    )

    data object Recipes : NavigationTab(
        AppScreen.RECIPES,
        "Recipes",
        Icons.Filled.RestaurantMenu,
        Icons.Outlined.RestaurantMenu,
        "nav_recipes"
    )
}

class MainActivity : ComponentActivity() {

    private val viewModel: PantryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartPantryTheme {
                SmartPantryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SmartPantryApp(viewModel: PantryViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.userFeedbackMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedbackMessage()
        }
    }

    val tabs = listOf(
        NavigationTab.Dashboard,
        NavigationTab.Inventory,
        NavigationTab.Scan,
        NavigationTab.Recipes
    )

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("main_bottom_nav")
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentScreen == tab.screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(tab.screen) },
                        icon = {
                            if (tab == NavigationTab.Dashboard && stats.expiringSoonCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = ExpiryWarning,
                                            contentColor = MaterialTheme.colorScheme.onSurface
                                        ) {
                                            Text("${stats.expiringSoonCount}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = tab.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldGreenDark,
                            selectedTextColor = EmeraldGreenDark,
                            indicatorColor = MintContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Crossfade(
            targetState = currentScreen,
            label = "screen_transition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { screen ->
            when (screen) {
                AppScreen.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                AppScreen.INVENTORY -> InventoryScreen(viewModel = viewModel)
                AppScreen.SCAN -> ScanReceiptScreen(viewModel = viewModel)
                AppScreen.RECIPES -> RecipeSuggestionsScreen(viewModel = viewModel)
            }
        }
    }
}

