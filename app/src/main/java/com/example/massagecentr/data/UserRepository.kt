package com.example.massagecentr.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Ищет пользователя по emailKey (стабильный ключ, не меняется при переавторизации).
     */
    suspend fun getUser(emailKey: String): User? {
        val doc = db.collection("users").document(emailKey).get().await()
        return if (doc.exists()) doc.toObject(User::class.java) else null
    }

    /**
     * Сохраняет профиль по emailKey.
     * uid обновляется при каждом входе (anonymous auth создаёт новый, но emailKey остаётся тем же).
     */
    suspend fun saveUser(emailKey: String, email: String, name: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val user = hashMapOf(
            "uid" to uid,
            "emailKey" to emailKey,
            "email" to email,
            "name" to name,
            "createdAt" to Timestamp.now()
        )
        db.collection("users").document(emailKey).set(user).await()
    }
}
