package com.example.dsatracker.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.dsatracker.data.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var completedProblems = mutableStateOf<Set<String>>(emptySet())
    var isLoading = mutableStateOf(true)
    var allSheets = mutableStateOf<List<Sheet>>(emptyList())

    init { fetchData() }

    fun fetchData() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val userId = auth.currentUser?.uid
                val jsonSheet = JsonReader.loadSheet(getApplication())

                // 1. Fetch Static Meta (Notes, Tags, Links) for Striver Sheet
                val staticMetaMap = mutableMapOf<String, Triple<String, List<String>, List<String>>>()
                if (userId != null) {
                    val metaSnapshot = db.collection("users").document(userId)
                        .collection("staticMeta").get().await()

                    for (doc in metaSnapshot.documents) {
                        val pId = doc.id.split("_").last()
                        val notes = doc.getString("notes") ?: ""
                        val tags = doc.get("tags") as? List<String> ?: emptyList()
                        val links = doc.get("extraLinks") as? List<String> ?: emptyList()
                        staticMetaMap[pId] = Triple(notes, tags, links)
                    }
                }

                // 2. Inject Meta into Striver Data
                val categoriesWithMeta = jsonSheet.categories.map { category ->
                    val problemsWithMeta = category.problems.map { problem ->
                        val saved = staticMetaMap[problem.id.toString()]
                        problem.copy(
                            notes = saved?.first ?: "",
                            tags = saved?.second ?: emptyList(),
                            extraLinks = saved?.third ?: emptyList()
                        )
                    }
                    category.copy(problems = problemsWithMeta)
                }

                val striverSheet = Sheet(
                    id = "striver_sde",
                    title = "Striver's SDE Sheet",
                    createdBy = "admin",
                    categories = categoriesWithMeta
                )

                val sheetsList = mutableListOf(striverSheet)
                if (userId != null) {
                    val customSnapshot = db.collection("users").document(userId)
                        .collection("customSheets").get().await()
                    sheetsList.addAll(customSnapshot.toObjects(Sheet::class.java))

                    val progressSnapshot = db.collection("users").document(userId).get().await()
                    val list = progressSnapshot.get("completed") as? List<String> ?: emptyList()
                    completedProblems.value = list.toSet()
                }

                allSheets.value = sheetsList
            } catch (e: Exception) {
                android.util.Log.e("FETCH_ERROR", e.message.toString())
            } finally {
                isLoading.value = false
            }
        }
    }

    // 🔥 MASTER SAVE FUNCTION (Notes, Tags, Links ek saath save honge)
    fun updateProblemAllData(sheetId: String, categoryId: String, problemId: Int, notes: String, tags: List<String>, links: List<String>) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                if (sheetId == "striver_sde") {
                    val metaId = "${userId}_${problemId}"
                    db.collection("users").document(userId).collection("staticMeta").document(metaId)
                        .set(mapOf(
                            "notes" to notes,
                            "tags" to tags,
                            "extraLinks" to links
                        ), SetOptions.merge()).await()
                } else {
                    val sheetRef = db.collection("users").document(userId).collection("customSheets").document(sheetId)
                    db.runTransaction { transaction ->
                        val sheet = transaction.get(sheetRef).toObject(Sheet::class.java)
                        val updatedCats = sheet?.categories?.map { cat ->
                            if (cat.id == categoryId) {
                                val updatedProbs = cat.problems.map { prob ->
                                    if (prob.id == problemId) {
                                        prob.copy(notes = notes, tags = tags, extraLinks = links)
                                    } else prob
                                }
                                cat.copy(problems = updatedProbs)
                            } else cat
                        } ?: emptyList()
                        transaction.update(sheetRef, "categories", updatedCats)
                    }.await()
                }
                fetchData() // Refresh everything together
            } catch (e: Exception) {
                android.util.Log.e("UPDATE_ALL_ERROR", e.message.toString())
            }
        }
    }

    // --- SHEET MANAGEMENT (Unchanged) ---
    fun addNewSheet(title: String) {
        val userId = auth.currentUser?.uid ?: return
        val newSheetId = db.collection("users").document(userId).collection("customSheets").document().id
        val newSheet = Sheet(id = newSheetId, title = title, createdBy = userId, categories = emptyList())
        db.collection("users").document(userId).collection("customSheets").document(newSheetId).set(newSheet).addOnSuccessListener { fetchData() }
    }

    fun deleteSheet(sheetId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("customSheets").document(sheetId).delete().addOnSuccessListener { fetchData() }
    }

    // --- CATEGORY MANAGEMENT (Unchanged) ---
    fun addCategoryToSheet(sheetId: String, categoryTitle: String) {
        val userId = auth.currentUser?.uid ?: return
        val sheetRef = db.collection("users").document(userId).collection("customSheets").document(sheetId)
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    val currentSheet = transaction.get(sheetRef).toObject(Sheet::class.java)
                    val newCat = Category(id = UUID.randomUUID().toString(), title = categoryTitle, userId = userId, problems = emptyList())
                    val updatedCats = currentSheet?.categories?.toMutableList() ?: mutableListOf()
                    updatedCats.add(newCat)
                    transaction.update(sheetRef, "categories", updatedCats)
                }.await()
                fetchData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteCategoryFromSheet(sheetId: String, categoryId: String) {
        val userId = auth.currentUser?.uid ?: return
        val sheetRef = db.collection("users").document(userId).collection("customSheets").document(sheetId)
        viewModelScope.launch {
            db.runTransaction { transaction ->
                val sheet = transaction.get(sheetRef).toObject(Sheet::class.java)
                val newList = sheet?.categories?.filter { it.id != categoryId } ?: emptyList()
                transaction.update(sheetRef, "categories", newList)
            }.await()
            fetchData()
        }
    }

    // --- PROBLEM MANAGEMENT (Unchanged) ---
    fun addProblemToCustomSheet(sheetId: String, categoryId: String, title: String, url: String) {
        val userId = auth.currentUser?.uid ?: return
        val sheetRef = db.collection("users").document(userId).collection("customSheets").document(sheetId)
        val newProblem = Problem(id = (1000..9999).random(), title = title, url = url)
        viewModelScope.launch {
            try {
                db.runTransaction { transaction ->
                    val sheet = transaction.get(sheetRef).toObject(Sheet::class.java)
                    val updatedCats = sheet?.categories?.map { cat ->
                        if (cat.id == categoryId) cat.copy(problems = cat.problems + newProblem) else cat
                    } ?: emptyList()
                    transaction.update(sheetRef, "categories", updatedCats)
                }.await()
                fetchData()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteProblemFromSheet(sheetId: String, categoryId: String, problemId: Int) {
        val userId = auth.currentUser?.uid ?: return
        val sheetRef = db.collection("users").document(userId).collection("customSheets").document(sheetId)
        viewModelScope.launch {
            db.runTransaction { transaction ->
                val sheet = transaction.get(sheetRef).toObject(Sheet::class.java)
                val updatedCats = sheet?.categories?.map { cat ->
                    if (cat.id == categoryId) cat.copy(problems = cat.problems.filter { it.id != problemId }) else cat
                } ?: emptyList()
                transaction.update(sheetRef, "categories", updatedCats)
            }.await()
            fetchData()
        }
    }

    fun toggleProblem(title: String) {
        val newList = completedProblems.value.toMutableSet()
        if (newList.contains(title)) newList.remove(title) else newList.add(title)
        completedProblems.value = newList
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).set(mapOf("completed" to newList.toList()), SetOptions.merge())
    }

    fun getProgress(category: Category): Int {
        if (category.problems.isEmpty()) return 0
        val done = category.problems.count { completedProblems.value.contains(it.title) }
        return (done * 100 / category.problems.size)
    }
}