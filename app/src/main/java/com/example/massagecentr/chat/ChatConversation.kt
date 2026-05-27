package com.example.massagecentr.chat

import com.google.firebase.Timestamp

data class ChatConversation(
    val chatKey: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Timestamp? = null
)
