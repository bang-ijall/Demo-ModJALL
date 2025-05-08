package com.jall.mod.demo

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.jall.mod.demo.preferences.AppPreference

class Application : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        AppPreference.init(this)
    }
}