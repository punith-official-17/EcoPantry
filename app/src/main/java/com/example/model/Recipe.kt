package com.example.model

data class Recipe(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val matchedExpiringIngredients: List<String> = emptyList(),
    val otherIngredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val prepTimeMinutes: Int = 15,
    val cookTimeMinutes: Int = 20,
    val difficulty: String = "Easy", // Easy, Medium, Hard
    val servings: Int = 2,
    val calories: Int = 350,
    val wasteSavedScoreKg: Double = 0.4,
    val tags: List<String> = listOf("Zero Waste", "Quick")
)
