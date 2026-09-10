package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.Recipe
import com.example.data.local.RecipeDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import com.example.ui.screens.RecipeCardWithImagePreview
import com.example.ui.theme.HeirloomKitchenTheme
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private lateinit var db: AppDatabase
  private lateinit var recipeDao: RecipeDao

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    recipeDao = db.recipeDao()
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Between Stained Pages", appName)
  }

  @Test
  fun `verify core navigation routes`() {
    assertEquals("recipes", com.example.ui.components.Screen.Recipes.route)
    assertEquals("pantry", com.example.ui.components.Screen.Pantry.route)
    assertEquals("card_scanner", com.example.ui.components.Screen.CardScanner.route)
    assertEquals("grocery", com.example.ui.components.Screen.Grocery.route)
    assertEquals("cookbooks", com.example.ui.components.Screen.Cookbooks.route)
  }

  @Test
  fun `insert and retrieve recipe in room database`() = runTest {
    val testRecipe = Recipe(
      title = "Grandma's Butter Biscuits",
      ingredients = listOf("2 cups all-purpose flour", "1 tbsp baking powder", "1/2 cup cold butter", "3/4 cup milk"),
      instructions = "Cut cold butter into dry flour mix until pea-sized. Fold milk gently, shape and bake at 425F for 12 mins.",
      handwrittenImagePath = "/data/user/0/com.example/files/cards/grandma_biscuits_1952.jpg"
    )

    val id = recipeDao.insertRecipe(testRecipe)
    assertTrue("Generated ID should be positive", id > 0)

    val allRecipes = recipeDao.getAllRecipes().first()
    assertEquals(1, allRecipes.size)

    val saved = allRecipes.first()
    assertEquals("Grandma's Butter Biscuits", saved.title)
    assertEquals(4, saved.ingredients.size)
    assertTrue(saved.ingredients.contains("1/2 cup cold butter"))
    assertEquals(saved.ingredients, saved.ingredientsList)
    assertEquals("Cut cold butter into dry flour mix until pea-sized. Fold milk gently, shape and bake at 425F for 12 mins.", saved.instructions)
    assertEquals("/data/user/0/com.example/files/cards/grandma_biscuits_1952.jpg", saved.handwrittenImagePath)
    assertEquals(saved.handwrittenImagePath, saved.imagePath)

    val queried = recipeDao.getRecipe(id)
    assertNotNull(queried)
    assertEquals("Grandma's Butter Biscuits", queried?.title)
  }

  @Test
  fun `recipe search filter matches by title and ingredients`() {
    // Seed test recipes into the repository state flow to make test independent of authentication
    val testRecipe1 = com.example.data.Recipe(
        id = "rec-test-1",
        userId = "user_test",
        title = "Grandma's Apple Cinnamon Cobbler",
        source = "Test Source",
        sourceType = com.example.data.RecipeSourceType.FAMILY_NOTE,
        description = "Tasty cobbler",
        imageUrl = "",
        prepTime = "10m",
        cookTime = "30m",
        servings = 4,
        difficulty = "Easy",
        binderCategory = "Bakes",
        pantryStatusText = "",
        allIngredientsInPantry = true,
        ingredients = listOf(
            com.example.data.IngredientItem("i1", "2 cups flour", category = "Pantry")
        ),
        steps = emptyList()
    )
    com.example.data.HeirloomRepository.addRecipe(testRecipe1)

    val recipes = com.example.data.HeirloomRepository.recipes.value
    val flourQuery = "flour"
    val matchingByIngredient = recipes.filter { r ->
      r.title.contains(flourQuery, ignoreCase = true) ||
      r.ingredients.any { it.name.contains(flourQuery, ignoreCase = true) }
    }
    assertTrue("Should find recipes containing flour in ingredients", matchingByIngredient.isNotEmpty())

    val cobblerQuery = "Cobbler"
    val matchingByTitle = recipes.filter { r ->
      r.title.contains(cobblerQuery, ignoreCase = true) ||
      r.ingredients.any { it.name.contains(cobblerQuery, ignoreCase = true) }
    }
    assertTrue("Should find Grandma's Apple Cinnamon Cobbler by title", matchingByTitle.any { it.title.contains("Cobbler") })
  }

  @Test
  fun `RecipeCardWithImagePreview displays title and preview correctly`() {
    composeTestRule.setContent {
      HeirloomKitchenTheme {
        RecipeCardWithImagePreview(
          title = "Aunt Clara's Lemon Meringue",
          imagePath = "/data/user/0/com.example/files/recipes/lemon_pie.jpg",
          subtitle = "Archived handwritten index card"
        )
      }
    }

    composeTestRule.onNodeWithTag("recipe_preview_title").assertIsDisplayed()
    composeTestRule.onNodeWithTag("recipe_preview_title").assertTextEquals("Aunt Clara's Lemon Meringue")
    composeTestRule.onNodeWithTag("recipe_preview_image").assertIsDisplayed()
  }

  @Test
  fun `verify onboarding completion updates user profile and saves locally`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.HeirloomRepository.completeOnboarding(
      context = context,
      name = "Eleanor Vance",
      defaultServings = 6,
      dietaryPreferences = listOf("Vegetarian", "Dairy-Free")
    )

    val profile = com.example.data.HeirloomRepository.userProfile.value
    assertEquals("Eleanor Vance", profile.name)
    assertEquals(6, profile.defaultServings)
    assertEquals(listOf("Vegetarian", "Dairy-Free"), profile.dietaryPreferences)
    assertTrue("Onboarding should be marked completed", profile.isOnboardingCompleted)
  }
}
