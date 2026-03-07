package com.example.dsatracker.presentation.dashboard

import android.graphics.drawable.Icon
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dsatracker.data.Sheet
import com.example.dsatracker.viewmodel.DashboardViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.dsatracker.data.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetDetailView(
    sheetId: String,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    // 🔥 LIVE STATE: ViewModel se hamesha fresh data observe karega
    val allSheets by viewModel.allSheets
    val sheet = allSheets.find { it.id == sheetId }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newTopicTitle by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Agar humne category select kar rakhi hai, toh wahi screen dikhao
    if (selectedCategoryId != null && sheet != null) {
        CategoryScreen(
            sheetId = sheet.id,
            categoryId = selectedCategoryId!!,
            viewModel = viewModel,
            onBack = { selectedCategoryId = null }
        )
    } else {
        sheet?.let { currentSheet ->
            // 🔥 SMART SEARCH: Title aur Tags dono dhoondega
            val filteredCategories = currentSheet.categories.filter { category ->
                val matchesTitle = category.title.contains(searchQuery, ignoreCase = true)
                val matchesTags = category.problems.any { problem ->
                    problem.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
                }
                matchesTitle || matchesTags
            }

            Scaffold(
                topBar = {
                    Column {
                        TopAppBar(
                            title = { Text(currentSheet.title, fontWeight = FontWeight.Bold) },
                            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }
                        )
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search topics or tags (e.g. #DP)") },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, "Clear Search")
                                    }
                                }
                            }
                        )
                    }
                },
                floatingActionButton = {
                    if (currentSheet.createdBy != "admin") {
                        FloatingActionButton(
                            onClick = { showAddDialog = true },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) { Icon(Icons.Default.Add, "Add Topic") }
                    }
                }
            ) { padding ->

                // --- Dialogs (Same Logic) ---
                if (showAddDialog) {
                    AlertDialog(
                        onDismissRequest = { showAddDialog = false },
                        title = { Text("Add New Topic") },
                        text = {
                            OutlinedTextField(
                                value = newTopicTitle,
                                onValueChange = { newTopicTitle = it },
                                label = { Text("Topic Name") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (newTopicTitle.isNotBlank()) {
                                    viewModel.addCategoryToSheet(currentSheet.id, newTopicTitle)
                                    newTopicTitle = ""
                                    showAddDialog = false
                                }
                            }) { Text("Add") }
                        }
                    )
                }

                categoryToDelete?.let { category ->
                    AlertDialog(
                        onDismissRequest = { categoryToDelete = null },
                        title = { Text("Delete Topic?") },
                        text = { Text("Delete '${category.title}'?") },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deleteCategoryFromSheet(currentSheet.id, category.id)
                                categoryToDelete = null
                            }) { Text("Yes", color = Color.Red) }
                        },
                        dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") } }
                    )
                }

                Column(Modifier.padding(padding)) {
                    // Overall Progress
                    val totalProbs = currentSheet.categories.sumOf { it.problems.size }
                    val doneProbs = currentSheet.categories.sumOf { cat ->
                        cat.problems.count { viewModel.completedProblems.value.contains(it.title) }
                    }
                    val sheetProgress = if (totalProbs > 0) doneProbs.toFloat() / totalProbs else 0f

                    LinearProgressIndicator(
                        progress = { sheetProgress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (filteredCategories.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(if (searchQuery.isEmpty()) "No topics yet." else "No matches for '$searchQuery'")
                        }
                    } else {
                        LazyColumn(Modifier.padding(horizontal = 16.dp)) {
                            items(filteredCategories) { category ->
                                val progress = viewModel.getProgress(category)

                                // 🔥 LIVE METADATA COUNT (Tags & Links)
                                val totalTags = category.problems.sumOf { it.tags.size }
                                val totalLinks = category.problems.sumOf { it.extraLinks.size }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .clickable { selectedCategoryId = category.id },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(category.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                                            // Badges UI
                                            if (totalTags > 0 || totalLinks > 0) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (totalTags > 0) {
                                                        Icon(Icons.Default.Info, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Text(" $totalTags Tags ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                    if (totalLinks > 0) {
                                                        Spacer(Modifier.width(8.dp))
                                                        Icon(Icons.Default.Share, null, Modifier.size(12.dp), tint = Color.Gray)
                                                        Text(" $totalLinks Resources", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))
                                            LinearProgressIndicator(
                                                progress = { progress / 100f },
                                                modifier = Modifier.width(100.dp).height(6.dp).clip(CircleShape)
                                            )
                                        }

                                        Text("$progress%", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)

                                        if (currentSheet.createdBy != "admin") {
                                            IconButton(onClick = { categoryToDelete = category }) {
                                                Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.6f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

