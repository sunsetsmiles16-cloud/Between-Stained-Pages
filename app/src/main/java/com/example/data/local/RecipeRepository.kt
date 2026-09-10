package com.example.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Repository pattern implementation for local Recipe persistence.
 */
class RecipeRepository(private val recipeDao: RecipeDao) {

    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()

    fun getRecipeById(id: Long): Flow<Recipe?> = recipeDao.getRecipeById(id)

    suspend fun getRecipe(id: Long): Recipe? = recipeDao.getRecipe(id)

    suspend fun insert(recipe: Recipe): Long = recipeDao.insertRecipe(recipe)

    suspend fun insertAll(recipes: List<Recipe>): List<Long> = recipeDao.insertAll(recipes)

    suspend fun update(recipe: Recipe) = recipeDao.updateRecipe(recipe)

    suspend fun delete(recipe: Recipe) = recipeDao.deleteRecipe(recipe)

    suspend fun deleteById(id: Long) = recipeDao.deleteRecipeById(id)
}
