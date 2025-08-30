package com.example.reisekostenabrechnung

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// DataStore Extension
val Application.dataStore by androidx.datastore.preferences.preferencesDataStore("travel_expenses")

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
}
