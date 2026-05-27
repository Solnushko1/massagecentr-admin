package com.example.massagecentr

import android.content.Context
import android.content.SharedPreferences

/**
 * Хранит данные текущей сессии в SharedPreferences.
 * emailKey используется как стабильный ключ Firestore-документов (вместо UID),
 * что позволяет переавторизации находить ранее созданный профиль.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("massagecentr_session", Context.MODE_PRIVATE)

    /** Электронный адрес текущего пользователя */
    var email: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL, value).apply()

    /** Ключ для Firestore-документа (email с точками → '_', без изменения '@') */
    var emailKey: String
        get() = prefs.getString(KEY_EMAIL_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EMAIL_KEY, value).apply()

    /** true, если у текущего пользователя есть права администратора */
    var isAdmin: Boolean
        get() = prefs.getBoolean(KEY_IS_ADMIN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_ADMIN, value).apply()

    /** Полная очистка — вызывается при выходе из аккаунта */
    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_EMAIL = "email"
        private const val KEY_EMAIL_KEY = "email_key"
        private const val KEY_IS_ADMIN = "is_admin"

        /**
         * Преобразует email в Firestore-ключ документа.
         * Должно совпадать с JS: email.toLowerCase().replace(/\./g, "_")
         * Пример: "user.name@gmail.com" → "user_name@gmail_com"
         */
        fun emailToKey(email: String): String =
            email.trim().lowercase().replace(".", "_")
    }
}
