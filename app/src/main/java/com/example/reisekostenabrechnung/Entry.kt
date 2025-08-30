package com.example.reisekostenabrechnung

import kotlinx.serialization.Serializable

@Serializable
data class Entry(
    val name: String,
    val amount: Double,
    val date: String,
    val description: String,
    val participants: List<String> = listOf()
)
