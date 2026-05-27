package com.example.massagecentr.chat

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.massagecentr.MassageCentrApp
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ChatViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // ─── Общее ──────────────────────────────────────────────────────────────────

    private val isAdmin: Boolean get() = MassageCentrApp.session.isAdmin

    /** Стабильный ключ текущего пользователя */
    val userId: String get() = MassageCentrApp.session.emailKey

    // ─── Режим клиента ───────────────────────────────────────────────────────────

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    private var clientListener: ListenerRegistration? = null

    /** Отправка сообщения клиентом (в свою переписку) */
    fun sendMessage(text: String) {
        val key   = userId
        val trimmed = text.trim()
        if (key.isBlank() || trimmed.isBlank()) return
        val ts = Timestamp.now()
        // Сообщение в подколлекцию
        db.collection("chats").document(key).collection("messages").add(
            hashMapOf("text" to trimmed, "senderId" to key, "role" to "client", "timestamp" to ts)
        ).addOnFailureListener { Log.w(TAG, "sendMessage failed", it) }
        // Обновляем/создаём родительский документ — иначе веб-панель не видит переписку
        db.collection("chats").document(key).set(
            hashMapOf("lastMessage" to trimmed, "lastTimestamp" to ts, "userKey" to key),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    // ─── Режим администратора ────────────────────────────────────────────────────

    private val _conversations = MutableLiveData<List<ChatConversation>>()
    val conversations: LiveData<List<ChatConversation>> = _conversations

    private val _adminMessages = MutableLiveData<List<ChatMessage>>()
    val adminMessages: LiveData<List<ChatMessage>> = _adminMessages

    /** Ключ текущей открытой переписки (для администратора) */
    var activeChatKey: String = ""
        private set

    private var convListener: ListenerRegistration? = null
    private var adminMsgListener: ListenerRegistration? = null

    /** Слушает список всех переписок клиентов */
    fun loadConversations() {
        convListener?.remove()
        convListener = db.collection("chats")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                viewModelScope.launch {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val chatKey = doc.id
                            val lastMsgSnap = db.collection("chats").document(chatKey)
                                .collection("messages")
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1).get().await()
                            val lastMsg = lastMsgSnap.documents.firstOrNull()?.data
                            val userDoc = db.collection("users").document(chatKey).get().await()
                            ChatConversation(
                                chatKey      = chatKey,
                                userName     = userDoc.getString("name") ?: "Клиент",
                                userEmail    = userDoc.getString("email") ?: chatKey,
                                lastMessage  = lastMsg?.get("text") as? String ?: "",
                                lastTimestamp = lastMsg?.get("timestamp") as? Timestamp
                            )
                        } catch (e: Exception) { null }
                    }.sortedByDescending { it.lastTimestamp?.seconds ?: 0L }
                    _conversations.value = list
                }
            }
    }

    /** Открывает переписку конкретного клиента (для администратора) */
    fun openConversation(chatKey: String) {
        activeChatKey = chatKey
        adminMsgListener?.remove()
        adminMsgListener = db.collection("chats").document(chatKey)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                _adminMessages.value = snapshot.documents.map { doc ->
                    ChatMessage(
                        id        = doc.id,
                        text      = doc.getString("text").orEmpty(),
                        senderId  = doc.getString("senderId").orEmpty(),
                        role      = doc.getString("role") ?: "client",
                        timestamp = doc.getTimestamp("timestamp")
                    )
                }
            }
    }

    /** Отправка ответа администратором в переписку клиента */
    fun sendAdminReply(text: String) {
        val key     = activeChatKey
        val trimmed = text.trim()
        if (key.isBlank() || trimmed.isBlank()) return
        val ts = Timestamp.now()
        db.collection("chats").document(key).collection("messages").add(
            hashMapOf("text" to trimmed, "senderId" to userId, "role" to "admin", "timestamp" to ts)
        ).addOnFailureListener { Log.w(TAG, "sendAdminReply failed", it) }
        // Обновляем родительский документ (для обновления превью в списке клиентов)
        db.collection("chats").document(key).set(
            hashMapOf("lastMessage" to "✓ $trimmed", "lastTimestamp" to ts),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    // ─── Инициализация ───────────────────────────────────────────────────────────

    init {
        if (isAdmin) {
            loadConversations()
        } else {
            startClientListening()
        }
    }

    private fun startClientListening() {
        val key = userId
        if (key.isBlank()) return
        clientListener = db.collection("chats").document(key)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.w(TAG, "Listen failed", error); return@addSnapshotListener }
                if (snapshot != null) {
                    _messages.value = snapshot.documents.map { doc ->
                        ChatMessage(
                            id        = doc.id,
                            text      = doc.getString("text").orEmpty(),
                            senderId  = doc.getString("senderId").orEmpty(),
                            role      = doc.getString("role") ?: "client",
                            timestamp = doc.getTimestamp("timestamp")
                        )
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        clientListener?.remove()
        convListener?.remove()
        adminMsgListener?.remove()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}
