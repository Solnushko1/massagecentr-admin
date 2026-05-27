package com.example.massagecentr

import android.app.Application

class MassageCentrApp : Application() {

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this)
    }

    companion object {
        /** Глобальный доступ к сессии из любого места приложения */
        lateinit var session: SessionManager
            private set
    }
}
