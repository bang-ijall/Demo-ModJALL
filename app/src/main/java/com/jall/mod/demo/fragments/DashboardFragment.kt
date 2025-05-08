package com.jall.mod.demo.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.transition.MaterialSharedAxis
import com.jall.mod.demo.R
import com.jall.mod.demo.activities.MainActivity
import com.jall.mod.demo.adapters.ModAdapter
import com.jall.mod.demo.adapters.ModShimmerAdapter
import com.jall.mod.demo.datas.ModData
import com.jall.mod.demo.preferences.AppPreference
import java.io.BufferedReader
import java.io.InputStreamReader

class DashboardFragment : Fragment() {
    private lateinit var tvAccess: MaterialTextView
    private lateinit var tvProvider: MaterialTextView
    private var color: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.topAppBar)
        tvAccess = view.findViewById(R.id.tv_access)
        tvProvider = view.findViewById(R.id.tv_provider)
        val btnRefresh = view.findViewById<MaterialButton>(R.id.btn_refresh)
        val tvEmpty = view.findViewById<MaterialTextView>(R.id.tv_empty)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        val tvDevice = view.findViewById<MaterialTextView>(R.id.tv_device)
        val tvVersion = view.findViewById<MaterialTextView>(R.id.tv_version)
        getExpiredTime(toolbar)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.setHasFixedSize(true)
        color = tvAccess.currentTextColor
        tvDevice.text = "${tvDevice.text} ${getDeviceMarketName()}"
        tvVersion.text = "${tvVersion.text} ${Build.VERSION.RELEASE}"
        getIsRoot()

        btnRefresh.setOnClickListener {
            tvAccess.text = "Access: Waiting"
            tvProvider.text = "Provider: Waiting"
            tvAccess.setTextColor(color)
            AppPreference.setBool(MainActivity.ROOT, MainActivity.isRooted())
            getIsRoot()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.logout -> {
                    MainActivity.isLogged = false
                    AppPreference.setBool(MainActivity.REMEMBER, false)
                    requireActivity().finish()
                    val intent = requireContext().packageManager.getLaunchIntentForPackage(requireContext().packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                }
            }

            true
        }

        Handler(Looper.getMainLooper()).post {
            val adapter = ModAdapter()
            val shimmerAdapter = ModShimmerAdapter(1)
            val transition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
            TransitionManager.beginDelayedTransition(recyclerView, transition)
            recyclerView.adapter = shimmerAdapter
            shimmerAdapter.notifyItemRangeInserted(0, 1)

            Handler(Looper.getMainLooper()).postDelayed({
                val data = getInstalledApps()

                if (data.isEmpty()) {
                    adapter.clear()
                    recyclerView.adapter = adapter
                    recyclerView.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                } else {
                    adapter.setList(data)
                    TransitionManager.beginDelayedTransition(recyclerView, transition)
                    recyclerView.adapter = adapter
                    adapter.notifyItemRangeInserted(0, 1)
                }
            }, 1000)
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        MainActivity.isStarted = false
    }

    private fun getExpiredTime(view: MaterialToolbar) {
        MainActivity.expiredHandler = Handler(Looper.getMainLooper())

        MainActivity.expiredRunnable = object : Runnable {
            var remainingTime = Int.MAX_VALUE

            @SuppressLint("SetTextI18n")
            override fun run() {
                val days = remainingTime / (24 * 60 * 60)
                val hours = (remainingTime % (24 * 60 * 60)) / 3600
                val minutes = (remainingTime % 3600) / 60
                val seconds = remainingTime % 60

                if (remainingTime > 0) {
                    view.subtitle = "Expired in ${days}d ${hours}h ${minutes}m ${seconds}s"
                    remainingTime--
                    MainActivity.expiredHandler.postDelayed(this, 1000)
                }
            }
        }

        MainActivity.expiredHandler.post(MainActivity.expiredRunnable)
    }

    private fun getInstalledApps(): List<ModData> {
        val pm = requireContext().packageManager
        val intent = Intent(Settings.ACTION_SETTINGS)
        val resolveInfo = pm.queryIntentActivities(intent, 0)
        val apps = mutableListOf<ModData>()

        for (info in resolveInfo) {
            val appInfo = info.activityInfo.applicationInfo
            val packageName = appInfo.packageName
            val appName = appInfo.loadLabel(pm).toString()
            val icon = appInfo.loadIcon(pm)
            val packageInfo = pm.getPackageInfo(packageName, 0)

            val version = "${packageInfo.versionName} (${if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }})"

            apps.add(ModData(icon, packageName, appName, version))
        }

        return apps
    }

    private fun getDeviceMarketName(): String {
        var systemProperty = getSystemProperty()

        if (systemProperty.isEmpty()) {
            systemProperty = StringBuffer().append(
                StringBuffer().append(Build.MANUFACTURER).append(" ").toString()
            ).append(
                Build.MODEL
            ).toString()
        }
        return systemProperty
    }

    private fun getSystemProperty(): String {
        var str2 = ""
        try {
            val bufferedReader = BufferedReader(
                InputStreamReader(
                    Runtime.getRuntime().exec(
                        StringBuffer().append("getprop ").append("ro.product.marketname").toString()
                    ).inputStream
                )
            )
            str2 = bufferedReader.readLine()
            bufferedReader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return str2
    }

    @SuppressLint("SetTextI18n")
    private fun getIsRoot() {
        if (AppPreference.getBool(MainActivity.ROOT, false)) {
            tvAccess.text = "Access: Granted"
            tvProvider.text = "Provider: ${MainActivity.rootProvider}"
            tvAccess.setTextColor(requireContext().getColor(R.color.md_theme_primary))
        } else {
            tvAccess.text = "Access: Not available"
            tvProvider.text = "Provider: None"
            tvAccess.setTextColor(color)
        }
    }
}