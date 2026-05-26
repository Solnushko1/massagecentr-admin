package com.example.massagecentr.services

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ServicesViewModel : ViewModel() {

    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    private val _items = MutableLiveData<List<Any>>()
    val items: LiveData<List<Any>> = _items

    private val _loading = MutableLiveData(true)
    val loading: LiveData<Boolean> = _loading

    init {
        loadServices()
    }

    private fun loadServices() {
        _loading.value = true
        db.collection("services")
            .orderBy("category")
            .get()
            .addOnSuccessListener { snapshot ->
                val services = snapshot.documents.map { doc ->
                    ServiceItem(
                        id = doc.id,
                        name = doc.getString("name").orEmpty(),
                        price = doc.getString("price").orEmpty(),
                        duration = doc.getString("duration").orEmpty(),
                        category = doc.getString("category").orEmpty()
                    )
                }
                _items.value = buildGroupedList(services)
                _loading.value = false
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "loadServices failed", e)
                _items.value = emptyList()
                _loading.value = false
            }
    }

    private fun buildGroupedList(services: List<ServiceItem>): List<Any> {
        val result = mutableListOf<Any>()
        var lastCategory = ""
        for (item in services) {
            if (item.category != lastCategory) {
                result.add(item.category)
                lastCategory = item.category
            }
            result.add(item)
        }
        return result
    }

    companion object {
        private const val TAG = "ServicesViewModel"
    }
}
