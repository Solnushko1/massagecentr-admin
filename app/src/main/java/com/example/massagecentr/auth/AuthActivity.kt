package com.example.massagecentr.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.massagecentr.MassageCentrApp
import com.example.massagecentr.R
import com.example.massagecentr.databinding.ActivityAuthBinding
import com.example.massagecentr.main.MainActivity
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val emailKey = MassageCentrApp.session.emailKey

        if (currentUser != null && emailKey.isNotBlank()) {
            // Сессия сохранена — проверяем профиль по emailKey
            viewModel.checkUser(emailKey)
            viewModel.state.observe(this) { state ->
                if (state is AuthState.LoggedIn) openMain()
            }
        }
        // Если emailKey пустой — показываем экран авторизации (навигация уже настроена в XML)
    }

    fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
