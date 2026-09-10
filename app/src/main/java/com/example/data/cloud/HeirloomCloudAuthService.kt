package com.example.data.cloud

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.BuildConfig
import com.example.data.CookingStep
import com.example.data.HeirloomRepository
import com.example.data.IngredientItem
import com.example.data.Recipe
import com.example.data.RecipeSourceType
import com.example.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Represents an authenticated Cloud User with security tokens and unique User ID (`user_id`).
 */
data class CloudUser(
    val id: String,
    val email: String,
    val name: String,
    val token: String,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Cloud Authentication and Persistent Storage Service integrating with Supabase.
 */
object HeirloomCloudAuthService {

    private const val TAG = "HeirloomCloudAuth"
    private const val PREFS_NAME = "heirloom_cloud_vault"

    // Local Cache Keys
    private const val KEY_USERS_TABLE = "cloud_table_users"
    private const val KEY_PROFILES_TABLE = "cloud_table_profiles"
    private const val KEY_RECIPES_TABLE = "cloud_table_recipes"
    private const val KEY_ACTIVE_SESSION_USER_ID = "cloud_session_user_id"
    private const val KEY_ACTIVE_SESSION_TOKEN = "cloud_session_token"

    private val _currentUser = MutableStateFlow<CloudUser?>(null)
    val currentUser: StateFlow<CloudUser?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentUserId: String
        get() = _currentUser.value?.id ?: ""

    val isAuthenticated: Boolean
        get() = _currentUser.value != null

    // Base client
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Initializes the cloud authentication system from persistent vault storage.
     * Restores active cloud session if present, or sets unauthenticated state.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Seed default local demo cloud user if first run ever (for fallback testing)
        seedDemoCloudDatabaseIfEmpty(prefs)

        val activeUserId = prefs.getString(KEY_ACTIVE_SESSION_USER_ID, null)
        val activeToken = prefs.getString(KEY_ACTIVE_SESSION_TOKEN, null)

        if (!activeUserId.isNullOrBlank() && !activeToken.isNullOrBlank()) {
            val usersJson = prefs.getString(KEY_USERS_TABLE, "{}") ?: "{}"
            try {
                val usersObj = JSONObject(usersJson)
                if (usersObj.has(activeUserId)) {
                    val userObj = usersObj.getJSONObject(activeUserId)
                    val user = CloudUser(
                        id = userObj.getString("id"),
                        email = userObj.getString("email"),
                        name = userObj.getString("name"),
                        token = activeToken,
                        createdAt = userObj.optLong("createdAt", System.currentTimeMillis())
                    )
                    _currentUser.value = user
                    // Load user's data (fetch from Supabase or fallback to local cache)
                    restoreUserCloudData(context, prefs, activeUserId, user.name, activeToken)
                    Log.d(TAG, "Restored active cloud session for user: ${user.email} (${user.id})")
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring session from cloud vault", e)
            }
        }

        // No active user session
        _currentUser.value = null
    }

    /**
     * Creates a new cloud account with name, email, and password using Supabase Auth.
     */
    suspend fun signUp(
        name: String,
        email: String,
        password: String,
        context: Context
    ): Result<CloudUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val trimmedName = name.trim()
        val normalizedEmail = email.trim().lowercase()

        try {
            Log.d(TAG, "Attempting Supabase SignUp for $normalizedEmail")

            val url = "${BuildConfig.SUPABASE_URL}/auth/v1/signup"
            val jsonBody = JSONObject().apply {
                put("email", normalizedEmail)
                put("password", password)
                put("data", JSONObject().apply {
                    put("name", trimmedName)
                })
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "SignUp response code: ${response.code}, body: $bodyStr")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val userJson = jsonObj.optJSONObject("user") ?: jsonObj
                    val userId = userJson.optString("id") ?: "usr_${UUID.randomUUID().toString().take(8)}"
                    val token = jsonObj.optString("access_token", "jwt_cloud_${UUID.randomUUID()}")

                    val cloudUser = CloudUser(
                        id = userId,
                        email = normalizedEmail,
                        name = trimmedName,
                        token = token,
                        createdAt = System.currentTimeMillis()
                    )

                    // Update Local Database cache
                    saveUserLocalCache(context, cloudUser, password, trimmedName)

                    withContext(Dispatchers.Main) {
                        _currentUser.value = cloudUser
                        HeirloomRepository.onCloudUserLoggedIn(
                            user = cloudUser,
                            name = trimmedName,
                            email = normalizedEmail,
                            recipes = emptyList(), // Brand-new empty tin
                            context = context
                        )
                    }

                    Result.success(cloudUser)
                } else {
                    val errorObj = JSONObject(bodyStr)
                    val desc = errorObj.optString("error_description", errorObj.optString("msg", "Sign up failed"))
                    Result.failure(Exception(desc))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase sign up error, trying fallback local register", e)
            // Fallback to local registry if offline
            fallbackLocalRegister(trimmedName, normalizedEmail, password, context)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Authenticates an existing user with email and password using Supabase Auth.
     */
    suspend fun logIn(
        email: String,
        password: String,
        context: Context
    ): Result<CloudUser> = withContext(Dispatchers.IO) {
        _isLoading.value = true
        val normalizedEmail = email.trim().lowercase()

        try {
            Log.d(TAG, "Attempting Supabase LogIn for $normalizedEmail")

            val url = "${BuildConfig.SUPABASE_URL}/auth/v1/token?grant_type=password"
            val jsonBody = JSONObject().apply {
                put("email", normalizedEmail)
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                Log.d(TAG, "LogIn response code: ${response.code}, body: $bodyStr")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val token = jsonObj.getString("access_token")
                    val userObj = jsonObj.getJSONObject("user")
                    val userId = userObj.getString("id")
                    val metadata = userObj.optJSONObject("user_metadata")
                    val userName = metadata?.optString("name") ?: normalizedEmail.substringBefore("@")

                    val cloudUser = CloudUser(
                        id = userId,
                        email = normalizedEmail,
                        name = userName,
                        token = token,
                        createdAt = System.currentTimeMillis()
                    )

                    // Update local cache
                    saveUserLocalCache(context, cloudUser, password, userName)

                    // Fetch recipes from Supabase 'recipes' table
                    val fetchedRecipes = fetchRecipesFromSupabase(userId, token)

                    withContext(Dispatchers.Main) {
                        _currentUser.value = cloudUser
                        HeirloomRepository.onCloudUserLoggedIn(
                            user = cloudUser,
                            name = userName,
                            email = normalizedEmail,
                            recipes = fetchedRecipes,
                            context = context
                        )
                    }

                    Result.success(cloudUser)
                } else {
                    val errorObj = JSONObject(bodyStr)
                    val desc = errorObj.optString("error_description", errorObj.optString("msg", "Invalid credentials"))
                    Result.failure(Exception(desc))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase login error, trying fallback local authentication", e)
            fallbackLocalAuthentication(normalizedEmail, password, context)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Signs out the current user and clears active cloud session tokens.
     */
    fun logOut(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ACTIVE_SESSION_USER_ID)
            .remove(KEY_ACTIVE_SESSION_TOKEN)
            .apply()

        _currentUser.value = null
        HeirloomRepository.onCloudUserLoggedOut(context)
        Log.i(TAG, "User signed out. Cloud session terminated.")
    }

    /**
     * Saves or updates a recipe in Supabase 'recipes' table, or falls back to local cache if offline.
     */
    fun saveRecipeForCurrentUser(recipe: Recipe, context: Context) {
        val userId = currentUserId
        val token = _currentUser.value?.token ?: ""
        if (userId.isBlank()) {
            Log.w(TAG, "Cannot save recipe: No authenticated user session.")
            return
        }

        // Always save to local cache first
        saveRecipeToLocalCache(recipe, userId, context)

        // Sync to Supabase in background
        if (token.isNotBlank()) {
            val jsonObject = serializeRecipeToJson(recipe.copy(userId = userId))
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/recipes"

            val request = Request.Builder()
                .url(url)
                .post(jsonObject.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .header("Prefer", "resolution=merge-duplicates")
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.w(TAG, "Failed to insert recipe into Supabase: ${e.message}. Retained locally.")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            Log.w(TAG, "Supabase response unsuccessful while saving recipe: ${it.code} - ${it.message}")
                        } else {
                            Log.i(TAG, "Recipe '${recipe.title}' synced successfully to Supabase.")
                        }
                    }
                }
            })
        }
    }

    /**
     * Deletes a recipe from Supabase and local cache.
     */
    fun deleteRecipeForCurrentUser(recipeId: String, context: Context) {
        val userId = currentUserId
        val token = _currentUser.value?.token ?: ""
        if (userId.isBlank()) return

        // 1. Delete from local cache
        deleteRecipeFromLocalCache(recipeId, userId, context)

        // 2. Delete from Supabase
        if (token.isNotBlank()) {
            val url = "${BuildConfig.SUPABASE_URL}/rest/v1/recipes?id=eq.$recipeId"
            val request = Request.Builder()
                .url(url)
                .delete()
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.w(TAG, "Failed to delete recipe from Supabase: ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            Log.w(TAG, "Supabase delete response unsuccessful: ${it.code}")
                        } else {
                            Log.i(TAG, "Recipe $recipeId deleted successfully from Supabase.")
                        }
                    }
                }
            })
        }
    }

    /**
     * Updates user profile in Supabase profiles/metadata and local cache.
     */
    fun updateProfileForCurrentUser(profile: UserProfile, context: Context) {
        val userId = currentUserId
        val token = _currentUser.value?.token ?: ""
        if (userId.isBlank()) return

        // Always update local cache
        updateLocalProfileCache(profile, userId, context)

        // Try updating Supabase User metadata
        if (token.isNotBlank()) {
            val url = "${BuildConfig.SUPABASE_URL}/auth/v1/user"
            val jsonBody = JSONObject().apply {
                put("data", JSONObject().apply {
                    put("name", profile.name)
                    put("title", profile.title)
                    put("bio", profile.bio)
                })
            }

            val request = Request.Builder()
                .url(url)
                .put(jsonBody.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .header("Authorization", "Bearer $token")
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    Log.w(TAG, "Failed to sync profile update to Supabase: ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            Log.w(TAG, "Supabase profile sync returned code: ${it.code}")
                        } else {
                            Log.i(TAG, "Profile synchronized successfully to Supabase.")
                        }
                    }
                }
            })
        }
    }

    /**
     * Queries recipes directly from the Supabase 'recipes' table.
     */
    private fun fetchRecipesFromSupabase(userId: String, token: String): List<Recipe> {
        val url = "${BuildConfig.SUPABASE_URL}/rest/v1/recipes?select=*"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Authorization", "Bearer $token")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val array = JSONArray(bodyStr)
                    val list = mutableListOf<Recipe>()
                    for (i in 0 until array.length()) {
                        list.add(deserializeRecipeFromJson(array.getJSONObject(i), userId))
                    }
                    Log.d(TAG, "Successfully fetched ${list.size} recipes from Supabase.")
                    return list
                } else {
                    Log.w(TAG, "Supabase recipe query unsuccessful: ${response.code} - $bodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception querying recipes from Supabase", e)
        }
        return emptyList()
    }

    /**
     * Recovers cached local data when Supabase is initialized or offline.
     */
    private fun restoreUserCloudData(
        context: Context,
        prefs: SharedPreferences,
        userId: String,
        fallbackName: String,
        token: String
    ) {
        val localRecipes = loadRecipesForUser(prefs, userId)
        val localName = loadProfileNameForUser(prefs, userId, fallbackName)

        // Try to async sync/fetch latest from Supabase
        val user = _currentUser.value ?: return
        HeirloomRepository.onCloudUserLoggedIn(
            user = user,
            name = localName,
            email = user.email,
            recipes = localRecipes,
            context = context
        )

        // Fire background refresh
        val job = kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            val remote = fetchRecipesFromSupabase(userId, token)
            if (remote.isNotEmpty()) {
                // Merge/save locally
                withContext(Dispatchers.Main) {
                    HeirloomRepository.onCloudUserLoggedIn(
                        user = user,
                        name = localName,
                        email = user.email,
                        recipes = remote,
                        context = context
                    )
                }
                // Update local cache
                val recipesObj = JSONObject(prefs.getString(KEY_RECIPES_TABLE, "{}") ?: "{}")
                val jsonArray = JSONArray()
                remote.forEach { jsonArray.put(serializeRecipeToJson(it)) }
                recipesObj.put(userId, jsonArray)
                prefs.edit().putString(KEY_RECIPES_TABLE, recipesObj.toString()).apply()
            }
        }
    }

    /**
     * Fallback register when network/Supabase is offline or not configured.
     */
    private suspend fun fallbackLocalRegister(
        name: String,
        email: String,
        password: String,
        context: Context
    ): Result<CloudUser> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val usersJson = prefs.getString(KEY_USERS_TABLE, "{}") ?: "{}"
            val usersObj = JSONObject(usersJson)

            // Check uniqueness
            val keys = usersObj.keys()
            while (keys.hasNext()) {
                val existingUserId = keys.next()
                val existingUser = usersObj.getJSONObject(existingUserId)
                if (existingUser.getString("email").equals(email, ignoreCase = true)) {
                    return@withContext Result.failure(IllegalStateException("Account already exists locally. Please log in."))
                }
            }

            val fallbackUserId = "usr_loc_${UUID.randomUUID().toString().take(8)}"
            val salt = "local_salt"
            val hash = hashPassword(password, salt)
            val token = "local_token_${UUID.randomUUID()}"

            val newUserObj = JSONObject().apply {
                put("id", fallbackUserId)
                put("email", email)
                put("name", name)
                put("salt", salt)
                put("passwordHash", hash)
                put("createdAt", System.currentTimeMillis())
            }
            usersObj.put(fallbackUserId, newUserObj)
            prefs.edit().putString(KEY_USERS_TABLE, usersObj.toString()).apply()

            val cloudUser = CloudUser(fallbackUserId, email, name, token)
            saveUserLocalCache(context, cloudUser, password, name)

            withContext(Dispatchers.Main) {
                _currentUser.value = cloudUser
                HeirloomRepository.onCloudUserLoggedIn(cloudUser, name, email, emptyList(), context)
            }
            Result.success(cloudUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fallback log in when network/Supabase is offline.
     */
    private suspend fun fallbackLocalAuthentication(
        email: String,
        password: String,
        context: Context
    ): Result<CloudUser> = withContext(Dispatchers.IO) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val usersJson = prefs.getString(KEY_USERS_TABLE, "{}") ?: "{}"
            val usersObj = JSONObject(usersJson)

            var matchedUserId: String? = null
            var matchedUserObj: JSONObject? = null

            val keys = usersObj.keys()
            while (keys.hasNext()) {
                val userId = keys.next()
                val u = usersObj.getJSONObject(userId)
                if (u.getString("email").equals(email, ignoreCase = true)) {
                    matchedUserId = userId
                    matchedUserObj = u
                    break
                }
            }

            if (matchedUserId == null || matchedUserObj == null) {
                return@withContext Result.failure(IllegalArgumentException("No local credentials found. Please connect to internet to sign in."))
            }

            val salt = matchedUserObj.getString("salt")
            val expectedHash = matchedUserObj.getString("passwordHash")
            val inputHash = hashPassword(password, salt)

            if (expectedHash != inputHash) {
                return@withContext Result.failure(IllegalArgumentException("Incorrect password."))
            }

            val token = "local_token_${UUID.randomUUID()}"
            val userName = matchedUserObj.getString("name")

            val cloudUser = CloudUser(matchedUserId, email, userName, token)
            prefs.edit()
                .putString(KEY_ACTIVE_SESSION_USER_ID, matchedUserId)
                .putString(KEY_ACTIVE_SESSION_TOKEN, token)
                .apply()

            val localRecipes = loadRecipesForUser(prefs, matchedUserId)

            withContext(Dispatchers.Main) {
                _currentUser.value = cloudUser
                HeirloomRepository.onCloudUserLoggedIn(cloudUser, userName, email, localRecipes, context)
            }
            Result.success(cloudUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Core Helper Cache Functions ---

    private fun saveUserLocalCache(context: Context, user: CloudUser, passwordSecret: String, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val usersObj = JSONObject(prefs.getString(KEY_USERS_TABLE, "{}") ?: "{}")
            if (!usersObj.has(user.id)) {
                val salt = "salt_" + UUID.randomUUID().toString().take(6)
                val newUserObj = JSONObject().apply {
                    put("id", user.id)
                    put("email", user.email)
                    put("name", name)
                    put("salt", salt)
                    put("passwordHash", hashPassword(passwordSecret, salt))
                    put("createdAt", System.currentTimeMillis())
                }
                usersObj.put(user.id, newUserObj)
                prefs.edit().putString(KEY_USERS_TABLE, usersObj.toString()).apply()
            }

            prefs.edit()
                .putString(KEY_ACTIVE_SESSION_USER_ID, user.id)
                .putString(KEY_ACTIVE_SESSION_TOKEN, user.token)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user cache", e)
        }
    }

    private fun saveRecipeToLocalCache(recipe: Recipe, userId: String, context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val recipesJson = prefs.getString(KEY_RECIPES_TABLE, "{}") ?: "{}"
            val recipesObj = JSONObject(recipesJson)

            val currentArray = if (recipesObj.has(userId)) {
                recipesObj.getJSONArray(userId)
            } else {
                JSONArray()
            }

            val newArray = JSONArray()
            var replaced = false
            val userRecipe = recipe.copy(userId = userId)
            val recipeJson = serializeRecipeToJson(userRecipe)

            for (i in 0 until currentArray.length()) {
                val item = currentArray.getJSONObject(i)
                if (item.optString("id") == recipe.id) {
                    newArray.put(recipeJson)
                    replaced = true
                } else {
                    newArray.put(item)
                }
            }

            if (!replaced) {
                newArray.put(recipeJson)
            }

            recipesObj.put(userId, newArray)
            prefs.edit().putString(KEY_RECIPES_TABLE, recipesObj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving recipe to local cache", e)
        }
    }

    private fun deleteRecipeFromLocalCache(recipeId: String, userId: String, context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val recipesJson = prefs.getString(KEY_RECIPES_TABLE, "{}") ?: "{}"
            val recipesObj = JSONObject(recipesJson)

            if (recipesObj.has(userId)) {
                val currentArray = recipesObj.getJSONArray(userId)
                val newArray = JSONArray()
                for (i in 0 until currentArray.length()) {
                    val item = currentArray.getJSONObject(i)
                    if (item.optString("id") != recipeId) {
                        newArray.put(item)
                    }
                }
                recipesObj.put(userId, newArray)
                prefs.edit().putString(KEY_RECIPES_TABLE, recipesObj.toString()).apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting recipe from cache", e)
        }
    }

    private fun updateLocalProfileCache(profile: UserProfile, userId: String, context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val profilesJson = prefs.getString(KEY_PROFILES_TABLE, "{}") ?: "{}"
            val profilesObj = JSONObject(profilesJson)

            val pObj = JSONObject().apply {
                put("userId", userId)
                put("name", profile.name)
                put("email", profile.email)
                put("title", profile.title)
                put("bio", profile.bio)
                put("defaultServings", profile.defaultServings)
                put("dietaryPreferences", JSONArray(profile.dietaryPreferences))
                put("avatarType", profile.avatarType)
                put("themePalette", profile.themePalette.id)
                put("updatedAt", System.currentTimeMillis())
            }

            profilesObj.put(userId, pObj)
            prefs.edit().putString(KEY_PROFILES_TABLE, profilesObj.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error caching profile", e)
        }
    }

    fun loadRecipesForUser(prefs: SharedPreferences, userId: String): List<Recipe> {
        val recipesJson = prefs.getString(KEY_RECIPES_TABLE, "{}") ?: "{}"
        val result = mutableListOf<Recipe>()
        try {
            val obj = JSONObject(recipesJson)
            if (obj.has(userId)) {
                val array = obj.getJSONArray(userId)
                for (i in 0 until array.length()) {
                    val rObj = array.getJSONObject(i)
                    result.add(deserializeRecipeFromJson(rObj, userId))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading user recipes", e)
        }
        return result
    }

    private fun loadProfileNameForUser(prefs: SharedPreferences, userId: String, fallback: String): String {
        val profilesJson = prefs.getString(KEY_PROFILES_TABLE, "{}") ?: "{}"
        try {
            val obj = JSONObject(profilesJson)
            if (obj.has(userId)) {
                return obj.getJSONObject(userId).optString("name", fallback)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading profile name", e)
        }
        return fallback
    }

    private fun hashPassword(password: String, salt: String): String {
        val input = "$salt:$password"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun serializeRecipeToJson(recipe: Recipe): JSONObject {
        return JSONObject().apply {
            put("id", recipe.id)
            put("userId", recipe.userId)
            put("user_id", recipe.userId)
            put("title", recipe.title)
            put("source", recipe.source)
            put("sourceType", recipe.sourceType.name)
            put("source_type", recipe.sourceType.name)
            put("description", recipe.description)
            put("imageUrl", recipe.imageUrl)
            put("image_url", recipe.imageUrl)
            put("prepTime", recipe.prepTime)
            put("prep_time", recipe.prepTime)
            put("cookTime", recipe.cookTime)
            put("cook_time", recipe.cookTime)
            put("servings", recipe.servings)
            put("difficulty", recipe.difficulty)
            put("binderCategory", recipe.binderCategory)
            put("binder_category", recipe.binderCategory)
            put("pantryStatusText", recipe.pantryStatusText)
            put("pantry_status_text", recipe.pantryStatusText)
            put("allIngredientsInPantry", recipe.allIngredientsInPantry)
            put("all_ingredients_in_pantry", recipe.allIngredientsInPantry)
            put("secretNote", recipe.secretNote ?: "")
            put("secret_note", recipe.secretNote ?: "")
            put("audioNoteTitle", recipe.audioNoteTitle ?: "")
            put("audio_note_title", recipe.audioNoteTitle ?: "")
            put("audioNoteDuration", recipe.audioNoteDuration ?: "")
            put("audio_note_duration", recipe.audioNoteDuration ?: "")
            put("bookmarked", recipe.bookmarked)
            put("isFavorite", recipe.isFavorite)
            put("is_favorite", recipe.isFavorite)

            val ingArray = JSONArray()
            recipe.ingredients.forEach { ing ->
                ingArray.put(JSONObject().apply {
                    put("id", ing.id)
                    put("name", ing.name)
                    put("note", ing.note)
                    put("category", ing.category)
                    put("inPantry", ing.inPantry)
                    put("isMarkedOut", ing.isMarkedOut)
                    put("confidence", ing.confidence)
                })
            }
            put("ingredients", ingArray)

            val stepArray = JSONArray()
            recipe.steps.forEach { s ->
                stepArray.put(JSONObject().apply {
                    put("stepNumber", s.stepNumber)
                    put("title", s.title)
                    put("instruction", s.instruction)
                    put("durationMinutes", s.durationMinutes ?: 0)
                })
            }
            put("steps", stepArray)
        }
    }

    private fun deserializeRecipeFromJson(obj: JSONObject, userId: String): Recipe {
        val ingredients = mutableListOf<IngredientItem>()
        val ingArray = obj.optJSONArray("ingredients")
        if (ingArray != null) {
            for (i in 0 until ingArray.length()) {
                val item = ingArray.getJSONObject(i)
                ingredients.add(
                    IngredientItem(
                        id = item.optString("id", "i-$i"),
                        name = item.optString("name", ""),
                        note = item.optString("note", ""),
                        category = item.optString("category", "Pantry"),
                        inPantry = item.optBoolean("inPantry", true),
                        isMarkedOut = item.optBoolean("isMarkedOut", false),
                        confidence = item.optInt("confidence", 98)
                    )
                )
            }
        }

        val steps = mutableListOf<CookingStep>()
        val stepArray = obj.optJSONArray("steps")
        if (stepArray != null) {
            for (i in 0 until stepArray.length()) {
                val item = stepArray.getJSONObject(i)
                steps.add(
                    CookingStep(
                        stepNumber = item.optInt("stepNumber", i + 1),
                        title = item.optString("title", "Step ${i + 1}"),
                        instruction = item.optString("instruction", ""),
                        durationMinutes = item.optInt("durationMinutes", 10).takeIf { it > 0 }
                    )
                )
            }
        }

        val sourceTypeStr = obj.optString("source_type", obj.optString("sourceType", RecipeSourceType.FAMILY_NOTE.name))
        val sourceType = try {
            RecipeSourceType.valueOf(sourceTypeStr)
        } catch (e: Exception) {
            RecipeSourceType.FAMILY_NOTE
        }

        return Recipe(
            id = obj.getString("id"),
            userId = obj.optString("user_id", obj.optString("userId", userId)),
            title = obj.getString("title"),
            source = obj.optString("source", "Family archive"),
            sourceType = sourceType,
            description = obj.optString("description", ""),
            imageUrl = obj.optString("image_url", obj.optString("imageUrl", "")),
            prepTime = obj.optString("prep_time", obj.optString("prepTime", "15m")),
            cookTime = obj.optString("cook_time", obj.optString("cookTime", "30m")),
            servings = obj.optInt("servings", 4),
            difficulty = obj.optString("difficulty", "Easy"),
            binderCategory = obj.optString("binder_category", obj.optString("binderCategory", "Family Binders")),
            pantryStatusText = obj.optString("pantry_status_text", obj.optString("pantryStatusText", "All ingredients in pantry")),
            allIngredientsInPantry = obj.optBoolean("all_ingredients_in_pantry", obj.optBoolean("allIngredientsInPantry", true)),
            ingredients = ingredients,
            steps = steps,
            secretNote = obj.optString("secret_note", obj.optString("secretNote", "")).takeIf { it.isNotBlank() },
            audioNoteTitle = obj.optString("audio_note_title", obj.optString("audioNoteTitle", "")).takeIf { it.isNotBlank() },
            audioNoteDuration = obj.optString("audio_note_duration", obj.optString("audioNoteDuration", "")).takeIf { it.isNotBlank() },
            bookmarked = obj.optBoolean("bookmarked", false),
            isFavorite = obj.optBoolean("is_favorite", obj.optBoolean("isFavorite", false))
        )
    }

    private fun seedDemoCloudDatabaseIfEmpty(prefs: SharedPreferences) {
        val usersJson = prefs.getString(KEY_USERS_TABLE, null)
        if (usersJson != null) return // Already initialized

        try {
            val demoUserId = "usr_demo_eleanor"
            val demoEmail = "cook@stainedpages.app"
            val demoSalt = "salt_demo_kitchen_1950"
            val demoPasswordHash = hashPassword("heirloom123", demoSalt)

            // Users Table
            val usersObj = JSONObject().apply {
                put(demoUserId, JSONObject().apply {
                    put("id", demoUserId)
                    put("email", demoEmail)
                    put("name", "Eleanor Vance")
                    put("salt", demoSalt)
                    put("passwordHash", demoPasswordHash)
                    put("createdAt", System.currentTimeMillis())
                })
            }

            // Profiles Table
            val profilesObj = JSONObject().apply {
                put(demoUserId, JSONObject().apply {
                    put("userId", demoUserId)
                    put("name", "Eleanor Vance")
                    put("email", demoEmail)
                    put("title", "Heirloom Recipe Curator")
                    put("bio", "Preserving family marginalia, handwritten cards, and kitchen love.")
                    put("defaultServings", 4)
                    put("dietaryPreferences", JSONArray(listOf("Vegetarian", "Dairy-Free")))
                    put("avatarType", "wooden_spoon")
                    put("themePalette", "terracotta")
                    put("updatedAt", System.currentTimeMillis())
                })
            }

            // Recipes Table: Pre-load Eleanor's family recipes for returning user demo
            val sampleCobbler = Recipe(
                id = "rec-demo-1",
                userId = demoUserId,
                title = "Grandma's Apple Cinnamon Cobbler",
                source = "Handwritten index card (1952)",
                sourceType = RecipeSourceType.VINTAGE_SCAN,
                description = "Warm bubbling honeycrisp apples baked under a golden butter biscuit crust.",
                imageUrl = "https://images.unsplash.com/photo-1568571780765-9276ac8b75a2?auto=format&fit=crop&w=600&q=80",
                prepTime = "25m",
                cookTime = "45m",
                servings = 6,
                difficulty = "Medium",
                binderCategory = "Desserts & Bakes",
                pantryStatusText = "All ingredients in pantry",
                allIngredientsInPantry = true,
                ingredients = listOf(
                    IngredientItem("i1", "6 Honeycrisp apples, peeled & sliced", category = "Produce"),
                    IngredientItem("i2", "2 cups all-purpose flour", category = "Baking & Pantry Grains"),
                    IngredientItem("i3", "1/2 cup brown sugar", category = "Baking & Pantry Grains"),
                    IngredientItem("i4", "1 tsp ground Ceylon cinnamon", category = "Spices")
                ),
                steps = listOf(
                    CookingStep(1, "Prep Apples", "Toss sliced apples with brown sugar and cinnamon in stoneware bowl.", 10),
                    CookingStep(2, "Biscuit Batter", "Cut cold butter into flour until pea-sized crumbs form.", 15),
                    CookingStep(3, "Bake to Golden", "Drop spoonfuls of dough over apples and bake at 375F until bubbly.", 45)
                ),
                secretNote = "Grandma Clara always used a pinch of freshly grated nutmeg.",
                audioNoteTitle = "Grandma's Apple Cobbler Crust Tip (1984)",
                audioNoteDuration = "1m 14s",
                bookmarked = true,
                isFavorite = true
            )

            val sampleLemonPie = Recipe(
                id = "rec-demo-2",
                userId = demoUserId,
                title = "Aunt Clara's Lemon Meringue",
                source = "Family Kitchen Binder",
                sourceType = RecipeSourceType.FAMILY_NOTE,
                description = "Tangy Meyer lemon curd crowned with toasted peaks of cloud-soft meringue.",
                imageUrl = "https://images.unsplash.com/photo-1519915028121-7d3463d20b13?auto=format&fit=crop&w=600&q=80",
                prepTime = "30m",
                cookTime = "20m",
                servings = 8,
                difficulty = "Advanced",
                binderCategory = "Sunday Bakes",
                pantryStatusText = "Need lemons",
                allIngredientsInPantry = false,
                ingredients = listOf(
                    IngredientItem("i5", "4 fresh Meyer lemons, juiced and zested", category = "Produce"),
                    IngredientItem("i6", "1 cup granulated sugar", category = "Baking & Pantry Grains"),
                    IngredientItem("i7", "4 egg yolks + 4 egg whites", category = "Cold Shelf")
                ),
                steps = listOf(
                    CookingStep(1, "Lemon Curd", "Whisk lemon juice, zest, sugar, and egg yolks over low heat until thickened.", 12),
                    CookingStep(2, "Meringue Peaks", "Whip egg whites to stiff peaks, spread over warm filling.", 8)
                ),
                secretNote = "Add a pinch of cream of tartar to stabilize the meringue.",
                bookmarked = false,
                isFavorite = true
            )

            val recipesObj = JSONObject().apply {
                put(demoUserId, JSONArray().apply {
                    put(serializeRecipeToJson(sampleCobbler))
                    put(serializeRecipeToJson(sampleLemonPie))
                })
            }

            prefs.edit()
                .putString(KEY_USERS_TABLE, usersObj.toString())
                .putString(KEY_PROFILES_TABLE, profilesObj.toString())
                .putString(KEY_RECIPES_TABLE, recipesObj.toString())
                .apply()

            Log.i(TAG, "Demo cloud database seeded with returning user: $demoEmail")
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding demo cloud database", e)
        }
    }
}
