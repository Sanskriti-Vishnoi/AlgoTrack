package com.example.dsatracker.data

data class Problem(
    val id: Int = 0,
    val title: String = "",
    val url: String = "",
    val notes: String = "",
    val timeTaken: String = "",
    val tags: List<String> = emptyList(),      // #Recursion, #DP etc.
    val extraLinks: List<String> = emptyList() // YouTube, Article URLs
)

data class Category(
    val id: String = "",
    val title: String = "",
    val userId: String = "",
    val problems: List<Problem> = emptyList()
)

data class StriverSheet(
    val categories: List<Category> = emptyList()
)

data class Sheet(
    val id: String = "",
    val title: String = "",
    val createdBy: String = "admin", // 'admin' for default, 'userId' for custom
    val categories: List<Category> = emptyList()
)