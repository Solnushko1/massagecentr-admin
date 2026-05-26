package com.example.massagecentr.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _news = MutableLiveData<List<NewsItem>>()
    val news: LiveData<List<NewsItem>> = _news

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    init {
        loadNews()
    }

    private fun loadNews() {
        _loading.value = true
        db.collection("news")
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.map { doc ->
                    NewsItem(
                        id = doc.id,
                        title = doc.getString("title").orEmpty(),
                        body = doc.getString("body").orEmpty(),
                        date = doc.getTimestamp("date"),
                        imageUrl = doc.getString("imageUrl").orEmpty()
                    )
                }
                _news.value = list
                _loading.value = false
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "loadNews failed", e)
                _news.value = emptyList()
                _loading.value = false
            }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
