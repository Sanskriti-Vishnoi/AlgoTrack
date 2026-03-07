package com.example.dsatracker.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun loginUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onResult(true, null)
            } else {
                onResult(false, task.exception?.message ?: "Login failed")
            }
        }
    }

    fun registerUser(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid ?: ""

                // Firestore mein user document create hone ka wait karo
                db.collection("users").document(userId)
                    .set(mapOf("completed" to listOf<String>()))
                    .addOnSuccessListener {
                        onResult(true, null) // Ab callback tabhi jayega jab DB ready ho
                    }
                    .addOnFailureListener { e ->
                        onResult(false, e.message)
                    }
            } else {
                onResult(false, task.exception?.message ?: "Registration failed")
            }
        }
    }

    fun logout() = auth.signOut()

    fun getCurrentUser() = auth.currentUser
}