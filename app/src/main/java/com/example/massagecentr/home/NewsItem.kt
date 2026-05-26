package com.example.massagecentr.home

import com.google.firebase.Timestamp

data class NewsItem(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val date: Timestamp? = null,
    val imageUrl: String = ""
)
