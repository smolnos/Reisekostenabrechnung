package com.example.reisekostenabrechnung

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.min

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: TravelExpenseViewModel = viewModel(
                factory = object : ViewModelProvider.AndroidViewModelFactory(application) {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(TravelExpenseViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return TravelExpenseViewModel(application) as T
                        }
                        return super.create(modelClass)
                    }
                }
            )
            MaterialTheme { TravelExpenseApp(vm) }
        }
    }
}

@Composable
fun TravelExpenseApp(viewModel: TravelExpenseViewModel) {
    val entries by viewModel.entries.collectAsState()
    val participantsList by viewModel.participants.collectAsState()

    var participantsInput by remember { mutableStateOf("") }
    var selectedParticipants by remember { mutableStateOf<List<String>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showSettlement by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, y, m, d -> date = "${d.toString().padStart(2, '0')}." +
                    "${(m + 1).toString().padStart(2, '0')}.$y" },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "🧳 Reisekostenabrechnung 🧳",
                    fontFamily = FontFamily.Cursive,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(20.dp))

            // Teilnehmerverwaltung
            Text("Teilnehmer verwalten:", style = MaterialTheme.typography.titleMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = participantsInput,
                    onValueChange = { participantsInput = it },
                    placeholder = { Text("Name eingeben") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    val newName = participantsInput.trim()
                    if (newName.isNotEmpty()) {
                        viewModel.addParticipant(newName)
                        participantsInput = ""
                    }
                }) { Text("Hinzufügen") }
            }
            Spacer(Modifier.height(12.dp))

            // Teilnehmerchips
            if (participantsList.isNotEmpty()) {
                Text("Teilnehmer auswählen:", style = MaterialTheme.typography.bodyMedium)
                ParticipantsChips(
                    participants = participantsList,
                    selected = selectedParticipants,
                    onSelect = { p ->
                        selectedParticipants = if (p in selectedParticipants) selectedParticipants - p
                        else selectedParticipants + p
                    },
                    onDelete = { p ->
                        viewModel.removeParticipant(p)
                        selectedParticipants = selectedParticipants - p
                    }
                )
                Spacer(Modifier.height(12.dp))
            }

            // Formular
            NameAutoCompleteField(name, onNameChange = { name = it }, allNames = participantsList)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                placeholder = { Text("€0,00") },
                leadingIcon = { Text("💶") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("TT.MM.JJJJ") },
                leadingIcon = { Text("📅") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "Datum wählen",
                        modifier = Modifier.clickable { datePickerDialog.show() }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Beschreibung") },
                leadingIcon = { Text("📝") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            Button(onClick = {
                amount.toDoubleOrNull()?.let { value ->
                    val parts = if (selectedParticipants.isEmpty() || selectedParticipants.size == participantsList.size)
                        participantsList.ifEmpty { listOf(name.trim()) } else selectedParticipants
                    viewModel.addEntry(
                        Entry(name.trim(), value, date, description.trim(), participants = parts)
                    )
                    name = ""; amount = ""; date = ""; description = ""
                    selectedParticipants = emptyList()
                }
            }) { Text("Hinzufügen") }

            Spacer(Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 420.dp)
            ) {
                itemsIndexed(entries) { idx, e ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${e.date} — ${e.name}: €${"%.2f".format(e.amount)} — ${e.description}")
                                Text("Teilnehmer: ${e.participants.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text(
                                "❌",
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clickable { viewModel.removeEntry(idx) }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(onClick = { showSettlement = !showSettlement }) {
                    Text(if (showSettlement) "Abrechnung ausblenden" else "Abrechnung anzeigen")
                }
            }

            if (showSettlement) {
                val settlements = computeSettlements(entries, participantsList)
                Spacer(Modifier.height(16.dp))
                Text("Ausgleichsvorschläge:", style = MaterialTheme.typography.titleMedium)
                settlements.forEach { s ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0EAD6))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(s, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

// Teilnehmerchips-Komponente
@Composable
fun ParticipantsChips(
    participants: List<String>,
    selected: List<String>,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        participants.forEach { person ->
            val isSelected = person in selected
            Box(
                modifier = Modifier
                    .background(
                        color = if (isSelected) Color(0xFFBB86FC) else Color(0xFFE0E0E0),
                        shape = MaterialTheme.shapes.small
                    )
                    .combinedClickable(
                        onClick = { onSelect(person) },
                        onLongClick = { onDelete(person) }
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = person, color = if (isSelected) Color.White else Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Löschen",
                        tint = if (isSelected) Color.White else Color.Black,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDelete(person) }
                    )
                }
            }
        }
    }
}

// Verbesserte Autocomplete
@Composable
fun NameAutoCompleteField(name: String, onNameChange: (String) -> Unit, allNames: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(name, allNames) {
        if (name.isBlank()) emptyList()
        else allNames.filter { it.contains(name, ignoreCase = true) }
    }
    Column {
        OutlinedTextField(
            value = name,
            onValueChange = {
                onNameChange(it)
                expanded = it.isNotBlank() && filtered.isNotEmpty()
            },
            placeholder = { Text("Name") },
            leadingIcon = { Text("👤") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        if (expanded) {
            Card(modifier = Modifier.fillMaxWidth().background(Color.White)) {
                Column {
                    filtered.forEach { suggestion ->
                        Text(
                            text = suggestion,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onNameChange(suggestion)
                                    expanded = false
                                }
                                .padding(8.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

// computeSettlements bleibt unverändert
fun computeSettlements(entries: List<Entry>, allNames: List<String>): List<String> {
    if (allNames.isEmpty()) return listOf("Keine Teilnehmer vorhanden.")
    val balances = mutableMapOf<String, Double>().apply { allNames.forEach { this[it] = 0.0 } }
    entries.forEach { e ->
        val parts = when {
            e.participants.isEmpty() -> allNames.ifEmpty { listOf(e.name) }
            e.participants.size == allNames.size -> allNames
            else -> e.participants
        }
        if (parts.isEmpty()) return@forEach
        val share = e.amount / parts.size
        balances[e.name] = (balances[e.name] ?: 0.0) + e.amount
        parts.forEach { p -> balances[p] = (balances[p] ?: 0.0) - share }
    }
    if (balances.values.all { abs(it) < 0.01 }) return listOf("Alles ausgeglichen!")
    data class Person(val name: String, var balance: Double)
    val debtors = mutableListOf<Person>()
    val creditors = mutableListOf<Person>()
    balances.forEach { (n, b) ->
        when {
            b < -0.01 -> debtors += Person(n, -b)
            b > 0.01 -> creditors += Person(n, b)
        }
    }
    debtors.sortByDescending { it.balance }
    creditors.sortByDescending { it.balance }
    val results = mutableListOf<String>()
    var i = 0; var j = 0
    while (i < debtors.size && j < creditors.size) {
        val d = debtors[i]; val c = creditors[j]; val amt = min(d.balance, c.balance)
        results += "${d.name} zahlt €${"%.2f".format(amt)} an ${c.name}"
        d.balance -= amt; c.balance -= amt
        if (d.balance < 0.01) i++; if (c.balance < 0.01) j++
    }
    return results
}
