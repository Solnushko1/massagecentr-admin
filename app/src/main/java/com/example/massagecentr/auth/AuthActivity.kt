package com.example.massagecentr.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
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
        if (currentUser != null) {
            // Анонимная сессия сохранена на устройстве → проверяем профиль
            viewModel.checkUser(currentUser.uid, "")
            viewModel.state.observe(this) { state ->
                if (state is AuthState.LoggedIn) openMain()
            }
        }
    }

    fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
