package com.example.presencia

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

// Data Models
data class SubjectData(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var attendanceData: MutableMap<String, Boolean> = mutableMapOf()
)

class StorageManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("PresenciaData", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveSubjects(subjects: List<SubjectData>) {
        val json = gson.toJson(subjects)
        prefs.edit().putString("subjects_list", json).apply()
    }

    fun getSubjects(): List<SubjectData> {
        val json = prefs.getString("subjects_list", null) ?: return emptyList()
        val type = object : TypeToken<List<SubjectData>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveTheme(isDarkTheme: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDarkTheme).apply()
    }

    fun getTheme(): Boolean {
        return prefs.getBoolean("is_dark_theme", false)
    }
}

class PresenciaViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = StorageManager(application)

    var subjects = mutableStateListOf<SubjectData>()
    var isDarkTheme by mutableStateOf(storage.getTheme())
    var selectAllMode by mutableStateOf(false)

    init {
        subjects.addAll(storage.getSubjects())
    }

    private fun saveData() {
        storage.saveSubjects(subjects)
    }

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
        storage.saveTheme(isDarkTheme)
    }

    fun addSubject(name: String) {
        subjects.add(SubjectData(name = name))
        saveData()
    }

    fun deleteSelectedSubjects() {
        if (selectAllMode) {
            subjects.clear()
            selectAllMode = false
            saveData()
        }
    }

    fun deleteSubject(id: String) {
        subjects.removeAll { it.id == id }
        saveData()
    }

    fun updateSubjectName(id: String, newName: String) {
        val index = subjects.indexOfFirst { it.id == id }
        if (index != -1) {
            subjects[index] = subjects[index].copy(name = newName)
            saveData()
        }
    }

    fun markAttendance(subjectId: String, date: LocalDate, isPresent: Boolean) {
        val index = subjects.indexOfFirst { it.id == subjectId }
        if (index != -1) {
            val updatedMap = subjects[index].attendanceData.toMutableMap()
            updatedMap[date.toString()] = isPresent
            subjects[index] = subjects[index].copy(attendanceData = updatedMap)
            saveData()
        }
    }

    fun removeAttendance(subjectId: String, date: LocalDate) {
        val index = subjects.indexOfFirst { it.id == subjectId }
        if (index != -1) {
            val updatedMap = subjects[index].attendanceData.toMutableMap()
            updatedMap.remove(date.toString())
            subjects[index] = subjects[index].copy(attendanceData = updatedMap)
            saveData()
        }
    }

    fun resetSubjectData(subjectId: String) {
        val index = subjects.indexOfFirst { it.id == subjectId }
        if (index != -1) {
            subjects[index] = subjects[index].copy(attendanceData = mutableMapOf())
            saveData()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PresenciaViewModel = viewModel()

            val colors = if (viewModel.isDarkTheme) {
                darkColorScheme(background = Color.Black, surface = Color.Black, onBackground = Color.White, onSurface = Color.White)
            } else {
                lightColorScheme(background = Color.White, surface = Color.White, onBackground = Color.Black, onSurface = Color.Black)
            }

            MaterialTheme(colorScheme = colors) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: PresenciaViewModel) {
    var selectedSubjectId by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp)),
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = { Text("Warning") },
            text = { Text("This will Permanently delete all Data") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteSelectedSubjects(); showDeleteConfirm = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp)),
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = { Text("Add Subject") },
            text = {
                OutlinedTextField(
                    value = newSubjectName,
                    onValueChange = { newSubjectName = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = MaterialTheme.colorScheme.onBackground,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.addSubject(newSubjectName); newSubjectName = ""; showAddDialog = false }) {
                    Text("Add", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (selectedSubjectId == null) {
                TopAppBar(
                    title = { Text("Presencia", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { viewModel.toggleTheme() }) {
                            Icon(
                                imageVector = if (viewModel.isDarkTheme) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                contentDescription = "Theme",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        IconButton(onClick = { viewModel.selectAllMode = !viewModel.selectAllMode }) {
                            Icon(
                                imageVector = if (viewModel.selectAllMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Select All",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        if (viewModel.selectAllMode) {
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete All",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        } else {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.AddCircle,
                                    contentDescription = "Add Subject",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (selectedSubjectId == null) {
            HomeScreen(viewModel, paddingValues) { selectedSubjectId = it }
        } else {
            val subject = viewModel.subjects.find { it.id == selectedSubjectId }
            if (subject != null) {
                BackHandler { selectedSubjectId = null }
                CalendarScreen(subject, viewModel, paddingValues) { selectedSubjectId = null }
            } else {
                selectedSubjectId = null
            }
        }
    }
}

@Composable
fun HomeScreen(viewModel: PresenciaViewModel, paddingValues: PaddingValues, onSubjectClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        viewModel.subjects.forEachIndexed { index, subject ->
            SubjectItem(index + 1, subject, viewModel, onSubjectClick)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SubjectItem(index: Int, subject: SubjectData, viewModel: PresenciaViewModel, onSubjectClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var editDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(subject.name) }

    val total = subject.attendanceData.size
    val present = subject.attendanceData.values.count { it }
    val percentage = if (total > 0) (present.toFloat() / total) * 100 else 0f

    if (editDialog) {
        AlertDialog(
            onDismissRequest = { editDialog = false },
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp)),
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            confirmButton = {
                TextButton(onClick = { viewModel.updateSubjectName(subject.id, editName); editDialog = false }) {
                    Text("Save", color = MaterialTheme.colorScheme.onBackground)
                }
            },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                        cursorColor = MaterialTheme.colorScheme.onBackground,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        )
    }

    Box(modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.onBackground).clickable { onSubjectClick(subject.id) }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${index}. ${subject.name}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (total > 0) {
                    Text("$present/$total", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }

            // Donut Chart
            val chartColor = MaterialTheme.colorScheme.onBackground
            val isDark = MaterialTheme.colorScheme.background == Color.Black
            val trackColor = if (isDark) Color.DarkGray else Color(0xFFE0E0E0)

            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp).padding(end = 8.dp)) {
                Canvas(modifier = Modifier.size(40.dp)) {
                    drawArc(color = trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 8f, cap = StrokeCap.Round))
                    if (total > 0) {
                        drawArc(color = chartColor, startAngle = -90f, sweepAngle = (percentage / 100) * 360, useCenter = false, style = Stroke(width = 8f, cap = StrokeCap.Round))
                    }
                }
                Text("${percentage.toInt()}%", fontSize = 10.sp)
            }

            // Triple Dot Menu
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, "More Options", tint = MaterialTheme.colorScheme.onBackground)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.background).border(1.dp, MaterialTheme.colorScheme.onBackground)
                ) {
                    DropdownMenuItem(text = { Text("Edit", color = MaterialTheme.colorScheme.onBackground) }, onClick = { expanded = false; editDialog = true })
                    DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.onBackground) }, onClick = { expanded = false; viewModel.deleteSubject(subject.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(subject: SubjectData, viewModel: PresenciaViewModel, paddingValues: PaddingValues, onBack: () -> Unit) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val minMonth = YearMonth.of(2026, 1)
    val maxMonth = YearMonth.of(2077, 12)

    val total = subject.attendanceData.size
    val present = subject.attendanceData.values.count { it }
    val absent = total - present
    val percentage = if (total > 0) (present.toFloat() / total) * 100 else 0f

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp)),
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = { Text("Warning") },
            text = { Text("This will Reset all Data") },
            confirmButton = {
                TextButton(onClick = { viewModel.resetSubjectData(subject.id); showResetConfirm = false }) {
                    Text("Reset", color = MaterialTheme.colorScheme.onBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        )
    }

    if (selectedDate != null) {
        val dateStr = selectedDate.toString()
        val isAlreadyMarked = subject.attendanceData.containsKey(dateStr)

        AlertDialog(
            onDismissRequest = { selectedDate = null },
            modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(28.dp)),
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            title = { Text("Mark Attendance") },
            text = { Text("Date: $dateStr") },
            confirmButton = {
                TextButton(onClick = { viewModel.markAttendance(subject.id, selectedDate!!, true); selectedDate = null }) {
                    Text("Present", color = Color(0xFF4CAF50))
                }
            },
            dismissButton = {
                Row {
                    if (isAlreadyMarked) {
                        TextButton(onClick = { viewModel.removeAttendance(subject.id, selectedDate!!); selectedDate = null }) {
                            Text("Remove", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    TextButton(onClick = { viewModel.markAttendance(subject.id, selectedDate!!, false); selectedDate = null }) {
                        Text("Absent", color = Color(0xFFF44336))
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {

        // Back to Home Button Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
                .padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Home",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Calendar Header
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (currentMonth.isAfter(minMonth)) currentMonth = currentMonth.minusMonths(1) }) {
                Text("<", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            Text("${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }}, ${currentMonth.year}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            IconButton(onClick = { if (currentMonth.isBefore(maxMonth)) currentMonth = currentMonth.plusMonths(1) }) {
                Text(">", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }

        // Days of week header
        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // Calendar Grid
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
        val columns = 7
        val rows = (daysInMonth + firstDayOfWeek + columns - 1) / columns

        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            var dayCounter = 1
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (c in 0 until columns) {
                        if (r == 0 && c < firstDayOfWeek || dayCounter > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = currentMonth.atDay(dayCounter)
                            val isAttended = subject.attendanceData[date.toString()]
                            val bgColor = when (isAttended) {
                                true -> Color(0xFF4CAF50)
                                false -> Color(0xFFF44336)
                                null -> Color.Transparent
                            }
                            val textColor = if (isAttended != null) Color.White else MaterialTheme.colorScheme.onBackground
                            val isToday = date == LocalDate.now()
                            val borderModifier = if (isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(4.dp)) else Modifier.border(1.dp, Color.Gray, RoundedCornerShape(4.dp))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .background(bgColor, RoundedCornerShape(4.dp))
                                    .then(borderModifier)
                                    .clickable { selectedDate = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$dayCounter",
                                    color = textColor,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }

        // Stats Boxes
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.onBackground).padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Present:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("$present", color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.onBackground).padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Absent:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("$absent", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.onBackground).padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("$total", color = MaterialTheme.colorScheme.onBackground)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f).border(1.dp, MaterialTheme.colorScheme.onBackground).padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Percentage:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Text("${percentage.toInt()}%", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        // Reset Button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            IconButton(onClick = { showResetConfirm = true }, modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(8.dp)).padding(8.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = MaterialTheme.colorScheme.onBackground)
            }
        }
    }
}