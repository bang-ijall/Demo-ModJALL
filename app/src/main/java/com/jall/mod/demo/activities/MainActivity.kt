package com.jall.mod.demo.activities

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.jall.mod.demo.R
import com.jall.mod.demo.fragments.DashboardFragment
import com.jall.mod.demo.fragments.LoginFragment
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            if (isLogged) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, DashboardFragment())
                    .commit()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, LoginFragment())
                    .commit()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment is LoginFragment || currentFragment is DashboardFragment) {
            finishAffinity()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    companion object {
        const val TOKEN = "token"
        const val ROOT = "root"
        const val REMEMBER = "remember"

        lateinit var uuid: String
        lateinit var expiredHandler: Handler
        lateinit var expiredRunnable: Runnable
        lateinit var rootProvider: String
        lateinit var createHandler: Handler
        lateinit var createRunnable: Runnable

        var isLogged = false
        var isStarted = false

        fun isRooted(): Boolean {
            val isRooted = try {
                val process = Runtime.getRuntime().exec("su -c whoami")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readLine()
                output == "root"
            } catch (e: Exception) {
                false
            }

            if (isRooted) {
                var process = Runtime.getRuntime().exec("su -v")
                var reader = BufferedReader(InputStreamReader(process.inputStream))
                var output = reader.readLine()

                if (output != null) {
                    rootProvider = output
                }

                process.waitFor()
                process = Runtime.getRuntime().exec("su -V")
                reader = BufferedReader(InputStreamReader(process.inputStream))
                output = reader.readLine()

                if (output != null) {
                    rootProvider += " ($output)"
                }

                process.waitFor()
            }

            return isRooted
        }
    }
}