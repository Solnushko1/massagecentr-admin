package com.example.massagecentr.chat

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val role: String = "client",
    val timestamp: Timestamp? = null
)
