package com.example.massagecentr.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private var listenerRegistration: ListenerRegistration? = null

    val userId: String get() = auth.currentUser?.uid.orEmpty()

    init {
        startListening()
    }

    private fun startListening() {
        val uid = userId
        if (uid.isBlank()) return

        listenerRegistration = db.collection("chats")
            .document(uid)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        ChatMessage(
                            id = doc.id,
                            text = doc.getString("text").orEmpty(),
                            senderId = doc.getString("senderId").orEmpty(),
                            role = doc.getString("role") ?: "client",
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    }
                    _messages.value = list
                }
            }
    }

    fun sendMessage(text: String) {
        val uid = userId
        if (uid.isBlank() || text.isBlank()) return

        val message = hashMapOf(
            "text" to text.trim(),
            "senderId" to uid,
            "role" to "client",
            "timestamp" to Timestamp.now()
        )

        db.collection("chats")
            .document(uid)
            .collection("messages")
            .add(message)
            .addOnFailureListener { e ->
                Log.w(TAG, "sendMessage failed", e)
            }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
