package com.jall.mod.demo.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jall.mod.demo.R
import com.jall.mod.demo.fragments.DashboardFragment
import com.jall.mod.demo.fragments.LoginFragment
import com.jall.mod.demo.preferences.AppPreference
import com.jall.mod.demo.views.LoadingDialog

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    @Suppress("DEPRECATION")
    @SuppressLint("HardwareIds")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        MainActivity.uuid = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        Handler(Looper.getMainLooper()).postDelayed({
            if (!Settings.canDrawOverlays(this@SplashActivity)) {
                MaterialAlertDialogBuilder(this@SplashActivity)
                    .setIcon(R.drawable.baseline_format_overline_24)
                    .setTitle("Overlay permission")
                    .setMessage("Overlay permission is required in order to show mod menu")
                    .setNegativeButton("Decline") { _, _ ->
                        finish()
                    }
                    .setPositiveButton("Accept") { _, _ ->
                        startActivityForResult(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")), 100)
                    }
                    .show()
            } else {
                val isRemember = AppPreference.getBool(MainActivity.REMEMBER, false)

                if (isRemember) {
                    fetchLogin(this@SplashActivity, AppPreference.getString(MainActivity.TOKEN, ""), null, true)
                } else {
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }, 1000)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 100) {
            finish()
            val restartIntent = packageManager.getLaunchIntentForPackage(packageName)
            restartIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(restartIntent)
        }
    }

    companion object {
        fun fetchLogin(ctx: Context, token: String, fragment: LoginFragment? = null, isRemember: Boolean = false) {
            val progress = LoadingDialog(ctx)
            progress.show()

            Handler(Looper.getMainLooper()).postDelayed({
                MainActivity.isLogged = true
                AppPreference.setString(MainActivity.TOKEN, token)
                AppPreference.setBool(MainActivity.REMEMBER, isRemember)
                AppPreference.setBool(MainActivity.ROOT, MainActivity.isRooted())
                progress.dismiss()

                if (fragment == null) {
                    ctx.startActivity(Intent(ctx, MainActivity::class.java))
                    (ctx as AppCompatActivity).finish()
                } else {
                    fragment
                        .parentFragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, DashboardFragment())
                        .commit()
                }
            }, 1000)
        }
    }
}