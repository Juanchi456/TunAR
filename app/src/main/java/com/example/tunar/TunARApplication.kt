package com.example.tunar

import android.app.Application
import com.example.tunar.di.AppContainer

class TunARApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
