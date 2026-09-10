package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.HeirloomRepository
import com.example.ui.components.HeirloomBottomBar
import com.example.ui.components.Screen
import com.example.ui.navigation.HeirloomNavHost
import com.example.ui.navigation.navigateToTab
import com.example.ui.theme.HeirloomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        HeirloomRepository.initPrefs(applicationContext)
        enableEdgeToEdge()
        setContent {
            val userProfile by HeirloomRepository.userProfile.collectAsState()
            HeirloomTheme(
                palette = userProfile.themePalette,
                themeMode = userProfile.themeMode
            ) {
                HeirloomApp()
            }
        }
    }
}

@Composable
fun HeirloomApp() {
    val navController = rememberNavController()
    val currentUser by com.example.data.cloud.HeirloomCloudAuthService.currentUser.collectAsState()
    val startDestination = remember(currentUser) {
        if (currentUser == null) {
            Screen.Auth.route
        } else {
            Screen.Recipes.route
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: startDestination
    val groceryItems by HeirloomRepository.groceryItems.collectAsState()
    val pendingGroceryCount = groceryItems.count { !it.isChecked }

    // Primary top-level destinations where the Heirloom navigation bar is active
    val showBottomBar = currentRoute in listOf(
        Screen.Recipes.route,
        Screen.Pantry.route,
        Screen.CardScanner.route,
        Screen.Cookbooks.route,
        Screen.Grocery.route,
        Screen.Import.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                HeirloomBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigateToTab(route)
                    },
                    groceryCount = pendingGroceryCount
                )
            }
        }
    ) { innerPadding ->
        HeirloomNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}


