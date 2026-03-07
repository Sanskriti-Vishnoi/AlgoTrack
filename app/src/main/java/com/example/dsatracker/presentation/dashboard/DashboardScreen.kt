package com.example.dsatracker.presentation.dashboard

import android.text.Layout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dsatracker.data.AuthRepository
import com.example.dsatracker.data.Category
import com.example.dsatracker.data.Sheet
import com.example.dsatracker.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController, viewModel: DashboardViewModel = viewModel()) {

    LaunchedEffect(Unit) {
        viewModel.fetchData()
    }

    val sheets by viewModel.allSheets
    var selectedSheetId by remember { mutableStateOf<String?>(null) }

    // --- FIX: Ab hum object nahi, sirf ID handle karenge ---
    var showDialog by remember { mutableStateOf(false) }
    var newSheetTitle by remember { mutableStateOf("") }
    var sheetToDelete by remember { mutableStateOf<Sheet?>(null) }

    // Phone ka back button handling
    if (selectedSheetId != null) {
        androidx.activity.compose.BackHandler {
            selectedSheetId = null
        }
    }

    // --- SMART NAVIGATION: Ab SheetDetailView hamesha fresh data uthayega ---
    if (selectedSheetId != null) {
        SheetDetailView(
            sheetId = selectedSheetId!!, // Pura object ki jagah sirf ID bhejo
            viewModel = viewModel,
            onBack = { selectedSheetId = null }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My DSA Sheets", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = {
                            AuthRepository.logout()
                            navController.navigate("login") {
                                popUpTo("dashboard") { inclusive = true }
                            }
                        }) { Icon(Icons.Default.ExitToApp, "Logout") }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) { Icon(Icons.Default.Add, "Add Sheet") }
            }
        ) { padding ->

            // --- Delete Confirmation Dialog ---
            sheetToDelete?.let { sheet ->
                AlertDialog(
                    onDismissRequest = { sheetToDelete = null },
                    title = { Text("Delete Entire Sheet?") },
                    text = { Text("Do you really want to delete '${sheet.title}'?") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.deleteSheet(sheet.id)
                            sheetToDelete = null
                        }) { Text("Yes, Delete", color = Color.Red) }
                    },
                    dismissButton = {
                        TextButton(onClick = { sheetToDelete = null }) { Text("Cancel") }
                    }
                )
            }

            // --- Naya Sheet Dialog ---
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Create New DSA Sheet") },
                    text = {
                        OutlinedTextField(
                            value = newSheetTitle,
                            onValueChange = { newSheetTitle = it },
                            label = { Text("Sheet Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newSheetTitle.isNotBlank()) {
                                viewModel.addNewSheet(newSheetTitle)
                                newSheetTitle = ""
                                showDialog = false
                            }
                        }) { Text("Create") }
                    }
                )
            }

            if (viewModel.isLoading.value) {
                Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                    items(sheets, key = { it.id }) { sheet -> // Key add karne se performance badhti hai
                        val totalProblems = sheet.categories.sumOf { it.problems.size }
                        val completedCount = sheet.categories.sumOf { cat ->
                            cat.problems.count { viewModel.completedProblems.value.contains(it.title) }
                        }
                        val sheetProgress = if (totalProblems > 0) (completedCount.toFloat() / totalProblems) else 0f

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { selectedSheetId = sheet.id }, // Sirf ID set karo
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(sheet.title, style = MaterialTheme.typography.titleLarge)
                                    Text("${sheet.categories.size} Topics | $totalProblems Problems", color = Color.Gray)

                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { sheetProgress },
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(
                                            CircleShape
                                        )
                                    )
                                    Text("${(sheetProgress * 100).toInt()}% Done", style = MaterialTheme.typography.labelSmall)
                                }

                                if (sheet.createdBy != "admin") {
                                    IconButton(onClick = { sheetToDelete = sheet }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(0.6f))
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