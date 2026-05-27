package com.example.massagecentr.auth

import android.app.Activity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.massagecentr.data.UserRepository
import com.google.firebase.auth.FirebaseAuth
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
    private val userRepository = UserRepository()

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    private var lastEmail: String = ""

    // ── URL Vercel API ──────────────────────────────────────────────────────────
    // После деплоя на Vercel замени на свой URL (см. README)
    companion object {
        const val API_BASE = "https://massagecentr-admin.vercel.app"
    }
    // ───────────────────────────────────────────────────────────────────────────

    fun initCallbacks() {
        // Совместимость с PhoneAuthFragment — не требует инициализации
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

    /** Проверяет код и выполняет анонимный вход в Firebase для сессии */
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

                // Получаем Firebase-сессию через анонимный вход
                // (UID сохраняется на устройстве, сессия не истекает 6 месяцев)
                val result = auth.signInAnonymously().await()
                val uid = result.user?.uid ?: throw Exception("Ошибка сессии")

                checkUser(uid, lastEmail)
            } catch (e: Exception) {
                _state.value = AuthState.Error(e.message ?: "Ошибка проверки кода")
            }
        }
    }

    fun checkUser(uid: String, email: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = userRepository.getUser(uid)
                _state.value = if (user != null) AuthState.LoggedIn else AuthState.NeedRegistration
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка загрузки профиля: ${e.message}")
            }
        }
    }

    fun registerUser(name: String) {
        val uid = auth.currentUser?.uid ?: return
        if (name.isBlank()) {
            _state.value = AuthState.Error("Введите имя.")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                userRepository.saveUser(uid, lastEmail, name.trim())
                _state.value = AuthState.LoggedIn
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun getCurrentEmail(): String = lastEmail

    // ── HTTP helper ─────────────────────────────────────────────────────────────

    private suspend fun httpPost(path: String, jsonBody: String): JSONObject =
        withContext(Dispatchers.IO) {
            doPost(buildApiUrl(path), jsonBody)
        }

    /** Строит правильный URL: убирает лишние слеши и добавляет /api/ префикс */
    private fun buildApiUrl(path: String): String {
        val base = API_BASE.trimEnd('/')
        // Если base уже заканчивается на /api — не добавляем повторно
        return if (base.endsWith("/api")) "$base/$path"
        else "$base/api/$path"
    }

    /**
     * Выполняет POST-запрос. Вручную следует за 3xx-редиректами,
     * потому что HttpURLConnection не делает этого для POST автоматически.
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
            conn.instanceFollowRedirects = false   // следим вручную
            conn.doOutput = true
            conn.outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode

            // Следуем за редиректом (301/302/307/308) вручную, сохраняя метод POST
            if (code in 301..308) {
                val location = conn.getHeaderField("Location")
                    ?: throw Exception("Редирект без заголовка Location")
                conn.disconnect()
                // Если location относительный — достраиваем
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
