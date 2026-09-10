package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Room Database entity representing a culinary recipe with handwritten archive card data.
 */
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: String = "",
    val title: String,
    val ingredients: List<String> = emptyList(),
    val instructions: String = "",
    @ColumnInfo(name = "handwritten_image_path")
    val handwrittenImagePath: String? = null
) {
    @get:Ignore
    val ingredientsList: List<String>
        get() = ingredients

    @get:Ignore
    val imagePath: String?
        get() = handwrittenImagePath
}

typealias RecipeEntity = Recipe
