package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

object HeirloomRepository {

    private val _cookbooks = MutableStateFlow<List<Cookbook>>(emptyList())
    val cookbooks: StateFlow<List<Cookbook>> = _cookbooks.asStateFlow()

    private val _recipes = MutableStateFlow<List<Recipe>>(emptyList())
    val recipes: StateFlow<List<Recipe>> = _recipes.asStateFlow()

    private val _pantryItems = MutableStateFlow<List<PantryItem>>(emptyList())
    val pantryItems: StateFlow<List<PantryItem>> = _pantryItems.asStateFlow()

    private val _groceryItems = MutableStateFlow<List<GroceryItem>>(emptyList())
    val groceryItems: StateFlow<List<GroceryItem>> = _groceryItems.asStateFlow()

    private val _scannedCards = MutableStateFlow<List<ScannedCard>>(emptyList())
    val scannedCards: StateFlow<List<ScannedCard>> = _scannedCards.asStateFlow()

    private val _hasSeenWelcomeModal = MutableStateFlow(false)
    val hasSeenWelcomeModal: StateFlow<Boolean> = _hasSeenWelcomeModal.asStateFlow()

    fun initPrefs(context: Context) {
        try {
            com.example.data.cloud.HeirloomCloudAuthService.init(context)

            val prefs = context.getSharedPreferences("heirloom_prefs", Context.MODE_PRIVATE)
            _hasSeenWelcomeModal.value = prefs.getBoolean("has_seen_welcome_modal", false)
            
            val isOnboarded = prefs.getBoolean("user_onboarded", false)
            val name = prefs.getString("user_name", "") ?: ""
            if (isOnboarded || name.isNotBlank()) {
                val title = prefs.getString("user_title", "Home Cook & Curator") ?: "Home Cook & Curator"
                val bio = prefs.getString("user_bio", "Baking memories from scratch with well-loved recipes.") ?: "Baking memories from scratch with well-loved recipes."
                val email = prefs.getString("user_email", "") ?: ""
                val defaultServings = prefs.getInt("user_default_servings", 4)
                val dietaryStringSet = prefs.getStringSet("user_dietary", emptySet()) ?: emptySet()
                val avatarType = prefs.getString("user_avatar_type", "wooden_spoon") ?: "wooden_spoon"
                val avatarUrl = prefs.getString("user_avatar_url", "") ?: ""
                val paletteId = prefs.getString("user_theme_palette", "terracotta") ?: "terracotta"
                val matchedPalette = AppThemePalette.values().firstOrNull { it.id == paletteId } ?: AppThemePalette.HEARTH_TERRACOTTA

                val currentUserId = com.example.data.cloud.HeirloomCloudAuthService.currentUserId

                _userProfile.value = UserProfile(
                    userId = currentUserId,
                    name = name,
                    title = title,
                    bio = bio,
                    email = email,
                    avatarType = avatarType,
                    avatarUrl = avatarUrl,
                    defaultServings = defaultServings,
                    dietaryPreferences = dietaryStringSet.toList(),
                    themePalette = matchedPalette,
                    isOnboardingCompleted = isOnboarded
                )
            } else {
                _userProfile.value = UserProfile(name = "", isOnboardingCompleted = false)
            }
        } catch (e: Exception) {
            Log.e("HeirloomRepository", "Error reading prefs", e)
        }
    }

    fun saveProfileToPrefs(context: Context, profile: UserProfile) {
        try {
            val prefs = context.getSharedPreferences("heirloom_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean("user_onboarded", profile.isOnboardingCompleted)
                .putString("user_name", profile.name)
                .putString("user_title", profile.title)
                .putString("user_bio", profile.bio)
                .putString("user_email", profile.email)
                .putInt("user_default_servings", profile.defaultServings)
                .putStringSet("user_dietary", profile.dietaryPreferences.toSet())
                .putString("user_avatar_type", profile.avatarType)
                .putString("user_avatar_url", profile.avatarUrl)
                .putString("user_theme_palette", profile.themePalette.id)
                .apply()
        } catch (e: Exception) {
            Log.e("HeirloomRepository", "Error saving profile prefs", e)
        }
    }

    fun completeOnboarding(
        context: Context,
        name: String,
        defaultServings: Int,
        dietaryPreferences: List<String>
    ) {
        val current = _userProfile.value
        val updated = current.copy(
            name = name.trim(),
            defaultServings = defaultServings,
            dietaryPreferences = dietaryPreferences,
            isOnboardingCompleted = true
        )
        _userProfile.value = updated
        saveProfileToPrefs(context, updated)
    }

    fun setSeenWelcomeModal(seen: Boolean, context: Context? = null) {
        _hasSeenWelcomeModal.value = seen
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences("heirloom_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("has_seen_welcome_modal", seen).apply()
            } catch (e: Exception) {
                Log.e("HeirloomRepository", "Error writing prefs", e)
            }
        }
    }

    suspend fun resetToNewUser(context: Context) = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            db.recipeDao().deleteAll()
        } catch (e: Exception) {
            Log.e("HeirloomRepository", "Error clearing Room DB", e)
        }

        try {
            val prefs = context.getSharedPreferences("heirloom_prefs", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (e: Exception) {
            Log.e("HeirloomRepository", "Error clearing SharedPreferences", e)
        }

        _recipes.value = emptyList()
        _cookbooks.value = emptyList()
        _pantryItems.value = emptyList()
        _groceryItems.value = emptyList()
        _scannedCards.value = emptyList()
        _notifications.value = emptyList()
        _userProfile.value = UserProfile(name = "", isOnboardingCompleted = false)
        _hasSeenWelcomeModal.value = false
    }

    fun updatePantryStatus(itemId: String, newStatus: PantryItemStatus) {
        _pantryItems.update { list ->
            list.map { item ->
                if (item.id == itemId) {
                    val wasOut = item.status == PantryItemStatus.OUT
                    val isNowOut = newStatus == PantryItemStatus.OUT
                    val updated = item.copy(status = newStatus, addedToGroceryToday = isNowOut)
                    if (isNowOut && !wasOut) {
                        // Automatically sync to grocery list
                        addPantryToGrocery(item.name)
                    }
                    updated
                } else item
            }
        }
    }

    private fun addPantryToGrocery(itemName: String) {
        val existing = _groceryItems.value.any { it.name.contains(itemName, ignoreCase = true) }
        if (!existing) {
            val newItem = GroceryItem(
                id = "g-${System.currentTimeMillis()}",
                name = itemName,
                note = "Auto-synced from depleted pantry staple",
                tag = "Auto-Pantry",
                isChecked = false
            )
            _groceryItems.update { listOf(newItem) + it }
        }
    }

    fun toggleGroceryItem(itemId: String) {
        _groceryItems.update { list ->
            list.map { if (it.id == itemId) it.copy(isChecked = !it.isChecked) else it }
        }
    }

    fun addCustomGroceryItem(name: String, note: String = "Manual entry") {
        if (name.isBlank()) return
        val newItem = GroceryItem(
            id = "g-${System.currentTimeMillis()}",
            name = name.trim(),
            note = note,
            tag = "Manual",
            isChecked = false
        )
        _groceryItems.update { it + newItem }
    }

    fun updateCookbookCustomization(
        cookbookId: String,
        fabricColor: String,
        foilStamp: String,
        typography: String
    ) {
        _cookbooks.update { list ->
            list.map {
                if (it.id == cookbookId) {
                    it.copy(
                        fabricColor = fabricColor,
                        foilStamp = foilStamp,
                        typography = typography
                    )
                } else it
            }
        }
    }

    fun toggleCardSelection(cardId: String) {
        _scannedCards.update { list ->
            list.map { if (it.id == cardId) it.copy(isSelected = !it.isSelected) else it }
        }
    }

    fun selectAllCards(select: Boolean) {
        _scannedCards.update { list ->
            list.map { it.copy(isSelected = select) }
        }
    }

    fun addRecipe(recipe: Recipe, context: Context? = null) {
        val currentUserId = com.example.data.cloud.HeirloomCloudAuthService.currentUserId
        val userRecipe = if (recipe.userId.isBlank() && currentUserId.isNotBlank()) {
            recipe.copy(userId = currentUserId)
        } else {
            recipe
        }
        _recipes.update { listOf(userRecipe) + it }
        if (context != null) {
            com.example.data.cloud.HeirloomCloudAuthService.saveRecipeForCurrentUser(userRecipe, context)
        }
    }

    fun deleteRecipe(recipeId: String, context: Context? = null) {
        _recipes.update { list -> list.filter { it.id != recipeId } }
        if (context != null) {
            com.example.data.cloud.HeirloomCloudAuthService.deleteRecipeForCurrentUser(recipeId, context)
        }
    }

    fun onCloudUserLoggedIn(
        user: com.example.data.cloud.CloudUser,
        name: String,
        email: String,
        recipes: List<Recipe>,
        context: Context
    ) {
        val current = _userProfile.value
        val updated = current.copy(
            userId = user.id,
            name = name,
            email = email,
            isOnboardingCompleted = true
        )
        _userProfile.value = updated
        saveProfileToPrefs(context, updated)

        // Set the user's isolated recipes (Row Level Security enforced)
        _recipes.value = recipes
    }

    fun onCloudUserLoggedOut(context: Context) {
        _recipes.value = emptyList()
        _cookbooks.value = emptyList()
        _scannedCards.value = emptyList()
        _groceryItems.value = emptyList()
        _pantryItems.value = emptyList()
        val clearedProfile = UserProfile(
            userId = "",
            name = "",
            title = "Home Cook & Curator",
            bio = "Baking memories from scratch with well-loved recipes.",
            email = "",
            isOnboardingCompleted = false
        )
        _userProfile.value = clearedProfile
        saveProfileToPrefs(context, clearedProfile)
    }

    fun addScannedCard(scannedCard: ScannedCard) {
        _scannedCards.update { listOf(scannedCard) + it }
    }

    fun toggleBookmark(recipeId: String) {
        _recipes.update { list ->
            list.map { if (it.id == recipeId) it.copy(bookmarked = !it.bookmarked, isFavorite = !it.bookmarked) else it }
        }
    }

    fun toggleFavorite(recipeId: String) {
        _recipes.update { list ->
            list.map { if (it.id == recipeId) it.copy(isFavorite = !it.isFavorite, bookmarked = !it.isFavorite) else it }
        }
    }

    fun clearCheckedGroceryItems() {
        _groceryItems.update { list ->
            list.filter { !it.isChecked }
        }
    }

    fun addCustomPantryItem(name: String, note: String, category: String = "Baking & Pantry Grains") {
        if (name.isBlank()) return
        val newItem = PantryItem(
            id = "p-${System.currentTimeMillis()}",
            name = name.trim(),
            note = note.ifBlank { "Added manually" },
            category = category,
            status = PantryItemStatus.IN
        )
        _pantryItems.update { it + newItem }
    }

    fun createCookbook(
        title: String,
        subtitle: String,
        description: String,
        fabricColor: String,
        foilStamp: String,
        typography: String
    ) {
        val newBook = Cookbook(
            id = "cb-${System.currentTimeMillis()}",
            title = title.ifBlank { "Untitled Binder" },
            subtitle = subtitle.ifBlank { "Personalized Collection" },
            description = description.ifBlank { "A curated collection of culinary treasures." },
            recipeCount = 0,
            lastUpdated = "Just now",
            badge = "New",
            fabricColor = fabricColor,
            foilStamp = foilStamp,
            typography = typography,
            recipes = emptyList()
        )
        _cookbooks.update { it + newBook }
    }

    fun reorderCookbookRecipes(cookbookId: String, reorderedRecipeIds: List<String>) {
        _cookbooks.update { list ->
            list.map {
                if (it.id == cookbookId) {
                    it.copy(recipes = reorderedRecipeIds, recipeCount = reorderedRecipeIds.size)
                } else it
            }
        }
    }

    // Notifications State
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    fun markAllNotificationsRead() {
        _notifications.update { list -> list.map { it.copy(isRead = true) } }
    }

    fun dismissNotification(id: String) {
        _notifications.update { list -> list.filter { it.id != id } }
    }

    // User Profile & Kitchen Preferences State
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    fun updateProfile(
        name: String,
        title: String,
        email: String,
        measurementUnits: String,
        defaultServings: Int,
        keepAwake: Boolean,
        autoSync: Boolean,
        haptic: Boolean,
        bio: String = _userProfile.value.bio,
        avatarType: String = _userProfile.value.avatarType,
        dietaryPreferences: List<String> = _userProfile.value.dietaryPreferences,
        compactFeedView: Boolean = _userProfile.value.compactFeedView,
        keepScreenAwakeByDefault: Boolean = _userProfile.value.keepScreenAwakeByDefault,
        themePalette: AppThemePalette = _userProfile.value.themePalette,
        themeMode: ThemeMode = _userProfile.value.themeMode,
        context: Context? = null
    ) {
        val updated = UserProfile(
            name = name,
            title = title,
            bio = bio,
            email = email,
            avatarType = avatarType,
            avatarUrl = _userProfile.value.avatarUrl,
            measurementUnits = measurementUnits,
            defaultServings = defaultServings,
            dietaryPreferences = dietaryPreferences,
            keepAwakeOnCookMode = keepAwake,
            keepScreenAwakeByDefault = keepScreenAwakeByDefault,
            compactFeedView = compactFeedView,
            autoSyncPantryToGrocery = autoSync,
            hapticFeedback = haptic,
            themePalette = themePalette,
            themeMode = themeMode,
            isOnboardingCompleted = _userProfile.value.isOnboardingCompleted || name.isNotBlank()
        )
        _userProfile.value = updated
        if (context != null) {
            saveProfileToPrefs(context, updated)
            com.example.data.cloud.HeirloomCloudAuthService.updateProfileForCurrentUser(updated, context)
        }
    }

    fun updateDietaryPreferences(preferences: List<String>, context: Context? = null) {
        _userProfile.update { current ->
            val updated = current.copy(dietaryPreferences = preferences)
            if (context != null) saveProfileToPrefs(context, updated)
            updated
        }
    }

    fun toggleDietaryPreference(pref: String, context: Context? = null) {
        _userProfile.update { current ->
            val updatedList = if (current.dietaryPreferences.contains(pref)) {
                current.dietaryPreferences - pref
            } else {
                current.dietaryPreferences + pref
            }
            val updated = current.copy(dietaryPreferences = updatedList)
            if (context != null) saveProfileToPrefs(context, updated)
            updated
        }
    }

    fun setDefaultServings(servings: Int, context: Context? = null) {
        _userProfile.update { current ->
            val updated = current.copy(defaultServings = servings.coerceIn(1, 24))
            if (context != null) saveProfileToPrefs(context, updated)
            updated
        }
    }

    fun setCompactFeedView(compact: Boolean) {
        _userProfile.update { it.copy(compactFeedView = compact) }
    }

    fun setKeepScreenAwakeByDefault(keepAwake: Boolean) {
        _userProfile.update { it.copy(keepScreenAwakeByDefault = keepAwake, keepAwakeOnCookMode = keepAwake) }
    }

    fun setAvatarType(type: String, context: Context? = null) {
        _userProfile.update { current ->
            val updated = current.copy(avatarType = type)
            if (context != null) saveProfileToPrefs(context, updated)
            updated
        }
    }

    fun resetAccountData(context: Context? = null) {
        _recipes.value = emptyList()
        _cookbooks.value = emptyList()
        _scannedCards.value = emptyList()
        _groceryItems.value = emptyList()
        _pantryItems.value = emptyList()
        val emptyProfile = UserProfile(
            name = "",
            title = "Home Cook & Curator",
            bio = "Baking memories from scratch with well-loved recipes.",
            email = "",
            avatarType = "wooden_spoon",
            isOnboardingCompleted = false
        )
        _userProfile.value = emptyProfile
        if (context != null) {
            saveProfileToPrefs(context, emptyProfile)
        }
    }

    fun exportRecipeTinJson(): String {
        val list = _recipes.value
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"appName\": \"Between Stained Pages\",\n")
        sb.append("  \"exportedAt\": \"${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())}\",\n")
        sb.append("  \"recipeCount\": ${list.size},\n")
        sb.append("  \"recipes\": [\n")
        list.forEachIndexed { index, recipe ->
            sb.append("    {\n")
            sb.append("      \"id\": \"${recipe.id}\",\n")
            sb.append("      \"title\": \"${recipe.title.replace("\"", "\\\"")}\",\n")
            sb.append("      \"prepTime\": \"${recipe.prepTime}\",\n")
            sb.append("      \"cookTime\": \"${recipe.cookTime}\",\n")
            sb.append("      \"servings\": ${recipe.servings},\n")
            sb.append("      \"binderCategory\": \"${recipe.binderCategory}\",\n")
            sb.append("      \"ingredients\": [")
            sb.append(recipe.ingredients.joinToString(", ") { "\"${it.name.replace("\"", "\\\"")}\"" })
            sb.append("],\n")
            sb.append("      \"instructions\": [")
            sb.append(recipe.steps.joinToString(", ") { "\"${it.instruction.replace("\"", "\\\"")}\"" })
            sb.append("]\n")
            sb.append("    }${if (index < list.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun exportRecipeTinCsv(): String {
        val list = _recipes.value
        val sb = StringBuilder()
        sb.append("Title,Prep Time,Cook Time,Servings,Category,Ingredients,Instructions\n")
        list.forEach { recipe ->
            val ingredientsStr = recipe.ingredients.joinToString("; ") { it.name }.replace("\"", "\"\"")
            val instructionsStr = recipe.steps.joinToString("; ") { "${it.stepNumber}. ${it.instruction}" }.replace("\"", "\"\"")
            val titleStr = recipe.title.replace("\"", "\"\"")
            sb.append("\"$titleStr\",\"${recipe.prepTime}\",\"${recipe.cookTime}\",${recipe.servings},\"${recipe.binderCategory}\",\"$ingredientsStr\",\"$instructionsStr\"\n")
        }
        return sb.toString()
    }

    fun setThemePalette(palette: AppThemePalette) {
        _userProfile.update { it.copy(themePalette = palette) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _userProfile.update { it.copy(themeMode = mode) }
    }

    // Device Sync State
    private val _syncDevices = MutableStateFlow(
        listOf(
            DeviceSyncItem("dev-1", "Kitchen Tablet & Reader", "Android Device", "Active now", isCurrentDevice = true),
            DeviceSyncItem("dev-2", "Kitchen Countertop Stand", "Tablet", "15m ago"),
            DeviceSyncItem("dev-3", "Family Galaxy Tab", "Tablet", "Yesterday at 6:45 PM"),
            DeviceSyncItem("dev-4", "Pantry Smart Display Hub", "Smart Display", "3 days ago")
        )
    )
    val syncDevices: StateFlow<List<DeviceSyncItem>> = _syncDevices.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow("Today at 2:45 PM")
    val lastSyncTimestamp: StateFlow<String> = _lastSyncTimestamp.asStateFlow()

    fun triggerCloudSync() {
        _lastSyncTimestamp.value = "Just now"
    }
}
