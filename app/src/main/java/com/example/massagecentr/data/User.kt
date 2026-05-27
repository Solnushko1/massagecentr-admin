package com.example.massagecentr.data

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val createdAt: Timestamp? = null
)
