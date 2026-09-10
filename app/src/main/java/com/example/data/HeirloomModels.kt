package com.example.data

data class Recipe(
    val id: String,
    val userId: String = "",
    val title: String,
    val source: String,
    val sourceType: RecipeSourceType,
    val description: String,
    val imageUrl: String,
    val prepTime: String,
    val cookTime: String,
    val servings: Int,
    val difficulty: String = "Easy",
    val binderCategory: String,
    val pantryStatusText: String = "All ingredients in pantry",
    val allIngredientsInPantry: Boolean = true,
    val ingredients: List<IngredientItem>,
    val steps: List<CookingStep>,
    val secretNote: String? = null,
    val audioNoteTitle: String? = null,
    val audioNoteDuration: String? = null,
    val bookmarked: Boolean = false,
    val isFavorite: Boolean = false
)

enum class RecipeSourceType {
    TIKTOK, REEL, VINTAGE_SCAN, INSTAGRAM, WEB, FAMILY_NOTE
}

data class IngredientItem(
    val id: String,
    val name: String,
    val note: String = "",
    val category: String = "Pantry",
    val inPantry: Boolean = true,
    val isMarkedOut: Boolean = false,
    val confidence: Int = 98,
    val isUnclear: Boolean = false
)

data class CookingStep(
    val stepNumber: Int,
    val title: String,
    val instruction: String,
    val durationMinutes: Int? = null,
    val timerLabel: String? = null,
    val confidence: Int = 98
)

data class Cookbook(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val recipeCount: Int,
    val lastUpdated: String,
    val badge: String = "Active",
    val fabricColor: String = "terracotta", // terracotta, avocado, mustard, oatmeal
    val foilStamp: String = "pot", // pot, wheat, pin, heart
    val typography: String = "groovy", // groovy, classic, script
    val coverImageUrl: String? = null,
    val recipes: List<String> = emptyList()
)

enum class PantryItemStatus {
    IN, LOW, OUT
}

data class PantryItem(
    val id: String,
    val name: String,
    val note: String,
    val category: String, // Dairy & Cold Shelf, Baking & Pantry Grains, Produce & Fresh Herbs
    val status: PantryItemStatus = PantryItemStatus.IN,
    val addedToGroceryToday: Boolean = false
)

data class GroceryItem(
    val id: String,
    val name: String,
    val note: String,
    val tag: String = "Auto-Pantry", // Auto-Pantry, Recipe Link, Manual
    val isChecked: Boolean = false
)

data class ScannedCard(
    val id: String,
    val title: String,
    val snippet: String,
    val imageUrl: String,
    val category: String,
    val confidence: Int,
    val isTwoSided: Boolean = false,
    val isStitched: Boolean = false,
    val unclearWords: List<String> = emptyList(),
    val familyLore: String? = null,
    val audioNote: Boolean = false,
    val isSelected: Boolean = true
)

data class NotificationItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val relatedRecipeId: String? = null
)

enum class NotificationType {
    FAMILY_NOTE, PANTRY_ALERT, SCAN_COMPLETE, CLOUD_SYNC
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class AppThemePalette(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val tertiaryColorHex: Long,
    val surfaceColorHex: Long,
    val isDarkDefault: Boolean = false
) {
    HEARTH_TERRACOTTA(
        id = "terracotta",
        title = "Hearth Terracotta",
        subtitle = "Warm Clay & Golden Mustard",
        description = "Sunbaked stoneware, paprika, and warm vintage kitchen spices",
        primaryColorHex = 0xFF9C3E1F,
        secondaryColorHex = 0xFF7F5700,
        tertiaryColorHex = 0xFF466341,
        surfaceColorHex = 0xFFFFF8F5
    ),
    AVOCADO_OLIVE(
        id = "avocado",
        title = "Avocado Velvet",
        subtitle = "70s Olive & Toasted Brass",
        description = "Mid-century enamel, kitchen garden herbs, and burnished honey brass",
        primaryColorHex = 0xFF3B5C26,
        secondaryColorHex = 0xFF8A6200,
        tertiaryColorHex = 0xFF874620,
        surfaceColorHex = 0xFFF9FAF4
    ),
    NAVY_COPPER(
        id = "navy",
        title = "Manor Indigo",
        subtitle = "Navy Leather & Burnished Copper",
        description = "Antique family recipe archives, rich indigo cloth, and gleaming copper pans",
        primaryColorHex = 0xFF1C3D5A,
        secondaryColorHex = 0xFF9F451B,
        tertiaryColorHex = 0xFF2F5444,
        surfaceColorHex = 0xFFF6F8FB
    ),
    FRENCH_LAVENDER(
        id = "lavender",
        title = "Provencal Lavender",
        subtitle = "Wild Thyme & Warm Ochre",
        description = "Sunlit French countryside, dried culinary herbs, and soft floral linen",
        primaryColorHex = 0xFF5E3F6D,
        secondaryColorHex = 0xFF885E15,
        tertiaryColorHex = 0xFF3D6148,
        surfaceColorHex = 0xFFFBF7FC
    ),
    ROSEMARY_SAGE(
        id = "sage",
        title = "Botanical Sage",
        subtitle = "Rosemary & Celadon Linen",
        description = "Fresh botanical kitchen herb gardens, olive wood, and morning dew",
        primaryColorHex = 0xFF2A5938,
        secondaryColorHex = 0xFF886300,
        tertiaryColorHex = 0xFF7A3E50,
        surfaceColorHex = 0xFFF5F9F6
    ),
    MIDNIGHT_HEARTH(
        id = "midnight",
        title = "Midnight Hearth",
        subtitle = "Cast Iron & Glowing Embers",
        description = "Woodfire embers, blackened cast iron skillets, and cozy evening cooking",
        primaryColorHex = 0xFFE26E54,
        secondaryColorHex = 0xFFE5A93C,
        tertiaryColorHex = 0xFF82AA85,
        surfaceColorHex = 0xFF1A1614,
        isDarkDefault = true
    );

    companion object {
        fun fromId(id: String): AppThemePalette {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: HEARTH_TERRACOTTA
        }
    }
}

data class UserProfile(
    val userId: String = "",
    val name: String = "",
    val title: String = "Home Cook & Curator",
    val bio: String = "Baking memories from scratch with well-loved recipes.",
    val email: String = "",
    val avatarType: String = "wooden_spoon",
    val avatarUrl: String = "",
    val memberPlan: String = "Family Legacy Plan",
    val memberCount: Int = 1,
    val measurementUnits: String = "US Customary (Cups, Tbsp)",
    val defaultServings: Int = 4,
    val dietaryPreferences: List<String> = emptyList(),
    val keepAwakeOnCookMode: Boolean = true,
    val keepScreenAwakeByDefault: Boolean = true,
    val compactFeedView: Boolean = false,
    val autoSyncPantryToGrocery: Boolean = true,
    val hapticFeedback: Boolean = true,
    val themePalette: AppThemePalette = AppThemePalette.HEARTH_TERRACOTTA,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val isOnboardingCompleted: Boolean = false
)

data class DeviceSyncItem(
    val id: String,
    val deviceName: String,
    val deviceType: String,
    val lastSyncTime: String,
    val isCurrentDevice: Boolean = false
)

typealias LocalRecipe = com.example.data.local.Recipe
typealias RecipeEntity = com.example.data.local.Recipe
typealias RecipeDao = com.example.data.local.RecipeDao
typealias AppDatabase = com.example.data.local.AppDatabase
typealias RecipeDatabase = com.example.data.local.AppDatabase
