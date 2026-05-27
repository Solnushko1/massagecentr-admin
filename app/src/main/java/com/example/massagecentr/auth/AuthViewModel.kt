package com.example.massagecentr.auth

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.massagecentr.MassageCentrApp
import com.example.massagecentr.SessionManager
import com.example.massagecentr.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class CodeSent(val email: String) : AuthState()
    object NeedRegistration : AuthState()
    object LoggedIn : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    private var lastEmail: String = ""

    companion object {
        const val API_BASE = "https://massagecentr-admin.vercel.app"
    }

    fun initCallbacks() {
        // Совместимость с PhoneAuthFragment
    }

    /** Запрашивает OTP-код на email через Vercel/Nodemailer */
    fun sendCode(email: String, @Suppress("UNUSED_PARAMETER") activity: Activity) {
        val trimmed = email.trim().lowercase()
        if (!trimmed.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            _state.value = AuthState.Error("Введите корректный email-адрес")
            return
        }
        lastEmail = trimmed
        _state.value = AuthState.Loading

        viewModelScope.launch {
            try {
                httpPost("send-otp", """{"email":"$trimmed"}""")
                _state.value = AuthState.CodeSent(trimmed)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Ошибка отправки кода")
            }
        }
    }

    fun resendCode(@Suppress("UNUSED_PARAMETER") activity: Activity) {
        if (lastEmail.isNotBlank()) sendCode(lastEmail, activity as Activity)
    }

    /** Проверяет OTP, создаёт сессию, определяет роль пользователя */
    fun verifyCode(code: String) {
        if (lastEmail.isBlank()) {
            _state.value = AuthState.Error("Сначала запросите код.")
            return
        }
        val trimmedCode = code.trim()
        if (trimmedCode.length != 6) {
            _state.value = AuthState.Error("Код состоит из 6 цифр.")
            return
        }
        _state.value = AuthState.Loading

        viewModelScope.launch {
            try {
                httpPost("verify-otp", """{"email":"$lastEmail","code":"$trimmedCode"}""")

                // Анонимный вход — создаёт Firebase-сессию (UID не важен, важен emailKey)
                auth.signInAnonymously().await()

                // Сохраняем email и emailKey в SessionManager
                val emailKey = SessionManager.emailToKey(lastEmail)
                MassageCentrApp.session.email = lastEmail
                MassageCentrApp.session.emailKey = emailKey

                // Проверяем права администратора
                val adminDoc = db.collection("admins").document(emailKey).get().await()
                MassageCentrApp.session.isAdmin = adminDoc.exists()

                checkUser(emailKey)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Ошибка проверки кода")
            }
        }
    }

    /** Проверяет, есть ли профиль пользователя по emailKey */
    fun checkUser(emailKey: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = userRepository.getUser(emailKey)
                _state.value = if (user != null) AuthState.LoggedIn else AuthState.NeedRegistration
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка загрузки профиля: ${e.message}")
            }
        }
    }

    fun registerUser(name: String) {
        val emailKey = MassageCentrApp.session.emailKey
        val email = MassageCentrApp.session.email
        if (emailKey.isBlank()) {
            _state.value = AuthState.Error("Ошибка: войдите снова.")
            return
        }
        if (name.isBlank()) {
            _state.value = AuthState.Error("Введите имя.")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                userRepository.saveUser(emailKey, email, name.trim())
                _state.value = AuthState.LoggedIn
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun getCurrentEmail(): String = MassageCentrApp.session.email.ifBlank { lastEmail }

    /** Вход администратора через Firebase Email/Password (для веб-панели и приложения) */
    fun loginAsAdmin(email: String, password: String) {
        val trimmed = email.trim().lowercase()
        if (trimmed.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Введите email и пароль.")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(trimmed, password).await()

                val emailKey = SessionManager.emailToKey(trimmed)

                // Проверяем, что email действительно администратора
                val adminDoc = db.collection("admins").document(emailKey).get().await()
                if (!adminDoc.exists()) {
                    auth.signOut()
                    _state.value = AuthState.Error("Этот аккаунт не является администратором.")
                    return@launch
                }

                MassageCentrApp.session.email    = trimmed
                MassageCentrApp.session.emailKey = emailKey
                MassageCentrApp.session.isAdmin  = true

                // Создаём профиль администратора, если его ещё нет
                val user = userRepository.getUser(emailKey)
                if (user == null) {
                    userRepository.saveUser(emailKey, trimmed, "Администратор")
                }
                _state.value = AuthState.LoggedIn
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true ||
                    e.message?.contains("wrong-password") == true ||
                    e.message?.contains("user-not-found") == true ->
                        "Неверный email или пароль"
                    e.message?.contains("too-many-requests") == true ->
                        "Слишком много попыток. Подождите немного."
                    else -> e.message ?: "Ошибка входа"
                }
                _state.value = AuthState.Error(msg)
            }
        }
    }

    // ── HTTP helper ─────────────────────────────────────────────────────────────

    private suspend fun httpPost(path: String, jsonBody: String): JSONObject =
        withContext(Dispatchers.IO) {
            doPost(buildApiUrl(path), jsonBody)
        }

    private fun buildApiUrl(path: String): String {
        val base = API_BASE.trimEnd('/')
        return if (base.endsWith("/api")) "$base/$path"
        else "$base/api/$path"
    }

    /**
     * POST с ручным следованием за 3xx-редиректами
     * (HttpURLConnection не делает этого для POST автоматически).
     */
    private fun doPost(urlStr: String, jsonBody: String, depth: Int = 0): JSONObject {
        if (depth > 5) throw Exception("Слишком много редиректов от сервера")

        val conn = URL(urlStr).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 20_000
            conn.readTimeout = 20_000
            conn.instanceFollowRedirects = false
            conn.doOutput = true
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode

            if (code in 301..308) {
                val location = conn.getHeaderField("Location")
                    ?: throw Exception("Редирект без заголовка Location")
                conn.disconnect()
                val nextUrl = if (location.startsWith("http")) location
                              else URL(urlStr).let { u -> "${u.protocol}://${u.host}$location" }
                return doPost(nextUrl, jsonBody, depth + 1)
            }

            val text = if (code in 200..299)
                conn.inputStream.bufferedReader(Charsets.UTF_8).readText()
            else
                conn.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                    ?: """{"error":"Ошибка сервера ($code)"}"""

            val json = JSONObject(text)
            if (code !in 200..299) {
                throw Exception(json.optString("error", "Ошибка сервера ($code)"))
            }
            return json
        } finally {
            conn.disconnect()
        }
    }
}
