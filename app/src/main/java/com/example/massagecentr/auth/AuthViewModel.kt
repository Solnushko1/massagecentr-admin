package com.example.massagecentr.auth

import android.app.Activity
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.massagecentr.data.UserRepository
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class CodeSent(val phone: String) : AuthState()
    object NeedRegistration : AuthState()
    object LoggedIn : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastPhone: String = ""

    private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    fun initCallbacks() {
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "onVerificationCompleted")
                _state.value = AuthState.Loading
                signInWithCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(TAG, "onVerificationFailed", e)
                val msg = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Неверный формат номера. Используйте +7XXXXXXXXXX"
                    is FirebaseTooManyRequestsException ->
                        "Превышена квота SMS. Попробуйте позже."
                    is FirebaseAuthMissingActivityForRecaptchaException ->
                        "Ошибка reCAPTCHA. Перезапустите приложение."
                    else -> "Ошибка: ${e.message}"
                }
                _state.value = AuthState.Error(msg)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "onCodeSent")
                storedVerificationId = verificationId
                resendToken = token
                _state.value = AuthState.CodeSent(lastPhone)
            }

            override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                storedVerificationId = verificationId
            }
        }
    }

    fun sendCode(phone: String, activity: Activity) {
        val trimmed = phone.trim()
        if (!trimmed.matches(Regex("^\\+[1-9]\\d{10,14}$"))) {
            _state.value = AuthState.Error("Формат: +7XXXXXXXXXX (11 цифр после +7)")
            return
        }
        lastPhone = trimmed
        _state.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(trimmed)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendCode(activity: Activity) {
        val token = resendToken ?: return
        if (lastPhone.isBlank()) return
        _state.value = AuthState.Loading
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(lastPhone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyCode(code: String) {
        val verificationId = storedVerificationId
        if (verificationId.isNullOrBlank()) {
            _state.value = AuthState.Error("Сначала запросите код.")
            return
        }
        if (code.isBlank()) {
            _state.value = AuthState.Error("Введите код из SMS.")
            return
        }
        _state.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential)
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val user = result.user ?: return@addOnSuccessListener
                checkUser(user.uid, user.phoneNumber.orEmpty())
            }
            .addOnFailureListener { e ->
                val msg = if (e is FirebaseAuthInvalidCredentialsException)
                    "Неверный код подтверждения."
                else "Ошибка входа: ${e.message}"
                _state.value = AuthState.Error(msg)
            }
    }

    fun checkUser(uid: String, phone: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = userRepository.getUser(uid)
                _state.value = if (user != null) AuthState.LoggedIn else AuthState.NeedRegistration
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка загрузки: ${e.message}")
            }
        }
    }

    fun registerUser(name: String) {
        val uid = auth.currentUser?.uid ?: return
        val phone = auth.currentUser?.phoneNumber.orEmpty()
        if (name.isBlank()) {
            _state.value = AuthState.Error("Введите имя.")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                userRepository.saveUser(uid, phone, name.trim())
                _state.value = AuthState.LoggedIn
            } catch (e: Exception) {
                _state.value = AuthState.Error("Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun getCurrentPhone(): String = auth.currentUser?.phoneNumber.orEmpty()

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
