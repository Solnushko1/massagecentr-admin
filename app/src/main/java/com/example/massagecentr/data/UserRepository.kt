package com.example.massagecentr.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getUser(uid: String): User? {
        val doc = db.collection("users").document(uid).get().await()
        return if (doc.exists()) doc.toObject(User::class.java) else null
    }

    suspend fun saveUser(uid: String, phoneNumber: String, name: String) {
        val user = hashMapOf(
            "uid" to uid,
            "phoneNumber" to phoneNumber,
            "name" to name,
            "createdAt" to Timestamp.now()
        )
        db.collection("users").document(uid).set(user).await()
    }
}
