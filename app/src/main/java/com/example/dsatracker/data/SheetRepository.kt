package com.example.dsatracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object SheetRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun addCategory(title: String) {
        val userId = auth.currentUser?.uid ?: return
        val categoryId = db.collection("users").document(userId).collection("categories").document().id

        // FIXED: problems = emptyList() add karna zaroori hai model ke hisaab se
        val category = Category(
            id = categoryId,
            title = title,
            userId = userId,
            problems = emptyList()
        )

        db.collection("users").document(userId)
            .collection("categories").document(categoryId)
            .set(category)
    }

    // Iski zaroorat aapko CategoryScreen mein padegi
    fun addProblemToCategory(categoryId: String, problem: Problem) {
        val userId = auth.currentUser?.uid ?: return
        val categoryRef = db.collection("users").document(userId)
            .collection("categories").document(categoryId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(categoryRef)
            val currentCategory = snapshot.toObject(Category::class.java)
            val updatedProblems = currentCategory?.problems?.toMutableList() ?: mutableListOf()
            updatedProblems.add(problem)
            transaction.update(categoryRef, "problems", updatedProblems)
        }
    }
}