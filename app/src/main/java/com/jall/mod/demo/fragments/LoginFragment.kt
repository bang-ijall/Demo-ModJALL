package com.jall.mod.demo.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.transition.MaterialSharedAxis
import com.jall.mod.demo.R
import com.jall.mod.demo.activities.MainActivity
import com.jall.mod.demo.activities.SplashActivity
import com.jall.mod.demo.preferences.AppPreference

class LoginFragment : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        val etToken = view.findViewById<TextInputEditText>(R.id.et_token)
        val cbRemember = view.findViewById<MaterialCheckBox>(R.id.cb_remember)
        val btnLogin = view.findViewById<MaterialButton>(R.id.btn_login)
        val btnToken = view.findViewById<MaterialButton>(R.id.btn_token)
        etToken.setText(AppPreference.getString(MainActivity.TOKEN, ""))
        cbRemember.isChecked = AppPreference.getBool(MainActivity.REMEMBER, false)

        btnLogin.setOnClickListener {
            SplashActivity.fetchLogin(requireContext(), etToken.text.toString(), this, cbRemember.isChecked)
        }

        btnToken.setOnClickListener {
            val tokenView = inflater.inflate(R.layout.layout_token, container, false)
            val webView = tokenView.findViewById<WebView>(R.id.web_view)
            val btnClose = tokenView.findViewById<MaterialButton>(R.id.btn_close)
            webView.settings.javaScriptEnabled = true
            webView.settings.javaScriptCanOpenWindowsAutomatically = true
            webView.settings.setSupportMultipleWindows(true)
            webView.settings.loadsImagesAutomatically = true
            webView.settings.domStorageEnabled = true
            webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            webView.loadUrl("https://mod.jall.my.id")
            val alertDialogBuilder = MaterialAlertDialogBuilder(requireContext())
            alertDialogBuilder.setView(tokenView)
            val alertDialog = alertDialogBuilder.create()

            btnClose.setOnClickListener {
                alertDialog.dismiss()
            }

            webView.webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun onReceivedError(
                    view: WebView,
                    errorCode: Int,
                    description: String,
                    failingUrl: String
                ) {
                    @Suppress("DEPRECATION")
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    alertDialog.dismiss()
                    Snackbar.make(tokenView, "Error receive client", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }

            alertDialog.show()
        }

        return view
    }
}