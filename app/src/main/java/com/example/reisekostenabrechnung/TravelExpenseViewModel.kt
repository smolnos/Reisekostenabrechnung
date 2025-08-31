package com.example.reisekostenabrechnung

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// DataStore Extension
val Application.dataStore by androidx.datastore.preferences.preferencesDataStore("travel_expenses")

// 👉 Datenklasse für Export/Import
@Serializable
data class ExportData(
    val entries: List<Entry>,
    val participants: List<String>
)

class TravelExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val dataStore = application.dataStore
    private val KEY_ENTRIES = stringPreferencesKey("entries_json")
    private val KEY_PARTICIPANTS = stringPreferencesKey("participants_json")

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val _participants = MutableStateFlow<List<String>>(emptyList())
    val participants: StateFlow<List<String>> = _participants.asStateFlow()

    init {
        // beim Start laden
        viewModelScope.launch {
            dataStore.data.collect { prefs ->
                prefs[KEY_ENTRIES]?.let { json ->
                    _entries.value = Json.decodeFromString(json)
                }
                prefs[KEY_PARTICIPANTS]?.let { json ->
                    _participants.value = Json.decodeFromString(json)
                }
            }
        }
    }

    fun addEntry(entry: Entry) {
        val newList = _entries.value + entry
        _entries.value = newList
        saveEntries(newList)
    }

    fun removeEntry(index: Int) {
        val newList = _entries.value.toMutableList().apply { removeAt(index) }
        _entries.value = newList
        saveEntries(newList)
    }

    fun addParticipant(name: String) {
        if (name.isNotBlank() && !_participants.value.contains(name)) {
            val newList = _participants.value + name
            _participants.value = newList
            saveParticipants(newList)
        }
    }

    fun removeParticipant(name: String) {
        val newList = _participants.value - name
        _participants.value = newList
        saveParticipants(newList)

        // gleichzeitig auch aus allen Einträgen entfernen
        val updatedEntries = _entries.value.map { entry ->
            entry.copy(participants = entry.participants - name)
        }
        _entries.value = updatedEntries
        saveEntries(updatedEntries)
    }

    private fun saveEntries(list: List<Entry>) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_ENTRIES] = Json.encodeToString(list)
            }
        }
    }

    private fun saveParticipants(list: List<String>) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_PARTICIPANTS] = Json.encodeToString(list)
            }
        }
    }

    // 👉 Export als JSON-Datei
    fun exportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            val exportObject = ExportData(
                entries = _entries.value,
                participants = _participants.value
            )
            val json = Json.encodeToString(exportObject)

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.bufferedWriter().use { writer ->
                    writer.write(json)
                    writer.flush() // 👉 wichtig!
                }
            }
        }
    }


    // 👉 Import aus JSON-Datei
    fun importData(context: Context, uri: Uri) {
        viewModelScope.launch {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val json = stream.bufferedReader().readText()
                val importObject = Json.decodeFromString<ExportData>(json)

                _entries.value = importObject.entries
                _participants.value = importObject.participants

                saveEntries(importObject.entries)
                saveParticipants(importObject.participants)
            }
        }
    }
}
