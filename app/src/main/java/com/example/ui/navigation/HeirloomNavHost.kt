package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.components.Screen
import com.example.ui.screens.BatchReviewScreen
import com.example.ui.screens.CardScannerScreen
import com.example.ui.screens.CloudSyncScreen
import com.example.ui.screens.CookbooksScreen
import com.example.ui.screens.ImportScreen
import com.example.ui.screens.NewCookbookScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.OrganizeCookbookScreen
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RecipeDetailScreen
import com.example.ui.screens.RecipesScreen
import com.example.ui.screens.SingleCardReviewScreen

/**
 * Extension function on NavController to navigate between primary top-level tabs
 * (Recipe list, Pantry view, Scanner camera, Cookbooks, Grocery) seamlessly,
 * saving and restoring backstack state without duplicate instances.
 */
fun NavController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Central NavigationHost using Jetpack Compose Navigation.
 * Orchestrates seamless switching between all primary tabs, detail views,
 * and dedicated sub-screens (Profile, Notifications, Cloud Sync, Binder Designer, etc.)
 */
@Composable
fun HeirloomNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Recipes.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(animationSpec = tween(220)) },
        exitTransition = { fadeOut(animationSpec = tween(220)) },
        popEnterTransition = { fadeIn(animationSpec = tween(220)) },
        popExitTransition = { fadeOut(animationSpec = tween(220)) }
    ) {
        // 0. First-Time Profile Creation Onboarding Screen
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onOnboardingCompleted = {
                    navController.navigate(Screen.Recipes.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        // 1. Primary Destination: Recipe List
        composable(Screen.Recipes.route) {
            RecipesScreen(
                onNavigateToDetail = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onNavigateToImport = {
                    navController.navigateToTab(Screen.Import.route)
                },
                onNavigateToScanner = {
                    navController.navigateToTab(Screen.CardScanner.route)
                },
                onNavigateToCookbooks = {
                    navController.navigateToTab(Screen.Cookbooks.route)
                },
                onNavigateToPantry = {
                    navController.navigateToTab(Screen.Pantry.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToCloudSync = {
                    navController.navigate(Screen.CloudSync.route)
                }
            )
        }

        // 2. Primary Destination: Pantry View
        composable(Screen.Pantry.route) {
            PantryScreen(
                initialTab = "pantry",
                onNavigateToRecipes = {
                    navController.navigateToTab(Screen.Recipes.route)
                },
                onNavigateToScanner = {
                    navController.navigateToTab(Screen.CardScanner.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        // 3. Primary Destination: Scanner Camera Interface
        composable(Screen.CardScanner.route) {
            CardScannerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSingleReview = {
                    navController.navigate(Screen.SingleCardReview.route)
                },
                onNavigateToBatchReview = {
                    navController.navigate(Screen.BatchReview.route)
                },
                onNavigateToRecipes = {
                    navController.navigateToTab(Screen.Recipes.route)
                },
                onNavigateToPantry = {
                    navController.navigateToTab(Screen.Pantry.route)
                }
            )
        }

        // Grocery View (Pantry Grocery sub-tab)
        composable(Screen.Grocery.route) {
            PantryScreen(
                initialTab = "grocery",
                onNavigateToRecipes = {
                    navController.navigateToTab(Screen.Recipes.route)
                },
                onNavigateToScanner = {
                    navController.navigateToTab(Screen.CardScanner.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        // Cookbooks Shelf
        composable(Screen.Cookbooks.route) {
            CookbooksScreen(
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onNavigateToNewCookbook = {
                    navController.navigate(Screen.NewCookbook.route)
                },
                onNavigateToOrganizeCookbook = { cookbookId ->
                    navController.navigate(Screen.OrganizeCookbook.createRoute(cookbookId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        // Video & Reel Link Ingestion
        composable(Screen.Import.route) {
            ImportScreen(
                onNavigateToScanner = {
                    navController.navigateToTab(Screen.CardScanner.route)
                },
                onRecipeSaved = {
                    navController.navigateToTab(Screen.Recipes.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                }
            )
        }

        // Dedicated Sub-Screen: Profile & Kitchen Settings
        composable(
            route = Screen.Profile.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCloudSync = {
                    navController.navigate(Screen.CloudSync.route)
                },
                onResetToFirstRun = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Dedicated Sub-Screen: Notifications & Kitchen Alerts
        composable(
            route = Screen.Notifications.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) {
            NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToRecipeDetail = { recipeId ->
                    navController.navigate(Screen.RecipeDetail.createRoute(recipeId))
                },
                onNavigateToPantry = {
                    navController.navigateToTab(Screen.Pantry.route)
                }
            )
        }

        // Dedicated Sub-Screen: Cloud Sync Hub
        composable(
            route = Screen.CloudSync.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) {
            CloudSyncScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Dedicated Sub-Screen: Design New Binder
        composable(
            route = Screen.NewCookbook.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) {
            NewCookbookScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Dedicated Sub-Screen: Organize Binder Recipes
        composable(
            route = Screen.OrganizeCookbook.route,
            arguments = listOf(navArgument("cookbookId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) { backStackEntry ->
            val cookbookId = backStackEntry.arguments?.getString("cookbookId") ?: "1"
            OrganizeCookbookScreen(
                cookbookId = cookbookId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // OCR Batch Review Destination
        composable(
            route = Screen.BatchReview.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) {
            BatchReviewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSingleReview = { _ ->
                    navController.navigate(Screen.SingleCardReview.route)
                },
                onNavigateToScanner = {
                    navController.navigateToTab(Screen.CardScanner.route)
                },
                onBatchSaved = {
                    navController.navigateToTab(Screen.Recipes.route)
                }
            )
        }

        // Single Card OCR & Marginalia Inspection
        composable(
            route = Screen.SingleCardReview.route,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) {
            SingleCardReviewScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.navigateToTab(Screen.Recipes.route)
                },
                onScanBackOfCard = {
                    navController.navigateToTab(Screen.CardScanner.route)
                }
            )
        }

        // Recipe Guided Cooking & Interactive Detail
        composable(
            route = Screen.RecipeDetail.route,
            arguments = listOf(navArgument("recipeId") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(260)) + fadeIn(tween(260))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(260)) + fadeOut(tween(260))
            }
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: "rec-chicken"
            RecipeDetailScreen(
                recipeId = recipeId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

