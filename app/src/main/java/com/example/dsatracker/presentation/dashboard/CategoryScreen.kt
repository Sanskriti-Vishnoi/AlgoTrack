package com.example.dsatracker.presentation.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dsatracker.data.Category
import com.example.dsatracker.viewmodel.DashboardViewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.example.dsatracker.data.Problem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CategoryScreen(
    sheetId: String,
    categoryId: String,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val completedSet by viewModel.completedProblems

    val allSheets by viewModel.allSheets
    val currentSheet = allSheets.find { it.id == sheetId }
    val category = currentSheet?.categories?.find { it.id == categoryId }
    val problems = category?.problems ?: emptyList()

    var searchQuery by remember { mutableStateOf("") }
    var expandedProblemId by remember { mutableStateOf<Int?>(null) }

    val filteredProblems = problems.filter { it.title.contains(searchQuery, ignoreCase = true) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(category?.title ?: "Category", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                    // 🔥 DELETE CATEGORY (Sirf Custom Sheets ke liye)
                    actions = {
                        if (sheetId != "striver_sde") {
                            IconButton(onClick = {
                                viewModel.deleteCategoryFromSheet(sheetId, categoryId)
                                onBack()
                            }) {
                                Icon(Icons.Default.Delete, "Delete Topic", tint = Color.Red)
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search problems or tags...") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val progress = if (category != null) viewModel.getProgress(category) else 0
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filteredProblems, key = { it.id }) { problem ->
                    val isDone = completedSet.contains(problem.title)
                    val isExpanded = expandedProblemId == problem.id

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(if (isExpanded) 4.dp else 1.dp)
                    ) {
                        Column {
                            ListItem(
                                headlineContent = {
                                    Column {
                                        Text(
                                            text = problem.title,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (isDone) TextDecoration.LineThrough else null,
                                            color = if (isDone) Color.Gray else Color.Unspecified
                                        )
                                        // 🏷️ TAGS VISIBILITY (Band card mein chamkenge)
                                        if (problem.tags.isNotEmpty() && !isExpanded) {
                                            FlowRow(modifier = Modifier.padding(top = 4.dp)) {
                                                problem.tags.forEach { tag ->
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                                        shape = RoundedCornerShape(4.dp),
                                                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                                                    ) {
                                                        Text(tag, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(4.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                supportingContent = {
                                    Column {
                                        Text("Solve Main Problem ↗", color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clickable {
                                                if (problem.url.isNotBlank()) {
                                                    val url = if (!problem.url.startsWith("http")) "https://${problem.url}" else problem.url
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                }
                                            }
                                        )

                                        // 🔗 SAVED RESOURCES (Band card mein clickable links)
                                        if (problem.extraLinks.isNotEmpty() && !isExpanded) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            problem.extraLinks.forEach { link ->
                                                Text(
                                                    text = "🔗 Resource: ${link.take(30)}...",
                                                    color = Color.Blue,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(vertical = 2.dp).clickable {
                                                        try {
                                                            val finalUrl = if (!link.startsWith("http")) "https://$link" else link
                                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl)))
                                                        } catch (e: Exception) { }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // 🔥 DELETE PROBLEM (Sirf Custom Sheets mein)
                                        if (sheetId != "striver_sde") {
                                            IconButton(onClick = { viewModel.deleteProblemFromSheet(sheetId, categoryId, problem.id) }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(0.6f))
                                            }
                                        }
                                        IconButton(onClick = { expandedProblemId = if (isExpanded) null else problem.id }) {
                                            Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.Edit, "Edit")
                                        }
                                        Checkbox(checked = isDone, onCheckedChange = { viewModel.toggleProblem(problem.title) })
                                    }
                                }
                            )



                            if (isExpanded) {
                                // --- EXPANDED LOGIC ---
                                var seconds by remember { mutableIntStateOf(0) }
                                var isTimerRunning by remember { mutableStateOf(false) }
                                LaunchedEffect(isTimerRunning) {
                                    if (isTimerRunning) {
                                        while (isTimerRunning) {
                                            kotlinx.coroutines.delay(1000)
                                            seconds++
                                        }
                                    }
                                }
                                val displayTime = String.format("%02d:%02d", seconds / 60, seconds % 60)

                                var tempNotes by remember(problem.notes) { mutableStateOf(problem.notes) }
                                var tagInput by remember { mutableStateOf("") }
                                var linkInput by remember { mutableStateOf("") }

                                // 🔥 SYNC LOGIC: Jab save ho jaye toh naya data yahan load ho
                                val currentTags = remember { mutableStateListOf<String>() }
                                val currentLinks = remember { mutableStateListOf<String>() }

                                LaunchedEffect(problem) {
                                    currentTags.clear()
                                    currentTags.addAll(problem.tags)
                                    currentLinks.clear()
                                    currentLinks.addAll(problem.extraLinks)
                                }

                                Column(modifier = Modifier.padding(16.dp)) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                                    // ⏱️ TIMER
                                    Surface(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), shape = RoundedCornerShape(8.dp)) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(displayTime, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                            Row {
                                                IconButton(onClick = { isTimerRunning = !isTimerRunning }) { Icon(if (isTimerRunning) Icons.Default.Close else Icons.Default.PlayArrow, null, tint = if (isTimerRunning) Color.Red else Color.Green) }
                                                IconButton(onClick = { isTimerRunning = false; seconds = 0 }) { Icon(Icons.Default.Refresh, null) }
                                            }
                                        }
                                    }

                                    // 🏷️ TAGS SECTION
                                    Text("Logic Tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    FlowRow(modifier = Modifier.padding(vertical = 4.dp)) {
                                        currentTags.forEach { tag ->
                                            InputChip(
                                                selected = true,
                                                onClick = { },
                                                label = { Text(tag) },
                                                trailingIcon = {
                                                    Icon(Icons.Default.Close, null, Modifier.size(14.dp).clickable { currentTags.remove(tag) })
                                                },
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                    }
                                    OutlinedTextField(
                                        value = tagInput,
                                        onValueChange = { tagInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Add Tag (e.g. DP)") },
                                        trailingIcon = {
                                            IconButton(onClick = {
                                                if(tagInput.isNotBlank()){
                                                    val formatted = if(tagInput.startsWith("#")) tagInput else "#$tagInput"
                                                    currentTags.add(formatted)
                                                    tagInput = ""
                                                }
                                            }) { Icon(Icons.Default.AddCircle, null) }
                                        }
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    // 🔗 LINKS SECTION
                                    Text("Resources (Click to Open)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    currentLinks.forEach { link ->
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                            Text(
                                                text = "🔗 ${link.take(35)}...",
                                                color = Color.Blue,
                                                modifier = Modifier.weight(1f).clickable {
                                                    val url = if (!link.startsWith("http")) "https://$link" else link
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(onClick = { currentLinks.remove(link) }) {
                                                Icon(Icons.Default.Delete, null, Modifier.size(18.dp), tint = Color.Red)
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = linkInput,
                                        onValueChange = { linkInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Paste URL") },
                                        trailingIcon = {
                                            IconButton(onClick = { if(linkInput.isNotBlank()){ currentLinks.add(linkInput); linkInput="" } }) { Icon(Icons.Default.Create, null) }
                                        }
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    // 📝 NOTES
                                    OutlinedTextField(value = tempNotes, onValueChange = { tempNotes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), maxLines = 5)

                                    // 🔘 ACTIONS
                                    Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        TextButton(onClick = {
                                            // 🔥 Master Clear
                                            viewModel.updateProblemAllData(sheetId, categoryId, problem.id, "", emptyList(), emptyList())
                                            expandedProblemId = null
                                        }) { Text("Clear All", color = Color.Red) }

                                        Row {
                                            TextButton(onClick = { expandedProblemId = null }) { Text("Cancel") }
                                            Button(onClick = {
                                                val timeSuffix = if (seconds > 0) "\n(Time: $displayTime)" else ""
                                                // 🔥 Master Save: Tags aur Links dono ko list mein convert karke bhejo
                                                viewModel.updateProblemAllData(
                                                    sheetId, categoryId, problem.id,
                                                    tempNotes + timeSuffix,
                                                    currentTags.toList(),
                                                    currentLinks.toList()
                                                )
                                                expandedProblemId = null
                                            }) { Text("Save All") }
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

