package com.jall.mod.demo.preferences

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("StaticFieldLeak")
object MenuPreference {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var context: Context

    fun init(context: Context, id: String) {
        MenuPreference.context = context
        sharedPreferences = context.getSharedPreferences("${id}_menu-preferences", Context.MODE_PRIVATE)
    }

    fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences
    }

    fun getBool(key: String, value: Boolean): Boolean {
        return try {
            sharedPreferences.getBoolean(key, value)
        } catch (e: ClassCastException) {
            e.printStackTrace()
            value
        }
    }

    fun setBool(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    fun getInt(key: String, value: Int): Int {
        return try {
            sharedPreferences.getInt(key, value)
        } catch (e: ClassCastException) {
            e.printStackTrace()
            value
        }
    }

    fun setInt(key: String, value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun getString(key: String, value: String): String {
        return try {
            sharedPreferences.getString(key, value)!!
        } catch (e: ClassCastException) {
            e.printStackTrace()
            value
        }
    }

    fun setString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }
}