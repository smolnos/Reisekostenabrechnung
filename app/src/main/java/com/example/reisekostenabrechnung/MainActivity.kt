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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.*
import kotlin.math.abs
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: TravelExpenseViewModel = viewModel()
            MaterialTheme {
                TravelExpenseApp(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            { _, y, m, d -> date = "${d.toString().padStart(2,'0')}." +
                    "${(m+1).toString().padStart(2,'0')}.$y" },
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
            // Titel
            Text(
                text = "🧳 Reiseabrechnung 🧳",
                fontFamily = FontFamily.Cursive,
                fontSize = 36.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))

            // Teilnehmerverwaltung
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFEFEF))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Teilnehmer verwalten:", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.height(8.dp))

                    // Teilnehmerchips horizontal scroll
                    if (participantsList.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            participantsList.forEach { person ->
                                val isSelected = person in selectedParticipants
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isSelected) Color(0xFFBB86FC) else Color(0xFFD0D0D0),
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                selectedParticipants = if (isSelected) selectedParticipants - person
                                                else selectedParticipants + person
                                            },
                                            onLongClick = {
                                                viewModel.removeParticipant(person)
                                                selectedParticipants = selectedParticipants - person
                                            }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(text = person, color = if (isSelected) Color.White else Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Formular für Einträge
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Neuer Eintrag:", style = MaterialTheme.typography.titleMedium)
                    // Name als simples Textfeld
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Name") },
                        leadingIcon = { Text("👤") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        placeholder = { Text("€0,00") },
                        leadingIcon = { Text("💶") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
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
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text("Beschreibung") },
                        leadingIcon = { Text("📝") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            amount.toDoubleOrNull()?.let { value ->
                                val parts = if (selectedParticipants.isEmpty() || selectedParticipants.size == participantsList.size)
                                    participantsList.ifEmpty { listOf(name.trim()) } else selectedParticipants
                                viewModel.addEntry(
                                    Entry(name.trim(), value, date, description.trim(), participants = parts)
                                )
                                name = ""; amount = ""; date = ""; description = ""
                                selectedParticipants = emptyList()
                            }
                        }
                    ) {
                        Text("Hinzufügen")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Einträge anzeigen
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 400.dp)
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
                                    style = MaterialTheme.typography.bodySmall)
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

// computeSettlements wie vorher
fun computeSettlements(entries: List<Entry>, allNames: List<String>): List<String> {
    val balances = mutableMapOf<String, Double>().apply { allNames.forEach { this[it] = 0.0 } }
    entries.forEach { e ->
        val participants = if (e.participants.isEmpty() || e.participants.size == allNames.size)
            allNames.ifEmpty { listOf(e.name) } else e.participants
        val splitAmount = e.amount / participants.size
        balances[e.name] = (balances[e.name] ?: 0.0) + e.amount
        participants.forEach { p -> balances[p] = (balances[p] ?: 0.0) - splitAmount }
    }
    if (balances.values.all { abs(it) < 0.01 }) return listOf("Alles ausgeglichen!")
    data class Person(val name: String, var balance: Double)
    val debtors = mutableListOf<Person>()
    val creditors = mutableListOf<Person>()
    balances.forEach { (name, bal) ->
        when {
            bal < -0.01 -> debtors += Person(name, -bal)
            bal > 0.01 -> creditors += Person(name, bal)
        }
    }
    return buildList {
        var i = 0
        var j = 0
        while (i < debtors.size && j < creditors.size) {
            val d = debtors[i]
            val c = creditors[j]
            val amt = min(d.balance, c.balance)
            add("${d.name} zahlt €${"%.2f".format(amt)} an ${c.name}")
            d.balance -= amt
            c.balance -= amt
            if (d.balance < 0.01) i++
            if (c.balance < 0.01) j++
        }
    }
}
